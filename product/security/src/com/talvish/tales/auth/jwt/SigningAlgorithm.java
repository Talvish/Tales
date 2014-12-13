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
package com.talvish.tales.auth.jwt;

/**
 * Enum describing the signing algorithms supported.
 * @author jmolnar
 *
 */
public enum SigningAlgorithm {
	/**
	 * SHA-256 based hash.
	 */
	HS256("HmacSHA256"), 
	/**
	 * SHA-384 based hash.
	 */
	HS384("HmacSHA384"), 
	/**
	 * SHA-512 based hash.
	 */
	HS512("HmacSHA512");

	// consider adding 'ES256' since
	//       the spec (https://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-37#section-3.1) indicates it is likely required soon
	
	private String javaName;
	
	/**
	 * Constructor that takes the Java-based name needed to load the algorithm from the runtime.
	 * @param theJavaName the Java-based algorithm name
	 */
	private SigningAlgorithm( String theJavaName ) {
		javaName = theJavaName;
	}
	
	/**
	 * The Java-based name algorithm name.
	 * @return the name that Java uses to load the algorithm
	 */
	public String getJavaName( ) {
		return javaName;
	}
	
	/**
	 * A helper method that takes the name from the JWT specification and returns
	 * the enum value to use. If "none" is used then null will be returned.
	 * @param theJWTName the JWT specification based name
	 * @return the enum signing algorithm to use, or null, if not to sign
	 */
	public static SigningAlgorithm fromString( String theJWTName ) {
		if( theJWTName.equals( "none") ) {
			return null;
		} else {
			return SigningAlgorithm.valueOf( theJWTName );
		}
	}
}