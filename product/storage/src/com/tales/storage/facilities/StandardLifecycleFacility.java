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

import com.tales.parts.ValidationSupport;
import com.tales.storage.DataContext;
import com.tales.storage.DataRepository;
import com.tales.storage.StorageType;
import com.tales.storage.StorageLifecycleSupport;

/**
 * The default lifecycle facility used by repositories. This facility does
 * two things, a) will check to see if the object implements LifecycleSupport 
 * and if so, calls it for each lifecycle method, and b) during post get and
 * pre put, checks to see if the objects supports validation, and if so
 * validates the object.
 * @author jmolnar
 *
 * @param <R> the type of repository
 * @param <C> the type of context
 */
public class StandardLifecycleFacility<R extends DataRepository<R, C>, C extends DataContext<R, C>> implements LifecycleFacility<R, C> {
	/**
	 * Called just after an object was retrieved from the data store.
	 * Calls the LifecycleSupport method, if implemented by the object,
	 * and calls validation, if implemented.
	 * @param theObject the object retrieved from the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	public <O> void postGetObject( O theObject, StorageType theStorageType, C theContext ) {
		if( theStorageType.supportsLifecycle() ) {
			( ( StorageLifecycleSupport )theObject ).postGetObject( theContext );
		}
		if( theStorageType.supportsValidation() ) {
			( ( ValidationSupport )theObject ).validate( );
		}
	}
	
	/**
	 * Called just before an object is placed into the data store.
	 * Calls the LifecycleSupport method, if implemented by the object,
	 * and calls validation, if implemented.
	 * @param theObject the object being placed into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	public <O> void prePutObject( O theObject, StorageType theStorageType, C theContext ) {
		if( theStorageType.supportsLifecycle() ) {
			( ( StorageLifecycleSupport )theObject ).prePutObject( theContext );
		}
		if( theStorageType.supportsValidation() ) {
			( ( ValidationSupport )theObject ).validate( );
		}
	}
	
	/**
	 * Called just after an object is placed into the data store.
	 * Calls the LifecycleSupport method, if implemented by the object.
	 * @param theObject the object placed into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	public <O> void postPutObject( O theObject, StorageType theStorageType, C theContext ) {
		if( theStorageType.supportsLifecycle() ) {
			( ( StorageLifecycleSupport )theObject ).postPutObject( theContext );
		}
	}
	
	/**
	 * Called just before an object is deleted from the data store.
	 * Calls the LifecycleSupport method, if implemented by the object.
	 * @param theObject the object being deleted into the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	public <O> void preDeleteObject( O theObject, StorageType theStorageType, C theContext ) {
		if( theStorageType.supportsLifecycle() ) {
			( ( StorageLifecycleSupport )theObject ).preDeleteObject( theContext );
		}
	}
	
	/**
	 * Called just after an object was deleted from the data store.
	 * Calls the LifecycleSupport method, if implemented by the object.
	 * @param theObject the object deleted from the store
	 * @param theStorageType the storage type info for the object
	 * @param theContext the data context of the call to the store
	 */
	public <O> void postDeleteObject( O theObject, StorageType theStorageType, C theContext ) {
		if( theStorageType.supportsLifecycle() ) {
			( ( StorageLifecycleSupport )theObject ).postDeleteObject( theContext );
		}
	}
}
