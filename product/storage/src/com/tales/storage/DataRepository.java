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

import com.tales.storage.facilities.DefinitionFacility;
import com.tales.storage.facilities.LifecycleFacility;
import com.tales.system.FacilityManager;

/**
 * The base interface for an data repositories. Data repositories are the central
 * locations to manage access to data storage systems.
 * @author jmolnar
 *
 * @param <R> the repository type
 * @param <C> the context type
 */
public interface DataRepository <R extends DataRepository<R, C>, C extends DataContext<R, C>> extends FacilityManager {
	/**
	 * The status instance for the repository, which details successes and 
	 * failures of the repository.
	 * @return the status of the repository.
	 */
	StorageStatus getStatus( );

	/**
	 * Creates a context to use for getting, putting and deleting from
	 * a repository.
	 * @return the context to use
	 */
	C createContext( );

	/**
	 * Gets the storage type facility being used by the repository.
	 * The facility manages type definitions for objects being persisted.
	 * @return the story type facility
	 */
	StorageTypeFacility getStorageTypeFacility( );
	
	/**
	 * Gets the definition facility being used by the repository.
	 * The facility is used to create table definitions within the repository.
	 * @return the definition facility
	 */
	DefinitionFacility<R,C> getDefinitionFacility( );
	/**
	 * Gets the lifecycle facility being used by the repository.
	 * The facility is used to intercept calls as objects are
	 * pull, put or deleted from the repository.
	 * @return the lifecycle facility
	 */
	LifecycleFacility<R,C> getLifecycleFacility( );
}
