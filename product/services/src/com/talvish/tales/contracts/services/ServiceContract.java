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
package com.talvish.tales.contracts.services;

import java.util.Collection;

import com.google.common.base.Preconditions;
import com.talvish.tales.contracts.Contract;
import com.talvish.tales.system.status.StatusBlock;
import com.talvish.tales.system.status.StatusManager;

/**
 * This base class that represents a contract bound to an interface in a service.
 * @author jmolnar
 * 
 */
// NOTE: if we get to the point of building out more than just version binding and have apis 
// 		 hide the servlet details, we could use the service version to route, within one 
//       servlet, to the particular version implementation
public abstract class ServiceContract extends Contract {
	private final ContractStatus status = new ContractStatus( );
	private final StatusManager statusManager = new StatusManager( );
	
	// TODO: consider some form of throttle manager
	// also need to consider how the contract 
	// makes its way over to the servlet
 	
	private final Object boundImplementation;

 	/**
 	 * This is the contract constructor taking the required parameters.
 	 * @param theBoundImplementation the object that exposes the
 	 */
	protected ServiceContract( String theName, String theDescription, String[] theVersions, Object theBoundImplementation ) {
		super( theName, theDescription, theVersions );
		Preconditions.checkNotNull( theBoundImplementation, "must have an object to bind to" );
		
		// save all the basic items
		boundImplementation = theBoundImplementation;

		// get the status blocks setup
		statusManager.register( "contract", status );
	}

	/**
	 * The object that implements and exposes the contract.
	 * @return
	 */
	public Object getBoundImplementation( ) {
		return boundImplementation;
	}
	
	/**
	 * Returns the status of the contract.
	 * @return returns the status of the contract
	 */
	public ContractStatus getStatus( ) {
		return this.status;
	}
	
	/**
	 * Returns the set of status blocks for the contract
	 * @return the contract status blocks
	 */
	public Collection<StatusBlock> getStatusBlocks( ) {
		return this.statusManager.getStatusBlocks();
	}

	/**
	 * Protected method that allows subclasses to register other blocks.
	 * @return the status manager
	 */
	protected StatusManager getStatusManager( ) {
		return this.statusManager;
	}
}
