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
package com.tales.samples.userclient;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.client.http.ResourceClient;
import com.tales.client.http.ResourceMethod;
import com.tales.client.http.ResourceResult;
import com.tales.communication.HttpVerb;
import com.tales.parts.ArgumentParser;
import com.tales.system.configuration.ConfigurationManager;
import com.tales.system.configuration.MapSource;
import com.tales.system.configuration.PropertySource;

public class UserClient extends ResourceClient {
	private static final Logger logger = LoggerFactory.getLogger( UserClient.class );
	
    public static void main( String[ ] theArgs ) throws Exception {

    	// get the configuration system up and running
    	ConfigurationManager configurationManager = new ConfigurationManager( );
    	// we prepare two sources for configurations
    	// first the command line source
    	configurationManager.addSource( new MapSource( "command-line", ArgumentParser.parse( theArgs ) ) );
		// second the file source, if the command-line indicates a file is to be used
    	String filename = configurationManager.getStringValue( "settings.file", null ); // we will store config in a file ideally
		if( !Strings.isNullOrEmpty( filename ) ) {
			configurationManager.addSource( new PropertySource( filename ) );
		}
		
		// now we prepare the client for talking to the server
		String serviceBase = configurationManager.getStringValue( "user_service.base_url" ); // no default, since we need it to run

    	UserClient client = new UserClient( serviceBase );
    	
    	// client has been created, so let's load a well known suer
    	User user = client.getUser( UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
    	if( user != null ) {
    		logger.debug( "Found user: '{}'", user.getFirstName( ) );
    		user.setFirstName( "Bilbo" );
    		user = client.updateUser( user );
    		logger.debug( "Updated user: '{}'", user.getFirstName( ) );
    	} else {
    		logger.debug( "Did not find user." );
    	}
    	// TODO: this doesn't exit at the end of the main here, need to understand why
    	//	     (which is why I added the System.exit(0)
    	// TODO: one time when this ran it throw some form of SSL EOF related error that 
    	//       I need to track down (this happened on the server too)
    	System.exit( 0 );
	}

	private String debugOptions; // header overrides, response overrides, etc
	
	
	public UserClient( String theServiceBase ) {
		super( theServiceBase, "/user", "20140124", "UserAgentSample/1.0", true ); // we are allowing untrusted SSL since the sample self-cert'ed
		
		// we now define the methods that we are going to expose for calling
		this.methods = new ResourceMethod[ 2 ];
		
		this.methods[ 0 ] = this.defineMethod( "get_user", User.class, HttpVerb.GET, "users/{id}" )
				.definePathParameter("id", UUID.class );

		this.methods[ 1 ] = this.defineMethod( "update_user", User.class, HttpVerb.POST, "users/{id}/update" )
				.definePathParameter("id", UUID.class )
				.defineBodyParameter( "user", User.class );
	}
	
	/**
	 * Requests a particular user.
	 * @param theUserId the id of the user being requested
	 * @return the requested user, if found, null otherwise
	 */
	public User getUser( UUID theUserId ) {
		Preconditions.checkNotNull( theUserId, "need a user id to retrieve a user" );
		ResourceResult<User> response;
		try {
			response = this.createRequest( this.methods[ 0 ], theUserId )
					.execute();
			return response.getResult( );
		} catch (InterruptedException e) {
			return null; // TODO: this isnt' right
		}
	}

	/**
	 * A call to save the values of a user on the server.
	 * @param theUser the user to save
	 * @return the server returned version of the saved user
	 */
	public User updateUser( User theUser ) {
		Preconditions.checkNotNull( theUser, "need a user to be able to update" );
		ResourceResult<User> response;
		try {
			response = this.createRequest( this.methods[ 1 ], theUser.getId() )
					.setBodyParameter( "user", theUser )
					.execute();
			return response.getResult( );
		} catch (InterruptedException e) {
			return null; // TODO: this isnt' right
		}
	}
}