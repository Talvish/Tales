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
package com.talvish.tales.client.http;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.communication.HttpVerb;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.UrlEncoding;
import com.talvish.tales.serialization.json.JsonTypeReference;
import com.talvish.tales.serialization.json.translators.ChainToJsonElementToStringTranslator;
import com.talvish.tales.system.Conditions;

/**
 * A representation of the method on a Tales-enabled service.
 * @author jmolnar
 *
 */
public class ResourceMethod {
	// the following from the method resource from the services
	// this allows us to indicate there are parameters in the path
	private static final String PARAMETER_REGEX = "\\{\\s*((?:[_a-zA-Z][_a-zA-Z0-9]*)?)\\s*\\}"; // this allows for an optional name to be given (to help with generation debugging)
	private static final Pattern PARAMETER_PATTERN = Pattern.compile( PARAMETER_REGEX );
	// this regex is based on the segment/pchar definition from RFC 3986 (Appendix A): http://www.ietf.org/rfc/rfc3986.txt
	// JAX RS: http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html "By default, the URI variable must match the regular expression "[^/]+?"
	private static final String UNRESERVED_CHAR_REGEX = "[a-zA-Z0-9\\-\\.\\_\\~]";
	private static final String PCT_ENCODED_CHAR_REGEX = "%[0-9a-fA-F][0-9a-fA-F]"; // a percent encoded character (e.g. space is %20)
	private static final String SUB_DELIMS_CHAR_REGEX = "[!$&'()*+,;=]";
	private static final String PCHAR_REGEX = String.format( "(?:%s)|(?:%s)|(?:%s)|(?:[:@])", UNRESERVED_CHAR_REGEX, PCT_ENCODED_CHAR_REGEX, SUB_DELIMS_CHAR_REGEX );
	private static final String SEGMENT_COMPONENT_REGEX = String.format( "(?:%s)+", PCHAR_REGEX );
	private static final String PARAMETER_COMPONENT_REGEX = String.format( "(?:%1$s)*(?:%2$s)(?:%1$s)*", PCHAR_REGEX, PARAMETER_REGEX );
	private static final String PATH_COMPONENT_REGEX = String.format( "(?:(?:%s)|(?:%s))", SEGMENT_COMPONENT_REGEX, PARAMETER_COMPONENT_REGEX );
	private static final String PATH_REGEX = String.format( "(%1$s(?:/%1$s)*/?)?", PATH_COMPONENT_REGEX );
	private static final Pattern PATH_PATTERN = Pattern.compile( PATH_REGEX );


	private final String name; 			// the name given to the method
	private final String methodPath;	// e.g. sign_in
	private final String methodUrl;
	private final ResourceMethodReturn returnType;
	private final HttpVerb httpVerb;
	
	private final Map<String,Integer> pathParameterIndices = new HashMap<String,Integer>( );
	private final List<String> pathParameterNames = new ArrayList<String>( );
	
	private final List<ResourceMethodParameter> pathParameters; 
	private final List<ResourceMethodParameter> externalPathParameters;
	
	private final Map<String,ResourceMethodParameter> queryParameters = new HashMap<String,ResourceMethodParameter>( );
	private final Map<String,ResourceMethodParameter> externalQueryParameters = Collections.unmodifiableMap( queryParameters );
	
	private final Map<String,ResourceMethodParameter> bodyParameters = new HashMap<String,ResourceMethodParameter>( );
	private final Map<String,ResourceMethodParameter> externalBodyParameters = Collections.unmodifiableMap( bodyParameters );
	
	private final Map<String,ResourceMethodParameter> cookieParameters = new HashMap<String,ResourceMethodParameter>( );
	private final Map<String,ResourceMethodParameter> externalCookieParameters = Collections.unmodifiableMap( cookieParameters );

	private final Map<String,ResourceMethodParameter> headerParameters = new HashMap<String,ResourceMethodParameter>( );
	private final Map<String,ResourceMethodParameter> externalHeaderParameters = Collections.unmodifiableMap( headerParameters );;
	
