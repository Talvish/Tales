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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.tales.businessobjects.ObjectId;

/**
 * The engine is the component that actually does the work
 * this includes any logic, persistence, etc. It is 
 * independent of the transports/communication mechanisms used.
 * This allows for a few possibilities including, a) having
 * more than one communication mechanism but same underlying
 * logic/control, b) a great entity to write tests againsts
 * since this is the pure logic/workhorse of the component.
 * @author Joseph Molnar
 */
public class UserEngine {
	// TODO: this needs to be updated to to proper persistence
	
	private UserEngineStatus status = new UserEngineStatus( );
	private Map<ObjectId, User> storage = new HashMap<ObjectId, User>( );
	
	public UserEngine( ) {
		// since we aren't building a real storage system
		// we are faking a storage system by using a map
		// and adding a few existing users
		User user;
		
		user = new User( new ObjectId( 1, 1, 100 ) );
		user.setFirstName( "John" );
		user.setLastName( "Doe" );		
		storage.put( user.getId(), user );
		
		user = new User( new ObjectId( 2, 1, 100 ) );
		user.setFirstName( "Jane" );
		user.setLastName( "Smith" );		
		storage.put( user.getId(), user );

	}
	
	/**
	 * The status block for the engine. This tracks
	 * engine specific states/status.
	 */
	public UserEngineStatus getStatus( ) {
		return status;
	}
	
	/**
	 * Returns a particular user, if it can be found.
	 */
	public User getUser( ObjectId theId ) {
		Preconditions.checkArgument( theId != null, "an id must be given" );
		return storage.get( theId );
	}
	
	/**
	 * Gets the users from storage.
	 */
	public Collection<User> getUsers( ) {
		// TODO: this should have a continuation token, maybe some filters, to minimize 
		//       the data returned.
		return storage.values();
	}

	/**
	 * Creates the user in storage. The parameters are not forced.
	 */
	public User createUser( String theFirstName, String theLastName ) {
		User user = new User( new ObjectId( 3, 1, 100 ) );
		
		user.setFirstName( theFirstName );
		user.setLastName( theLastName );
		storage.put( user.getId(), user);
		status.recordCreatedUser(); // update our status block
		return user;
	}

	/**
	 * Updates the user's information, though not all fields.
	 */
	public User updateUser( User theUser ) {
		Preconditions.checkArgument( theUser != null, "a user must be given if it is to be updated" );
		User user = getUser( theUser.getId() );
		
		if( user != null && ! user.isDeleted() ) { // we don't allow updates to soft-deleted users
			user.setFirstName( theUser.getFirstName( ) );
			user.setLastName(  theUser.getLastName( ) );
			// we don't reset the creation/modification time stamps
			storage.put( user.getId( ),  user ); // yes not needed, but pretend we are storing back into persistence

			return user;
		} else {
			return null;		
		}

	}
	
	/**
	 * Performs the removal of the user from the storage system.
	 * This actually performs a soft-delete.
	 */
	public boolean deleteUser( ObjectId theId ) {
		Preconditions.checkArgument( theId != null, "an id must be given" );
		
		User user = getUser( theId );
		if( user != null ) {
			user.indicateDeleted();
			storage.put( user.getId( ),  user ); // yes not needed, but pretend we are storing back into persistence
			status.recordDeletedUser(); // update our status block
			return true;
		} else {
			return false;
		}
	}

}
