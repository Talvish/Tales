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
import com.tales.storage.StorageType;
import com.tales.system.Facility;

// TODO: consider making postGet, prePut and preDelete support boolean returns which
//       could then be used to act as filters

/**
 * A facility that perform activities depending on the lifecycle state of an object
 * being saved into, retrieved from, or deleted from the underlying data store.
 * Implementations can perform things like validating objects, modifying timestamps,
 * calling lifecycle suport, etc.
 * @author jmolnar
 *
 * @param <R> the repository type
 * @param <C> the context type
 */
public interface LifecycleFacility<R extends DataRepository<R, C>, C extends DataContext<R, C>> extends Facility {
	/**
	 * Called just after an object was retrieved from the data store.
	 * @param theObject the object retrieved from the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	<T> void postGetObject( T theObject, StorageType theStorageType, C theContext );
	
	/**
	 * Called just before an object is placed into the data store.
	 * @param theObject the object being placed into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	<T> void prePutObject( T theObject, StorageType theStorageType, C theContext );
	/**
	 * Called just after an object is placed into the data store.
	 * @param theObject the object placed into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	/**
	 * Called just after an object is placed into the data store.
	 * @param theObject the object placed into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	<T> void postPutObject( T theObject, StorageType theStorageType, C theContext );
	
	/**
	 * Called just before an object is deleted from the data store.
	 * @param theObject the object being deleted into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	<T> void preDeleteObject( T theObject, StorageType theStorageType, C theContext );
	/**
	 * Called just after an object was deleted from the data store.
	 * @param theObject the object deleted from the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	<T> void postDeleteObject( T theObject, StorageType theStorageType, C theContext );
}
