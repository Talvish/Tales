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
package com.tales.storage.facilities;

import com.tales.storage.DataContext;
import com.tales.storage.DataRepository;
import com.tales.system.Facility;

/**
 * The definition facility manages table definitions in the underlying data store.
 * Users of the method simply provide the type they use and the system will do
 * the necessary tasks to map from that type to the needs of the underlying store.
 * @author jmolnar
 *
 * @param <R> the repository type
 * @param <C> the context type
 */
public interface  DefinitionFacility <R extends DataRepository<R, C>, C extends DataContext<R, C>> extends Facility {
	/**
	 * Creates the definition for the type specified in the underlying store.
	 * @param theType the type to create a table for
	 * @param theContext the context to run the request within
	 */
	<T> void createDefinition( Class<T> theType, C theContext );
	/**
	 * Deletes the definition for the type specified from the underlying store.
	 * @param theType the type to delete a table for
	 * @param theContext the context to run the request within
	 */
	<T> void deleteDefinition( Class<T> theType, C theContext);
	/**
	 * Checks if a definition for the type specified is in the underlying store.
	 * @param theType the type to check for
	 * @param theContext the context to run the request within
	 */
	<T> boolean definitionExists( Class<T> theType, C theContext);
}
