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
package com.talvish.tales.communication;

/**
 * Enum representing the supported HTTP verbs/methods. 
 * @author jmolnar
 *
 */
public enum HttpVerb {

	/**
	 * Represents a GET request.
	 */
	GET( "GET" ),
	/**
	 * Represents a POST request.
	 */
	POST( "POST" ),
	/**
	 * Represents a PUT request.
	 */
	PUT( "PUT" ),
	/**
	 * Represents a DELETE request.
	 */
	DELETE( "DELETE" ),
	/**
	 * Represents a HEAD request.
	 */
	HEAD( "HEAD" ),
	/**
	 * Represents an OPTIONS request.
	 */
	OPTIONS( "OPTIONS" );
	
	private final String value;
	
	/**
	 * Enum constructor.
	 */
	private HttpVerb( String theValue ) {
		value = theValue;
	}
	
	/**
	 * The string representing the HTTP verb.
	 * @return the string value
	 */
	public String getValue( ) {
		return value;
	}
}