	private volatile int maxResponseSize		; // the maximum size, in bytes, that the response buffer can hold
	private final ResourceClient client;
	
	/**
	 * A constructor, called by the ResourceClient, to create a method.
	 * @param theName the name of the method
	 * @param theReturnType the type of object returned by the method
	 * @param theHttpVerb the http verb to use to communicate to the service
	 * @param theMethodPath the partial path (doesn't include http scheme, domain, contract root, etc)
	 * @param theClient the client responsible for creating this ResourceMethod
	 */
	protected ResourceMethod( String theName, JavaType theReturnType, HttpVerb theHttpVerb, String theMethodPath, ResourceClient theClient ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ),  "theName" );
		Preconditions.checkNotNull( theReturnType, "theReturnType" );
		Preconditions.checkNotNull( theHttpVerb, "theHttpVerb" );
		Preconditions.checkNotNull( theClient, "theClient" );
		Preconditions.checkNotNull( !Strings.isNullOrEmpty( theMethodPath ),  "theMethodRoot" );
		Matcher pathMatcher = PATH_PATTERN.matcher( theMethodPath );
		Preconditions.checkArgument( pathMatcher.matches( ), "the path string '%s' for contract '%s' does not conform to the pattern '%s'", theMethodPath, theClient.getContractRoot(), PATH_REGEX );

		name = theName;
		returnType = new ResourceMethodReturn( theReturnType );// this will do validation (e.g. not null ) of the return type
		httpVerb = theHttpVerb;
		methodPath = theMethodPath;
		methodUrl = generateUrl( theMethodPath, theClient, pathParameterNames );
		
		pathParameters = new ArrayList<ResourceMethodParameter>( pathParameterNames.size( ) );
		externalPathParameters = Collections.unmodifiableList( pathParameters );

		String pathParameterName;
		// we save name to index matching in case the developer decided to give path parameter names
		for( int index = 0; index < pathParameterNames.size(); index += 1 ) {
			pathParameterName = pathParameterNames.get( index );
			if( !Strings.isNullOrEmpty( pathParameterName ) ) {
				pathParameterIndices.put( pathParameterName, index );
			}
			// we add a null here to make sure we have entries in the list for
			// all known offsets since we will be setting those directly below
			pathParameters.add( null ); 
		}
		client = theClient;
		maxResponseSize = theClient.getDefaultMaxResponseSize();
	}
	
	/**
	 * Get the name for the method.
	 * @return the name for the resource method
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The type information of the return of this method.
	 * @return
	 */
	public ResourceMethodReturn getReturn( ) {
		return returnType;
	}
	
	/**
	 * The part of the URL path that represents this method.
	 * It doesn't contain query parameters, host name, ports, etc.
	 * @return the part of the URL path that represents the method
	 */
	public String getMethodPath( ) {
		return methodPath;
	}
	
	/**
	 * The URL that will be called. It is a full URL except it 
	 * may contain Java string formatting codes (e.g %1$s) for 
	 * path parameters that need replacing.
	 * @return the URL to be called
	 */
	public String getMethodUrl( ) {
		return methodUrl;
	}
	
	/**
	 * The HTTP verb (e.g. GET, POST, PUT, etc.) that this method will use.
	 * @return the HTTP verb to be used to call the service
	 */
	public HttpVerb getHttpVerb( ) {
		return httpVerb;
	}

	/**
	 * The maximum buffer size, in bytes, for this method.
	 * If responses are bigger exceptions may occur.
	 * @return the maximum response buffer size
	 */
	public final int getMaxResponseSize( ) {
		return this.maxResponseSize;
	}
	
	/**
	 * Sets the maximum response size that can be used for this method.
	 * Values too small will result in failed requests.
	 * @param theMaxResponseSize the maximum response size for this method
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public final ResourceMethod setMaxResponseSize( int theMaxResponseSize ) {
		Preconditions.checkArgument( theMaxResponseSize > 0, "the maximum response size, %s, for method '%s', is too small", theMaxResponseSize, this.getName( ) );
		this.maxResponseSize = theMaxResponseSize;
		return this;
	}
	
	/**
	 * Indicates that a path parameter is expected by the service and it is expecting a particular type.
	 * Using names is a convenience/debugging mechanism since setting path parameters is done in the 
	 * request constructor as a list and not by calling set methods like other parameter types.
	 * @param theName the name of the path parameter
	 * @param theType the type of the data
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod definePathParameter( String theName, Type theType ) {
		return definePathParameter( theName, new JavaType( theType ) );
	}

	/**
	 * Indicates that a path parameter is expected by the service and it is expecting a particular type.
	 * Using names is a convenience/debugging mechanism since setting path parameters is done in the 
	 * request constructor as a list and not by calling set methods like other parameter types.
	 * @param theName the name of the path parameter
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod definePathParameter( String theName, JavaType theType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ),  "name must be given" );
		Preconditions.checkArgument( pathParameterIndices.containsKey( theName ), "parameter '%s' was not specified in the path '%s'", theName, this.methodPath );
		Integer index = pathParameterIndices.get( theName );
		Preconditions.checkNotNull( index < pathParameters.size( ), "parameter '%s' refers to an unknown parameters definition", theName );
		Preconditions.checkNotNull( pathParameters.get( index ) == null, "parameter '%s' at index '%s' was already defined", theName, index );
		Preconditions.checkNotNull( theType, "type not specified for '%s'", theName );
		
		Translator translator = getSuitableTranslator( theType );  // this doesn't URL encode here, that is done when generating the path during the execute call 
		
		pathParameters.set( index, new ResourceMethodParameter( theName, index, theType, translator ) );
		return this;
	}

	/**
	 * Indicates that a path parameter is expected by the service and it is expecting a particular type.
	 * @param theIndex the index of the parameter
	 * @param theType the type of the data
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod definePathParameter( int theIndex, Type theType ) {
		return definePathParameter( theIndex, new JavaType( theType ) );
	}

	/**
	 * Indicates that a path parameter is expected by the service and it is expecting a particular type.
	 * @param theIndex the index of the parameter
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod definePathParameter( int theIndex, JavaType theType ) {
		Preconditions.checkArgument( theIndex < pathParameters.size( ), "a parameter should not exist at index '%s' according to path '%s'", theIndex, this.methodPath );
		Preconditions.checkArgument( pathParameters.get( theIndex ) == null, "the path parameter at index '%s' was already defined", theIndex );
		Preconditions.checkNotNull( theType, "theType" );

		Translator translator = getSuitableTranslator( theType );  // this doesn't URL encode here, that is done when generating the path during the execute call
		
		pathParameters.set( theIndex, new ResourceMethodParameter( "path_param_" + String.valueOf( theIndex ), theIndex, theType, translator ) );
		return this;
	}

	/**
	 * Indicates that a query parameter is expected by the service and it is expecting a particular type.
	 * @param theName the name of the query parameter
	 * @param theType the type of the data
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineQueryParameter( String theName, Type theType ) {
		return defineQueryParameter( theName, new JavaType( theType ) );
	}
	
	/**
	 * Indicates that a query parameter is expected by the service and it is expecting a particular type.
	 * @param theName the name of the query parameter
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineQueryParameter( String theName, JavaType theType ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theName ),  "theName", "name must be given" );
		Conditions.checkParameter( !queryParameters.containsKey( theName ), "theName", "parameter '%s' was already defined", theName );
		Preconditions.checkNotNull( theType, "type not specified for '%s'", theName );

		Translator translator = getSuitableTranslator( theType );
		
		queryParameters.put( theName, new ResourceMethodParameter(theName, queryParameters.size( ), theType, translator ) );
		return this;
	}

	/**
	 * Indicates that a body parameter is expected by the service and it is expecting a particular type.
	 * @param theName the name of the body parameter
	 * @param theType the type of the data
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineBodyParameter( String theName, Type theType ) {
		return defineBodyParameter( theName, new JavaType( theType ) );
	}
	
	/**
	 * Indicates that a body parameter is expected by the service and it is expecting a particular type.
	 * @param theName the name of the body parameter
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineBodyParameter( String theName, JavaType theType ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theName ),  "theName", "name must be given" );
		Conditions.checkParameter( !bodyParameters.containsKey( theName ), "theName", "parameter '%s' was already defined", theName );
		Preconditions.checkNotNull( theType, "type not specified for '%s'", theName );

		Translator translator = getSuitableTranslator( theType );
		
		bodyParameters.put( theName, new ResourceMethodParameter(theName, bodyParameters.size( ), theType, translator ) );
		return this;
	}
	
	/**
	 * Indicates that a cookie is expected by the service and it is expecting a particular type.
	 * @param theName the name of the cookie
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineCookieParameter( String theName, Type theType ) {
		return defineCookieParameter( theName, new JavaType( theType) );
	}
	
	/**
	 * Indicates that a header is expected by the service and it is expecting a particular type.
	 * @param theName the name of the header
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineCookieParameter( String theName, JavaType theType ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theName ),  "theName", "name must be given" );
		Conditions.checkParameter( !cookieParameters.containsKey( theName ), "theName", "parameter '%s' was already defined", theName );
		Preconditions.checkNotNull( theType, "type not specified for '%s'", theName );

		Translator translator = getSuitableTranslator( theType );
		
		cookieParameters.put( theName, new ResourceMethodParameter(theName, cookieParameters.size(), theType, translator ) );
		return this;
	}

	/**
	 * Indicates that a header is expected by the service and it is expecting a particular type.
	 * @param theName the name of the header
	 * @param theType the type of the data
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineHeaderParameter( String theName, Type theType ) {
		return defineHeaderParameter( theName, new JavaType( theType ) );
	}
	
	/**
	 * Indicates that a header is expected by the service and it is expecting a particular type.
	 * @param theName the name of the header
	 * @param theType the type of the data
	 * @param theGenericType the generic type, if applicable
	 * @return the ResourceMethod again, so calls can be strung together
	 */
	public ResourceMethod defineHeaderParameter( String theName, JavaType theType ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theName ),  "theName", "name must be given" );
		Conditions.checkParameter( !headerParameters.containsKey( theName ), "theName", "parameter '%s' was already defined", theName );
		Preconditions.checkNotNull( theType, "type not specified for '%s'", theName );

		Translator translator = getSuitableTranslator( theType );
		
		headerParameters.put( theName, new ResourceMethodParameter(theName, headerParameters.size(), theType, translator ) );
		return this;
	}
	
	/**
	 * The private helper method that will get a suitable translator for a type.
	 * If one isn't found, an exception is thrown.
	 * @param theType the main type of the class
	 * @param theGenericType the generic types, if given
	 * @return a suitable translator
	 */
	private Translator getSuitableTranslator( JavaType theType ) {
		Translator translator = client.jsonFacility.getToStringTranslator( theType );
		
		if( translator == null ) {
			// did not find one so we are now look for something that will be more complicated
			JsonTypeReference typeReference = client.jsonFacility.getTypeReference(theType);
			// verify we got something back
			Preconditions.checkNotNull( typeReference, "Could not get json handler for type '%s'.", theType.getSimpleName() );
			translator = new ChainToJsonElementToStringTranslator( typeReference.getToJsonTranslator() );
			// we don't need to do URL encoding here since Jetty does it automatically and if we do
			// need it we expect the caller to handle
		}
		return translator;
	}
	
	/**
	 * Gets the path parameters currently defined.
	 * @return the path parameters
	 */
	public List<ResourceMethodParameter> getPathParameters( ) {
		return this.externalPathParameters;
	}

	/**
	 * Gets the query parameters currently defined.
	 * @return the query parameters
	 */
	public Map<String,ResourceMethodParameter> getQueryParameters( ) {
		return this.externalQueryParameters;
	}

	/**
	 * Gets the body parameters currently defined.
	 * @return the body parameters
	 */
	public Map<String,ResourceMethodParameter> getBodyParameters( ) {
		return this.externalBodyParameters;
	}

	/**
	 * Gets the header parameters currently defined.
	 * @return the path parameters
	 */
	public Map<String,ResourceMethodParameter> getHeaderParameters( ) {
		return this.externalHeaderParameters;
	}

	/**
	 * Gets the cookie parameters currently defined.
	 * @return the cookie parameters
	 */
	public Map<String,ResourceMethodParameter> getCookieParameters( ) {
		return this.externalCookieParameters;
	}
	
	/**
	 * Generates a version of the path that can be used for string formatting. 
	 * @param thePath the path to generate a formatting path for
	 * @param thePathParams collects the list of path parameters found
	 * @return the usable path
	 */
	private String generateUrl( String thePath, ResourceClient theClient, List<String> thePathParams ) {
		StringBuilder pathBuilder = new StringBuilder( );
		
		Matcher parameterMatcher = PARAMETER_PATTERN.matcher( thePath );
		String paramName;
		
		// we expect the given values to be URL encoded as needed, and we 
		// know that a trailing '/' isn't here and the contract root has it
		pathBuilder.append( theClient.getEndpoint( ).toString( ) ); 
		pathBuilder.append( theClient.getContractRoot( ) ); 
		if( !theClient.getContractRoot( ).endsWith( "/") ) {
			pathBuilder.append( "/" );
		}
		
		// we need to go through the path given, extract and store the parameters given and
		// create a path we can use to generate requests
		int lastEnd = 0;
		String helper;
		// we start by looking for strings that match our parameter notion {name}
		while( parameterMatcher.find( ) ) {
			if( lastEnd < parameterMatcher.start() ) {
				// if we found a parameter we look to see if there is text before the parameter match  
				// that we need to copy to our new path, we also escape the string in case it contains
				//  regex characters (since we are building a regex)
				helper = thePath.substring( lastEnd, parameterMatcher.start( ) );
				pathBuilder.append( helper );
			}
			// get the parameter name
			paramName = parameterMatcher.group( 1 ); // group 1 since we don't want the braces
			paramName = paramName.trim();
			if( !Strings.isNullOrEmpty( paramName ) ) {
				// we save the parameter name for later use, the index in the array is important since it will
				// represent the regex group location to we can  later tell which parameter name the match 
				// will belong to . . . but WE CAN ONLY HAVE ONE!
				if( thePathParams.contains( paramName ) ) {
					// yes doing a linear search isn't exactly speed, but there should be a small
					// number in here and rather not take up space storing another structure 
					throw new IllegalArgumentException( String.format( "More than one use for path parameter '%s' found in path '%s'.", paramName, thePath ) );
				}
			} else {
				paramName = null;
			}
			
			// save the name for later
			thePathParams.add( paramName );

			// now add the Java string formatting location BUT it is
			// offset by the three first parameters (the base url, contract, version)
			pathBuilder.append( "%" );
			pathBuilder.append( thePathParams.size( ) ); // the format location is in order of the parameter itself 
			pathBuilder.append( "$s" );
			lastEnd = parameterMatcher.end( );
		}
		if( lastEnd < thePath.length() ) {
			// if we have more text to save, we save it and escape it as well to have a safe regex
			helper = thePath.substring( lastEnd, thePath.length() );
			pathBuilder.append( helper );
		}
		pathBuilder.append( "?version=" ); 
		pathBuilder.append( UrlEncoding.encode( theClient.getContractVersion( ) ) ); 
		return pathBuilder.toString( );
	}
}
