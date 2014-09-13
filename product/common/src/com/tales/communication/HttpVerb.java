package com.tales.communication;


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
	HEAD( "HEAD" );
	
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