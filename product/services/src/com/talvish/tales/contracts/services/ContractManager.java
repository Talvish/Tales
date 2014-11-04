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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

/**
 * This class represents the component that manages contracts in the system.
 * @author jmolnar
 *
 */
public class ContractManager {
	private Collection<ServiceContract> contracts = Collections.unmodifiableCollection( new ArrayList<ServiceContract>( 0 ) );
	private Object contractsLock = new Object( );

	/**
	 * Registers a contract with the system, verifying that
	 * the same contract name with at least one version isn't
	 * already registered.
	 * 
	 * @param theContract the contract to register
	 */
	public void register( ServiceContract theContract ) {
		Preconditions.checkNotNull( theContract, "must provide a contract" );
		
		synchronized( contractsLock ) {
			// we make a copy here to help with multi-threaded access
			ArrayList<ServiceContract> newContracts = new ArrayList<ServiceContract>( contracts );
			
			// don't expect too many so, for the moment going to linearly verify
			for( ServiceContract contract : contracts ) {
				Preconditions.checkArgument( !contract.hasOverlap( theContract ), "the contract '%s' is already registered with at least one same version", theContract.getName( ) );
			}
			
			newContracts.add( theContract );
			contracts = Collections.unmodifiableCollection( newContracts );
		}
	}

	/**
	 * Gets a list of all contracts registered with the manager..
	 * @return the list of contracts
	 */
	public Collection<ServiceContract> getContracts( ) {
		return contracts;
	}
}
