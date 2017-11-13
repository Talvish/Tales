// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.talvish.tales.validation;

/**
 * A {@code RuntimeException} thrown when there is some form of authorization failure. 
 * @author jmolnar
 *
 */
public class AuthorizationException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7361544983120610451L;

	private final String scheme;
	private final String realm;

	/**
	 * Empty constructor.
	 */
	public AuthorizationException() {
		scheme = null;
		realm = null;
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 */
	public AuthorizationException(String message) {
		super(message);
		scheme = null;
		realm = null;
	}

	/**
	 * Constructor taking the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 */
	public AuthorizationException(Throwable cause) {
		super(cause);
		scheme = null;
		realm = null;
	}

	/**
	 * Constructor taking a message and the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 * @param message the message for the exception
	 */
	public AuthorizationException(String message, Throwable cause) {
		super(message, cause);
		scheme = null;
		realm = null;
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 * @param scheme the auth scheme that should have been in place
	 * @param realm the realm/scope that is covered if authed
	 */
	public AuthorizationException(String message, String scheme, String realm ) {
		super(message);
		this.scheme = scheme;
		this.realm = realm;
	}

	/**
	 * Constructor taking a message and the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 * @param scheme the auth scheme that should have been in place
	 * @param realm the realm/scope that is covered if authed
	 * @param message the message for the exception
	 */
	public AuthorizationException(String message, String scheme, String realm, Throwable cause) {
		super(message, cause);
		this.scheme = scheme;
		this.realm = realm;
	}

	/**
	 * The auth scheme that is being used, which defaults to null.
	 * @return the auth scheme
	 */
	public String getScheme( ) {
		return this.scheme;
	}
	
	/**
	 * Returns the realm information, which defaults to null.
	 * @return the realm of the authentication
	 */
	public String getRealm( ) {
		return this.realm;		
	}
}
