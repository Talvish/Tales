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

/**
 * This class indicates that a particular type of stored object
 * would like to participate in it's own lifecycle as it is 
 * retrieved, saved and deleted from a data store.
 * @author jmolnar
 *
 */
public interface StorageLifecycleSupport {
	/**
	 * Called just after the object was retrieved from the data store.
	 * @param theContext the data context of the call to the store
	 */
	void postGetObject( DataContext<?,?> theContext );
	
	/**
	 * Called just before the object is placed into the data store.
	 * @param theContext the data context of the call to the store
	 */
	void prePutObject( DataContext<?,?> theContext );
	/**
	 * Called just after the object is placed into the data store.
	 * @param theContext the data context of the call to the store
	 */
	void postPutObject( DataContext<?,?> theContext );
	
	/**
	 * Called just before the object is deleted from the data store.
	 * @param theContext the data context of the call to the store
	 */
	void preDeleteObject( DataContext<?,?> theContext );
	/**
	 * Called just after the object was deleted from the data store.
	 * @param theContext the data context of the call to the store
	 */
	void postDeleteObject( DataContext<?,?> theContext );
}
