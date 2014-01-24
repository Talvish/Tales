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
package com.tales.storage;

import java.util.Collection;

import com.google.common.base.Preconditions;

import com.tales.serialization.SerializationTypeSource;

/**
 * This is a helper class to get type information and field information for classes that a 
 * developer would like to serialize. 
 * @author jmolnar
 *
 */
public class StorageTypeSource implements SerializationTypeSource<StorageType, StorageField> {
	private final StorageTypeFacility storageTypeFacility;

	/**
	 * Constructs the source with the provided storage type facility
	 * @param theStorageTypeFacility the storage type facility to use
	 */
	public StorageTypeSource( StorageTypeFacility theStorageTypeFacility ) {
		Preconditions.checkNotNull( theStorageTypeFacility, "need a storage type facility" );
		storageTypeFacility = theStorageTypeFacility;
	}
	
	/**
	 * Gets the storage type facility being used by this source.
	 * @return the data contract manager being used
	 */
	public StorageTypeFacility getStorageTypeFacility( ) {
		return this.storageTypeFacility;
	}
	
	/**
	 * Gets a reflected type from a given class. The returned
	 * type represents a definition for serialization. 
	 * The reflected type represents a data contract.
	 * @param theType the type to get serialization information on
	 * @return the serializable form of the type
	 */
	@Override
	public StorageType getSerializedType( Class<?> theType ) {
		return storageTypeFacility.generateType( theType );
	}

	/**
	 * Gets, from the previously requested serializable type,
	 * the set of fields to serialize.
	 * The collection is of a set of data contract fields.
	 * @param theType the type to get serializable fields for
	 * @return the collection of serializable fields.
	 */
	@Override
	public Collection<StorageField> getSerializedFields( StorageType theType ) {
		Preconditions.checkNotNull( theType, "the type cannot be null" );
		return theType.getStoredFields();
	}
}
