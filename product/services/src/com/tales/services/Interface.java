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
package com.tales.services;

import java.util.Collection;

import com.tales.contracts.services.ServiceContract;
import com.tales.system.ExecutionLifecycleListener;
import com.tales.system.ExecutionLifecycleState;
import com.tales.system.status.StatusBlock;

/**
 * This class represents an interface that hosted workers can live off of. 
 * @author jmolnar
 *
 */
public interface Interface {
	/**
	 * Gets the name given to the interface.
	 * @return the name of the interface
	 */
	String getName( );
	
	/**
	 * Gets the service the interface is bound to.
	 * @return the interface the service is bound to
	 */
	Service getService( );
	
	/**
	 * Returns the set of status blocks for the interface
	 * @return the interface status blocks
	 */
	Collection<StatusBlock> getStatusBlocks( );
	
	/**
	 * Returns the contracts bound to the interface.
	 * @return the contracts bound to the interface
	 */
	Collection<ServiceContract> getBoundContracts( );

	/**
	 * Adds an object interested in getting execution state updates.
	 * @param theListener the listener to add
	 */
	void addListener( ExecutionLifecycleListener theListener );
	
	/**
	 * Removes an object that was once interested in getting execution state updates.
	 * @param theListener the listener to remove
	 */
	void removeListener( ExecutionLifecycleListener theListener );

	/**
	 * Returns the current lifecycle state of the interface.
	 * @return the current lifecycle state
	 */
	ExecutionLifecycleState getState( );
	
	/**
	 * Starts the interface.
	 */
	void start( );
	
	/**
	 * Stops the interface.
	 */
	void stop( );
	
	/**
	 * This is call to suspend an interface.
	 * This will not pause any operations in progress.
	 */
	void suspend( );
	
	/**
	 * This is called to resume a previously suspended interface.
	 */
	void resume( );
	
	/**
	 * Helper method that indicates if the interface is in a state that will
	 * allow suspending.
	 * @return return true if suspendable, false otherwise
	 */
	boolean canSuspend( );
	
	/**
	 * Helper method that indicates if the interface is suspended so that
	 * it can be resumed.
	 * @return return true if resumable, false otherwise
	 */
	boolean canResume( );
}
