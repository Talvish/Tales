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
import java.util.List;

/**
 * This an interface into a storage system, allowing objects to be
 * retrieved, saved and deleted. Contexts should not be considered
 * thread safe.
 * @author jmolnar
 *
 */
public interface DataContext <R extends DataRepository<R, C>, C extends DataContext<R, C>> {
	/**
	 * Gets the repository backing the context.
	 */
	R getRepository( );

	/**
	 * Gets the object of a particular type, as specified by the given key. 
	 * @param theKey the key of the object to retrieve
	 * @param theType the type of the object to retrieve
	 * @return the retrieved object, or null if not found
	 */
	<K, T> T getObject( K theKey, Class<T> theType );
	/**
	 * Gets the set of objects that match the given query definition.
	 * @param theQuery the query to perform
	 * @return the list of objects matching the query
	 */
	<T> List<T> getObjects( Query<T> theQuery );
	
	// TODO: consider a 'preparedquery', which is potentially more optimal since it will have preprocessed a bunch of items
	
	/**
	 * Saves the single object into the data store.
	 * @param theObject the object to save
	 * @param theType the type of the object to save
	 */
	<T> void putObject( T theObject, Class<T> theType );
	
	/**
	 * Puts a single object, of the specified type, into the system
	 * if, and only if, the optimistic lock value, as passed in, is the
	 * same as the value in the database.
	 * The method also ensures the lifecycle handling occurs.
	 * @param theLockCompareValue the value to compare against in the db to ensure there hasn't been a change
	 */
	<T> boolean checkedPutObject( T theObject, Class<T> theType, Object theLockCompareValue );
	
	/**
	 * Saves a collection of a type of objects into the data store.
	 * @param theObjects the collection of objects to save
	 * @param theType the type of the objects in the collection
	 */
	<T> void putObjects( Collection<T> theObjects, Class<T> theType );

	/**
	 * Deletes a single object from the data store.
	 * @param theObject the object to delete
	 * @param theType the type of the object to delete
	 */
	<T> void deleteObject( T theObject, Class<T> theType );
	/**
	 * Deletes a collection of a type of objects from the data store.
	 * @param theObjects the collection of objects to delete
	 * @param theType the type of the objects in the collection
	 */
	<T> void deleteObjects( Collection<T> theObjects, Class<T> theType );
}
