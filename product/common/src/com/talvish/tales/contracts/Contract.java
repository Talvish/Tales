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
package com.talvish.tales.contracts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.ContractVersion;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.naming.SegmentedLowercaseEntityNameValidator;

/**
 * This base class that represents a contract bound to an interface in a service.
 * @author jmolnar
 * 
 */
// NOTE: if we get to the point of building out more than just version binding and have apis 
// 		 hide the servlet details, we could use the service version to route, within one 
//       servlet, to the particular version implementation
public abstract class Contract {
	public static final String CONTRACT_NAME_VALIDATOR = "tales.contracts.contract_name";
	
	static {
		if( !NameManager.hasValidator( Contract.CONTRACT_NAME_VALIDATOR ) ) {
			NameManager.setValidator( Contract.CONTRACT_NAME_VALIDATOR, new SegmentedLowercaseEntityNameValidator( ) );
		}
	}
	
	private final String name;
	private final String description; 
	private final Map<String, ContractVersion> versions;
	
	// this is static because it is never set nor used and will always be empty so might as well share across all instances
	// subclasses that have subcontracts need to manage their subcontracts themselves
	private static final Collection<Subcontract> subcontracts = Collections.unmodifiableCollection( new ArrayList<Subcontract>( 0 ) );

 	/**
 	 * This is the contract constructor taking the required parameters.
 	 * This will perform full validation on the versions.
 	 */
	protected Contract( String theName, String theDescription, String[] theVersions ) {
		NameValidator nameValidator = NameManager.getValidator( Contract.CONTRACT_NAME_VALIDATOR );
		
        Preconditions.checkState( !Strings.isNullOrEmpty( theName ), "must have a contract name" );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Contract name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkState( theVersions != null && theVersions.length > 0, "must have at least one version" );
		
		// save all the basic items
		name = theName;
		description = theDescription;

		// generate the contract versions and save as an unmodifiable collection
		versions = Collections.unmodifiableMap( ContractVersion.generateVersions( theVersions ) );
	}

 	/**
 	 * This is the contract constructor taking the required parameters.
 	 */
	protected Contract( String theName, String theDescription, Collection<ContractVersion> theVersions ) {
		NameValidator nameValidator = NameManager.getValidator( Contract.CONTRACT_NAME_VALIDATOR );
		
        Preconditions.checkState( !Strings.isNullOrEmpty( theName ), "must have a contract name" );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Contract name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkState( theVersions != null && theVersions.size( ) > 0, "must have at least one version" );
		
		// save all the basic items
		name = theName;
		description = theDescription;

		// generate the contract versions and save as an unmodifiable collection
		versions = Collections.unmodifiableMap( ContractVersion.generateVersions( theVersions ) );
	}

 	/**
 	 * This is the contract constructor taking the required parameters.
 	 * This will not perform validation on the versions passed it. It will presume 
 	 */
	protected Contract( String theName, String theDescription, Map<String, ContractVersion> theVersions ) {
		NameValidator nameValidator = NameManager.getValidator( Contract.CONTRACT_NAME_VALIDATOR );
		
        Preconditions.checkState( !Strings.isNullOrEmpty( theName ), "must have a contract name" );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Contract name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkState( theVersions != null && theVersions.size() > 0, "must have at least one version" );
		
		// save all the basic items
		name = theName;
		description = theDescription;

		// generate the contract versions and save as an unmodifiable collection
		versions = Collections.unmodifiableMap( new TreeMap< String, ContractVersion>( theVersions ) );
	}

	/**
	 * The name of the contract.
	 * @return the name of the contract
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The description of the contract.
	 * @return the description of the contract
	 */
	public String getDescription( ){
		return description;
	}
	
	/**
	 * The versions support by the current contract. 
	 * @return
	 */
	public Collection<ContractVersion> getSupportedVersions( ) {
		return versions.values( );
	}
	
	/**
	 * Indicates if the specified version is supported by this contract. 
	 * @param theVersion the version to check for
	 * @return true if found, false otherwise
	 */
	public boolean supports( String theVersion ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theVersion ), "version is needed" );
		return versions.containsKey( theVersion );		
	}
	
	/**
	 * Indicates if the specified version is supported by this contract. 
	 * @param theVersion the version to check for
	 * @return true if found, false otherwise
	 */
	public boolean supports( ContractVersion theVersion ) {
		Preconditions.checkNotNull( theVersion, "version is needed" );
		return versions.containsKey( theVersion.getVersionString( ) );
	}
	
	/**
	 * Indicates if all the versions mentioned are supported by this contract.
	 * @param theVersions the versions to check if this contract supports
	 * @return returns false if at least one version is not supported, true if all are supported
	 */
	public boolean supports( Collection<ContractVersion> theVersions ) {
		Preconditions.checkNotNull( theVersions, "versions are needed" );
		boolean returnValue = true;
		
		for( ContractVersion version : theVersions ) {
			if( !supports( version ) ) {
				returnValue = false;
				break;
			}
		}
		return returnValue;
	}
	
	/**
	 * Indicates if the specified contract is the same name and has overlapped version. 
	 * @param theContract the contract to check if there is overlap
	 * @return true if overlap, false otherwise
	 */
	public boolean hasOverlap( Contract theContract ) {
		boolean overlap = false;
		
		Preconditions.checkNotNull( theContract, "contract is needed" );
		if( theContract.name.equals( this.name ) ) {
			for( ContractVersion version : versions.values( ) ) {
				if( theContract.supports( version ) ) {
					overlap = true;
					break;
				}
			}
		}
		return overlap;
	}
	
	/**
	 * Returns the list of subcontracts, if any, carried by this contract.
	 * This class, conveniently, returns an empty list. Subclasses that
	 * support subcontracts must override this to provide their list.
	 * The list will never be null but may be empty. 
	 * @return the list of subcontracts
	 */
	public Collection<Subcontract> getSubcontracts( ) {
		return subcontracts;
	}
}
