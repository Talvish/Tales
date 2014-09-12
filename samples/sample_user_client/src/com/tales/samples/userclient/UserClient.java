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

import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tales.client.http.ResourceClient;
import com.tales.client.http.ResourceMethod;
import com.tales.client.http.ResourceResult;

public class UserClient extends ResourceClient {
	private static final Logger logger = LoggerFactory.getLogger( UserClient.class );
	
    public static void main( String[ ] args ) throws Exception {
    	UserClient client = new UserClient( );
    	

    	client.getUser( UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
	}

	private String debugOptions; // header overrides, response overrides, etc
	
	
	public UserClient( ) {
		super( "http://localhost:8000", "user", "20140124", "UserAgentSample/1.0" );
		
		// TODO: BytesContentProvider provider;
		// http://download.eclipse.org/jetty/stable-9/apidocs/
		// TODO: FormContentProvider provider;
		
		// now we need to create the http methods
		this.methods = new ResourceMethod[ 1 ];
		
		this.methods[ 0 ] = this.defineMethod( "get_user", Boolean.class, HttpMethod.GET, "users/{id}" )
				.definePathParameter("id", UUID.class );
	}
	
	
	// so I think all of this can go into a base implementation pretty easily
	// we can hold onto the string force the format ordering for execution
	// then we can do the http verb based on the resource implementation
	// the response can be parsed just as outlined below and can handle the  
	// result value by knowing the type of the object in question, which can
	// also be stored on the method client

	public Boolean getUser( UUID theUserId ) {
		
		// need to be able to take the value, run through the translator and urlencode if on a parameter
		// though if post body then it is down automatically I believe
		
		ResourceResult<Boolean> response;
		try {
			response = this.createRequest( this.methods[ 0 ], theUserId  )
					.execute();
			// TODO: gson is currently throwing an UnsupportedOperationException (likely because the return isn't a boolean) 
			//		 but want to investigate what is happening to ensure a TranslatedException
			return response.getResult( );
		} catch (InterruptedException e) {
			return false; // TODO: this isnt' right
		}
		
	}
}