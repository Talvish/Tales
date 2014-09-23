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

import com.tales.businessobjects.BusinessObjectBase;
import com.tales.businessobjects.ObjectId;
import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;


@DataContract( name ="com.tales.transport.user")
public class TransportUser extends BusinessObjectBase {
	@DataMember( name = "first_name" ) private String firstName;
	@DataMember( name = "last_name" ) private String lastName;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	@SuppressWarnings("unused") 
	private TransportUser( ) {
	}
	
	/**
	 * A transforming copy constructor from the internal
	 * representation to the wire representation.
	 */
	protected TransportUser( User theUser ) {
		super( theUser );
		firstName = theUser.getFirstName();
		lastName = theUser.getLastName();
	}
	
	/**
	 * Constructor taking the required id.
	 */
	public TransportUser( ObjectId theId ) {
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
	
	
	public static User toEngineUser( TransportUser theUser ) {
		// TODO: need to do parameter validation
		User storageUser = new User( theUser.getId( ) );
		storageUser.setFirstName( theUser.getFirstName());
		storageUser.setLastName(theUser.getLastName());

		// NOTE: the timestamp objects are not set here
		//       in part because we opt not to trust the 
		//       outside world, the engine loads
		//       the user and updates the fields it
		//       wants to update
		return storageUser;
	}
	
	public static TransportUser toTransportUser( User theUser ) {
		TransportUser transportUser = new TransportUser( theUser );
		return transportUser;
	}
}
