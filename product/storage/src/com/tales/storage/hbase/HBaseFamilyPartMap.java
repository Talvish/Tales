// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// ***************************************************************************
package com.tales.storage.hbase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.translators.Translator;
import com.tales.storage.StorageField;
import com.tales.storage.StorageField.FieldMode;
import com.tales.storage.StorageType;

public class HBaseFamilyPartMap {
	private final HBaseFamilyMap containingFamily;
	private final StorageType storageType;
	private Map<String, HBaseColumnMap> realColumns	= Collections.unmodifiableMap( new HashMap<String, HBaseColumnMap>( 0 ) );
	private Map<StorageField, HBaseColumnVirtualMap> virtualColumns = Collections.unmodifiableMap( new HashMap<StorageField, HBaseColumnVirtualMap>( 0 ) );
	private Collection<StorageField> referringObjectFacetFields = Collections.unmodifiableCollection( new ArrayList<StorageField>( 0 ) );
	private Collection<StorageField> referringMemberFacetFields = Collections.unmodifiableCollection( new ArrayList<StorageField>( 0 ) );
	
	// NOTE: consider saving things like family name/bytes here as an optimization
	
	public HBaseFamilyPartMap( StorageType theStorageType, HBaseFamilyMap theContainingFamily ) {
		Preconditions.checkNotNull( theStorageType, "need the storage type" );
		Preconditions.checkNotNull( theContainingFamily, "need the family" );

		storageType = theStorageType;
		containingFamily = theContainingFamily;
	}

	public StorageType getStorgeType( ) {
		return storageType;
	}
	public HBaseFamilyMap getContainingFamily( ) {
		return containingFamily;
	}

	public HBaseColumnMap getRealColumn( String theColumn ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theColumn ), "need a column name" );
		return realColumns.get( theColumn );
	}

	public HBaseColumnVirtualMap getVirtualColumn( StorageField theField ) {
		Preconditions.checkNotNull( theField, "need a field" );
		return virtualColumns.get( theField );
	}

	public Collection<HBaseColumnMap> getRealColumns( ) {
		return realColumns.values();
	}

	public Collection<HBaseColumnVirtualMap> getVirtualColumns( ) {
		return virtualColumns.values();
	}

    /**
     * Sets the families on this table. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theFamilies the families to use
     */
    void setRealColumns( Collection<HBaseColumnMap> theColumns ) {
    	Preconditions.checkNotNull( theColumns, "need members" );
    	Preconditions.checkState( realColumns.size() == 0, "columns are already set" );

    	HashMap<String,HBaseColumnMap> newColumns = new HashMap<String, HBaseColumnMap>( theColumns.size() );
    	
    	for( HBaseColumnMap column : theColumns ) {
    		// TODO: naming for columns is actually pretty complicated so
    		//       below isn't accurate at all since it gets calculated at runtime, and even the prefix
    		//       isn't considered . . . need to fix
    		if( newColumns.containsKey( column.getName( ) ) ) {
    			throw new IllegalStateException( String.format( "A family part with storage type name '%s' is attempting to add more than one column called ~'%s'.", this.storageType.getName(), column.getName( ) ) );
    		} else if( column.getContainingFamilyPart() != this ) {
    			throw new IllegalStateException( String.format( "A family part with storge type name '%s' is attempting to add a column called ~'%s', but the columns is associated with the family part with storge name '%s'.", this.storageType.getName(), column.getName( ), column.getContainingFamilyPart().getStorgeType( ).getName( ) ) );
    		} else {
    			newColumns.put( column.getName( ), column );
    		}
    	}
    	realColumns = Collections.unmodifiableMap( newColumns );
    	
    	// so maybe there is an idea of knowing how it is used
    	// if a root, then it is only called by column
    	// everythign else is prefix
    	
    	// don't want everything, just what is accessible
    	// and for those facets that live externally (dont' have facet, member), maybe we can do it dynamically based
    	// on what it passed in on the reqeust to datacontext
    }

    /**
     * Returns the fields of other storage types that point to this
     * particular storage type/family part where the intent is to 
     * store each instance of this type as column.
     * @param theFields
     */
    public Collection<StorageField> getReferringObjectFacetFields( ) {
    	return this.referringObjectFacetFields;
    }

    /**
     * Returns the fields of other storage types that point to this
     * particular storage type/family part where the intent is to
     * store each field of each instance of this type as a column.
     * @param theFields
     */
    public Collection<StorageField> getReferringMemberFacetFields( ) {
    	return this.referringMemberFacetFields;
    }

	private Translator toByteTranslator = null;
	private Translator fromByteTranslator = null;

    /**
     * Stores the fields of other storage types that point to this
     * particular storage type/family part.
     * @param theFields
     */
	void setReferringFacetFields(Collection<StorageField> theFields, HBaseTranslationFacility theTranslationFacility ) {
		Collection<StorageField> newObjectFacetFields = new ArrayList<StorageField>( 0 );
		Collection<StorageField> newMemberFacetFields = new ArrayList<StorageField>( 0 );

		Map<StorageField, HBaseColumnVirtualMap> newColumns = new HashMap<StorageField, HBaseColumnVirtualMap>( );
		

		// need some arrays here
		for( StorageField referringField : theFields ) {
			if( referringField.getFieldMode() == FieldMode.FACET_MEMBER ) {
				newMemberFacetFields.add( referringField );
			} else if( referringField.getFieldMode() == FieldMode.FACET_OBJECT ) {
				if( toByteTranslator == null ) {
					toByteTranslator = theTranslationFacility.getToByteTranslator( this.storageType.getType(), null );
					fromByteTranslator = theTranslationFacility.getFromByteTranslator( this.storageType.getType( ), null );
				}
				newObjectFacetFields.add( referringField );
				newColumns.put( referringField, new HBaseColumnVirtualMap( referringField, this ) );
			} else {
				throw new IllegalStateException( String.format( "Field '%s.%s' refers to type '%s' but for some reason isn't a facet member or facet object.", referringField.getContainingType().getName(), referringField.getName( ), this.storageType.getName( ) ) );
			}
		}
		if( newMemberFacetFields.size() != 0 ) {
			this.referringMemberFacetFields = Collections.unmodifiableCollection( newMemberFacetFields );
		}
		if( newObjectFacetFields.size() != 0 ) {
			this.referringObjectFacetFields = Collections.unmodifiableCollection( newObjectFacetFields );
			this.virtualColumns = Collections.unmodifiableMap( newColumns );
		}
	}
	
	public byte[] toBytes( Object theInstance ) {
		return ( byte[] )toByteTranslator.translate( theInstance );
	}
	
	public Object fromBytes( byte[] theBytes ) {
		return fromByteTranslator.translate( theBytes );
	}
}
