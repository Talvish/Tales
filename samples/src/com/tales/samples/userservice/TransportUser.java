// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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
package com.tales.samples.userservice;

import java.util.UUID;

import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;


@DataContract( name ="com.tales.transport.user")
public class TransportUser extends TransportModelBase {
	@DataMember( name = "first_name" ) private String firstName;
	@DataMember( name = "last_name" ) private String lastName;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	@SuppressWarnings("unused") 
	private TransportUser( ) {
	}
	
	/**
	 * Constructor taking the required id.
	 */
	public TransportUser( UUID theId ) {
		super( theId );
	}
	
	/**
	 * Returns the first name;
	 */
	public String getFirstName( ) {
		return firstName;
	}
	
	/**
	 * Returns the last name.
	 */
	public String getLastName( ) {
		return lastName;
	}
	
	
	public static StorageUser toStorageUser( TransportUser theUser ) {
		StorageUser storageUser = new StorageUser( theUser.getId( ) );
		storageUser.setFirstName( theUser.getFirstName());
		storageUser.setLastName(theUser.getLastName());
		return storageUser;
	}
	
	public static TransportUser toTransportUser( StorageUser theUser ) {
		TransportUser transportUser = new TransportUser( theUser.getId( ) );
		transportUser.firstName = theUser.getFirstName();
		transportUser.lastName = theUser.getLastName();
		
		// TODO: need to set the base class members
		return transportUser;
	}
}
