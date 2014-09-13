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
package com.tales.communication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * The HTTP endpoint represents where a single point of where a service may expose a contract on an interface.
 * @author jmolnar
 *
 */
public class HttpEndpoint {
	private final String scheme;
	private final String host;
	private final int port;
	private final String stringForm;
	
	private static final String IP_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	private static final String LABEL_REGEX = "[a-zA-Z][a-zA-Z0-9\\-]*";
	private static final String NAME_REGEX = LABEL_REGEX + "(?:\\." + LABEL_REGEX + ")*";

	private static final String LAX_HOST_REGEX = "(?:" + IP_REGEX + ")|(?:" + NAME_REGEX + ")|(?:\\*)";
	private static final String LAX_ENDPOINT_REGEX = "(https?)://(" + LAX_HOST_REGEX + "):(\\d{1,5})";
	private static final Pattern LAX_ENDPOINT_PATTERN = Pattern.compile( LAX_ENDPOINT_REGEX );
	
	private static final String STRICT_HOST_REGEX = "(?:" + IP_REGEX + ")|(?:" + NAME_REGEX + ")";
	private static final String STRICT_ENDPOINT_REGEX = "(https?)://(" + STRICT_HOST_REGEX + "):((\\d{1,5}))";
	private static final Pattern STRICT_ENDPOINT_PATTERN = Pattern.compile( STRICT_ENDPOINT_REGEX );

	private static final int SCHEME_GROUP = 1; // the group number in the regex matcher for the scheme
	private static final int HOST_GROUP = 2; // the group number in the regex matcher for the host name
	private static final int PORT_GROUP = 3; // the group number in the regex matcher for the port
	
	/**
	 * The constructor taking a string presentation of the endpoint.
	 * The endpoint must be of the pattern 'http(s)://host:port', where host can be an IP, a name
	 * @param theEndpoint
	 */
	public HttpEndpoint( String theEndpoint ) {
		this( theEndpoint, true );
	}
	
	/**
	 * The constructor taking a string presentation of the endpoint.
	 * The endpoint must be of the pattern 'http(s)://host:port', where host can be an IP, a name or '*'.
	 * '*' can only be used if isStrict is set to false.
	 * 
	 * @param theEndpoint the string form to make into an endpoint object
	 * @param isStrict whether to be lax (allow '*' for name, to mean any host,) or strict (must be a specific ip or name)
	 */
	public HttpEndpoint( String theEndpoint, boolean isStrict ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theEndpoint ), "must have a valid endpoint string" );
		Matcher matcher;
		if( isStrict ) {
			matcher = HttpEndpoint.STRICT_ENDPOINT_PATTERN.matcher( theEndpoint );
			Preconditions.checkArgument( matcher.matches( ), "the endpoint must be of the pattern: 'http(s)://host:port', where host can be an IP, a name" );
		} else {
			matcher = HttpEndpoint.LAX_ENDPOINT_PATTERN.matcher( theEndpoint );
			Preconditions.checkArgument( matcher.matches( ), "the endpoint must be of the pattern: 'http(s)://host:port', where host can be an IP, a name or '*'" );
		}

		scheme = matcher.group( SCHEME_GROUP );
		host = matcher.group( HOST_GROUP );
		try {
			System.out.print( "=================+++#$+#+$#+$#+$#+$+::::::::::: " + scheme + " " + host + " " + matcher.group( PORT_GROUP ) );
			port = Integer.parseInt( matcher.group( PORT_GROUP ) );
		} catch( NumberFormatException e ) {
			throw new IllegalArgumentException( String.format( "invalid port in the endpoint definition: %s ", theEndpoint ), e );
		}
		stringForm = String.format( "%s://%s:%d", scheme, host, port );
	}
	
	/**
	 * Returns the scheme used by the endpoint. 
	 * This can be http or https.
	 * @return the scheme used by the endpoint
	 */
	public String getScheme( ) {
		return scheme;
	}
	
	/**
	 * The host the endpoint is meant to be bound to.
	 * This can be either an IP address or '*' meaning all network interfaces.
	 * @return the host of the endpoint
	 */
	public String getHost( ) {
		return host;
	}
	
	/**
	 * The TCP port the endpoint is meant to be bound to.
	 * @return the TCP port of the endpoint
	 */
	public int getPort( ) {
		return port;
	}
	
	/**
	 * Returns the string version of the http endpoint.
	 */
	@Override 
	public String toString() {
		return stringForm;
	}
}
