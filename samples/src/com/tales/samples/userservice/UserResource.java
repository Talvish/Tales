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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.services.http.PathParam;
import com.tales.contracts.services.http.RequestParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;
import com.tales.services.Conditions;
import com.tales.services.NotFoundException;

/**
 * @author Joseph Molnar
 *
 */
@ResourceContract( name="com.tales.user_contract", versions={ "20140124" } )
public class UserResource {
	private final UserEngine engine;
	
	public UserResource( UserEngine theEngine ) {
		Preconditions.checkArgument( theEngine != null );
		engine = theEngine;
	}
	
	@ResourceOperation( name="create_user", path="GET | POST : users/create" ) // supporting GET just so a browser can easily be used for manual testing
	public TransportUser createUser( 
			 @RequestParam( name="first_name" )String theFirstName, 
			 @RequestParam( name="last_name" )String theLastName ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theFirstName ), "first name must be provided" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theLastName ), "last name must be provided" );
		
		return TransportUser.toTransportUser( engine.createUser( theFirstName, theLastName ) );
	}

	@ResourceOperation( name="get_user", path="GET : users/{id}" )
	public TransportUser getUser(  @PathParam( name="id" )UUID theId  ) {
		Conditions.checkParameter( theId != null , "an id must be given" );
		
		StorageUser user = engine.getUser( theId );
		if( user == null ) {
			throw new NotFoundException( String.format( "Cound not find the user with id '%s'.", theId ) );
		} else {
			return TransportUser.toTransportUser( user );
		}
	}
	
	@ResourceOperation( name="get_users", path="GET : users/" )
	public Collection<TransportUser> getUsers( ) { // TODO: generally you dont' want to get a list of them all, but allow filters or limiting how many, continuation tokens, etc
		Collection<StorageUser> users = engine.getUsers( );
		List<TransportUser> transportUsers = new ArrayList<TransportUser>(users.size());
		
		for( StorageUser user : users ) {
			transportUsers.add( TransportUser.toTransportUser( user ) );
		}
		return transportUsers;
	}
	
//	@ResourceOperation( name="get_user", path="GET : users/{id}" )
//	public TransportUser getUser(  @RequestParam( name="id" )UUID theId  ) { // TODO: investigate why this didnt' fail TALES validation...a path param saying it is a request param
//		Conditions.checkParameter( theId == null , "an id must be given" );
//		
//		StorageUser user = engine.findUser( theId );
//		if( user == null ) {
//			throw new NotFoundException( String.format( "Cound not find the user with id '%s'.", theId ) );
//		} else {
//			return TransportUser.toTransportUser( user );
//		}
//	}
}
