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
package com.tales.contracts.services.http;


import javax.servlet.Servlet;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.services.ServiceContract;

/**
 * This class represents a contract where a path is bound to an object within a service.
 * This is an abstract class that allows subclasses to provide particular details.
 * @author jmolnar
 * 
 */
// NOTE: if we get to the point of building out more than just version binding and have apis 
// 		 hide the servlet details, we could use the service version to route, within one 
//       servlet, to the particular version implementation
// TODO: SO it would be the SAME path, but a different version, to know how to route
public abstract class HttpContract extends ServiceContract {
 	private final String boundPath;
 	private final Servlet boundServlet;

 	/**
 	 * This is the contract constructor taking the required parameters.
	 * @param theName the name of the contract
	 * @param theDescription the optional description of the contract
	 * @param theVersions the versions supported by the contract
 	 * @param theBoundObject the object the contract is bound to
 	 * @param theBoundServlet the servlet that contract is bound to
 	 * @param theBoundPath the path the servlet is bound to
 	 */
	public HttpContract( String theName, String theDescription, String[] theVersions, Object theBoundObject, Servlet theBoundServlet, String theBoundPath ) {
		super( theName, theDescription, theVersions, theBoundObject );
		Preconditions.checkNotNull( theBoundServlet, "must have a servlet" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBoundPath ), "must have a path binding" );
		
		// save all the basic items
		boundPath = theBoundPath;
		boundServlet = theBoundServlet;
	}

	/**
	 * The path in the system that the interface is bound to.
	 * @return the path in the system
	 */
	public String getBoundPath( ) {
		return boundPath;
	}
	
	/**
	 * The servlet this contract is bound to. This may
	 * also represent the bound boject.
	 * @return the bound servlet
	 */
	public Servlet getBoundServlet( ) {
		return boundServlet;
	}
}
