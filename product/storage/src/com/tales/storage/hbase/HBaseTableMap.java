// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.storage.StorageType;

/**
 * This class represents an HBase table and contains the full definition 
 * used to create and destroy the table, and to put/get columns, across
 * families, into the table.
 * @author jmolnar
 *
 */
public class HBaseTableMap {
	private Map<String, HBaseFamilyMap> families = Collections.unmodifiableMap( new HashMap<String, HBaseFamilyMap>( ) );
	private Map<StorageType, HBaseFamilyPartMap> familyParts = Collections.unmodifiableMap( new HashMap<StorageType, HBaseFamilyPartMap>() );
	private HBaseKeyMap keyMap;
	
	private final StorageType storageType;
	private final String name;
	private final byte[] nameBytes;
	
	/**
	 * Constructor taking the root storage type.
	 * @param theStorageType the root storage type
	 */
	public HBaseTableMap( StorageType theStorageType ) {
		Preconditions.checkNotNull( theStorageType, "need the root type info" );
		Preconditions.checkArgument( theStorageType.isRootStorageType(), "needs to have the root storage for a table" );
		
		storageType = theStorageType;
		name = storageType.getName( );
		nameBytes = Bytes.toBytes( name );
	}
	
	/**
	 * The name of the table.
	 * @return the name of the table
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * A byte representation of the name of the table, 
	 * for optimization dealing with HBase.
	 * @return
	 */
	public byte[] getNameBytes( ) {
		return nameBytes;
	}
	
	/**
	 * The root storage type for the table.
	 * @return the root storage type
	 */
	public StorageType getStorageType( ) {
		return storageType;
	}
	
	/**
	 * The key map for the table.
	 * @return
	 */
	public HBaseKeyMap getKey( ) {
		return keyMap;
	}
	
	/**
	 * Sets the key map for the table.This is only called by helpers within
	 * close proximity of this class's creation.
	 * @param theKeyMap
	 */
	void setKey( HBaseKeyMap theKeyMap ) {
    	Preconditions.checkNotNull( theKeyMap, "need the key map" );
    	Preconditions.checkState( keyMap == null, "key map already set" );
    	Preconditions.checkState( theKeyMap.getContainingTable() == this, "key map must belong to the table" );
    	
    	keyMap = theKeyMap;
	}
	
	/**
	 * Returns the full set of families supported by the table.
	 * @return the full set of families
	 */
	public Collection<HBaseFamilyMap> getFamilies( ) {
		return families.values();
	}
	
	/**
	 * Gets the family based on the name.
	 * @param theFamily the name of the family to get
	 * @return the family, or null if not found
	 */
	public HBaseFamilyMap getFamily( String theFamily ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need a family name" );
		return families.get( theFamily );
	}
	
    /**
     * Sets the families on this table. This should only be called by the 
     * helpers and occurs within close proximity to this class's creation.
     * @param theFamilies the families to use
     */
    void setFamilies( Collection<HBaseFamilyMap> theFamilies ) {
    	Preconditions.checkNotNull( theFamilies, "need families" );
    	Preconditions.checkState( families.size() == 0, "families are already set" );

    	HashMap<String,HBaseFamilyMap> newFamilies = new HashMap<String, HBaseFamilyMap>( theFamilies.size() );
    	HashMap<StorageType, HBaseFamilyPartMap> newFamilyParts = new HashMap<StorageType, HBaseFamilyPartMap>( theFamilies.size( ) );
    	
    	for( HBaseFamilyMap family : theFamilies ) {
    		if( newFamilies.containsKey( family.getName() ) ) {
    			throw new IllegalStateException( String.format( "The table with name '%s' is attempting to add more than one family called '%s'.", this.storageType.getName(), family.getName() ) );
    		} else if( family.getContainingTable() != this ) {
    			throw new IllegalStateException( String.format( "The table with name '%s' is attempting to add a family called '%s', but the family is associated with the table '%s'.", this.storageType.getName(), family.getName(), family.getContainingTable().getName( ) ) );
    		} else {
    			newFamilies.put( family.getName( ), family );
    			for( HBaseFamilyPartMap familyPartMap : family.getFamilyParts( ) ) {
    				newFamilyParts.put( familyPartMap.getStorgeType(), familyPartMap );
    			}
    		}
    	}
    	families = Collections.unmodifiableMap( newFamilies );
    	familyParts = Collections.unmodifiableMap( newFamilyParts );
    }

    /**
     * Gets the part of the family associated with the given storage type.
     * Since storage types can belong to at most one family, but families
     * can have more than one storage type, this works well when trying
     * to save to storage.
     * @param theStorageType the type to lookup
     * @return the family part to return, or null if not found
     */
    public HBaseFamilyPartMap getFamilyPart( StorageType theStorageType ) {
    	return this.familyParts.get( theStorageType );
    }
}
