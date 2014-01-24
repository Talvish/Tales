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

import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.parts.sites.TranslatedDataSite;
import com.tales.parts.translators.TranslationException;
import com.tales.storage.StorageField;

/*
 * TODO: look at this
 * Members point to
 * - the table they are part of
 * - the column family they are part of
 * - their own column name
 * 
 * but we also what the idea of a dynamic maps
 * we shouldn't a member map for dynamic maps, what we may have instead
 * is 'template' map that contains the translator to use and perhaps a hashmap 
 * to head into 
 */

/**
 * The class that represents a particular column, that 
 * is mapped to a field from a storage type.
 * @author jmolnar
 *
 */
public class HBaseColumnMap {
	private final HBaseFamilyPartMap containingFamilyPart;
	private final StorageField storageField;
	private final TranslatedDataSite dataSite;
	
	private final String name; 
	private final byte[] nameBytes; 

	/**
	 * The constructor taking field and the translator to translate to/from the field.
	 * @param theFieldInfo the field
	 * @param theDataSite the site that translates the data
	 */
	public HBaseColumnMap( StorageField theStorageField, TranslatedDataSite theDataSite, HBaseFamilyPartMap theContainingFamilyPart ) {
		Preconditions.checkNotNull( theStorageField, "need the associated field" );
		Preconditions.checkNotNull( theDataSite, "need the data site" );
		Preconditions.checkNotNull( theContainingFamilyPart, "need the column family part" );

		dataSite = theDataSite;
		storageField = theStorageField;
		containingFamilyPart = theContainingFamilyPart;

		name = storageField.getName();
		nameBytes = Bytes.toBytes( name );
}
	
	public String getName( ) {
		return name;
	}
	
	
	public byte[] getNameBytes( String thePrefix ) {
		if( Strings.isNullOrEmpty( thePrefix ) ) {
			return nameBytes;
		} else {
			return Bytes.toBytes( thePrefix + name );
		}
	}
	
	/**
	 * The storage field associated with this column
	 * @return the underlying storage field
	 */
	public StorageField getField( ) {
		return storageField;
	}
	
	/**
	 * The family this column is contained within.
	 * @return the family the column is contained within
	 */
	public HBaseFamilyPartMap getContainingFamilyPart( ) {
		return containingFamilyPart;
	}

	/**
	 * The item that translates data to/from column
	 * @return the item that translate data 
	 */
	public TranslatedDataSite getDataSite( ) {
		return dataSite;
	}
	
	/**
	 * Gets data for the column from the instance passed in. 
	 * @param theInstance the parent object that contains the field
	 * @return the byte representation result of what is in the field
	 */
	public byte[] getData( Object theInstance ) {
		try {
			return ( byte[] )dataSite.getData( theInstance );
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
	
	/**
	 * Sets the data on the field for the instance passed in.
	 * @param theInstance the parent object that contains the field
	 * @param theValue the byte value, which will be translated, to set the field to
	 */
	public void setData( Object theInstance, byte[] theValue ) {
		dataSite.setData( theInstance, theValue ); 
	}
}
