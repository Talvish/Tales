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
package com.tales.client.http;

import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import com.tales.communication.HttpEndpoint;
import com.tales.communication.HttpVerb;
import com.tales.contracts.ContractVersion;
import com.tales.contracts.data.DataContractManager;
import com.tales.contracts.data.DataContractTypeSource;
import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.naming.NameValidator;
import com.tales.parts.naming.SegmentedLowercaseEntityNameValidator;
import com.tales.serialization.Readability;
import com.tales.serialization.json.JsonTranslationFacility;
import com.tales.serialization.json.JsonTypeMap;
import com.tales.serialization.json.JsonTypeReference;
import com.tales.serialization.json.translators.JsonObjectToObjectTranslator;
import com.tales.serialization.json.translators.ObjectToJsonObjectTranslator;
import com.tales.system.Conditions;


/**
 * This class represents a client of a service that can be communicated with.
 * This is typically a base class and is used to define all methods that can be
 * communicated with and generate the request that will ultimately talk to the 
 * service.
 * As a note, the goal is to auto generate clients, but this class can be used
 * to hand-craft clients fairly easily. 
 * @author jmolnar
 *
 */
public class ResourceClient {
	private static final Logger logger = LoggerFactory.getLogger( ResourceClient.class );
	
	protected final HttpClient httpClient;

	protected ResourceMethod[] methods;
	
	protected final JsonTranslationFacility jsonFacility;
	protected final JsonTypeReference resultTypeReference;
	
	protected final JsonParser jsonParser;
	
	protected final HttpEndpoint endpoint; 	// e.g. http://localhost:8000
	protected final String contractRoot;	// e.g. login
	protected final String contractVersion;	// e.g. 20140901
	
	protected final String userAgent; // the user agent to use
	
	// these are volatile because when we add/remove headers here it reset these members
	// since they are expected to change through-out the life-time of the resource client
	// unlike other collections that are used through-out the com.tales.client.http 
	// package 
	protected volatile Map<String,String> headerOverrides = new HashMap<String,String>( );
	protected volatile Map<String,String> externalHeaderOverrides = Collections.unmodifiableMap( headerOverrides );
	protected final Object overrideLock = new Object( );
	

	/**
	 * Creates a resource client that will create the underlying HttpClient to talk to the
	 * service and a default JsonTypeFacility to read and generate json. The endpoint and contract root 
	 * should already have url encoded anything that needs url encoding.
	 * @param theEndpoint the end point to talk to which should be of the form http(s)?//name:port, e.g. http://localhost:8000
	 * @param theContractRoot the contract root to talk to, which is of the form /name, e.g. /login
	 * @param theContractVersion the version of the contract which is a date of the form yyyyMMDD, e.g. 20140925
	 * @param theUserAgent the user agent that this client should use
	 * @param allowUntrustedSSL indicates whether SSL will be trusted or not, which can be useful for self-certs, early development, etc
	 */
	public ResourceClient( String theEndpoint, String theContractRoot, String theContractVersion, String theUserAgent, boolean allowUntrustedSSL ) {
		this( theEndpoint, theContractRoot, theContractVersion, theUserAgent, null, allowUntrustedSSL, null );
	}
	
	/**
	 * Creates a resource client that will use the specified HttpClient and JsonTypeFacility.
	 * The endpoint and contract root should already have url encoded anything that needs url encoding.
	 * This constructor assumes all SSL setup, if needed, has already been handled and setup in the HttpClient.
	 * @param theEndpoint the end point to talk to which should be of the form http(s)?//name:port, e.g. http://localhost:8000
	 * @param theContractRoot the contract root to talk to, which is of the form /name, e.g. /login
	 * @param theContractVersion the version of the contract which is a date of the form yyyyMMDD, e.g. 20140925
	 * @param theUserAgent the user agent that this client should use
	 * @param theClient the HttpClient to use
	 * @param theJsonFacility the JsonTypeFacility to use
	 */
	public ResourceClient( String theEndpoint, String theContractRoot, String theContractVersion, String theUserAgent, HttpClient theClient, JsonTranslationFacility theJsonFacility ) {
		this( theEndpoint, theContractRoot, theContractVersion, theUserAgent, theClient, false, theJsonFacility );
	}
	/**
	 * Creates a resource client that will use the specified HttpClient and JsonTypeFacility.
	 * The endpoint and contract root should already have url encoded anything that needs url encoding.
	 * @param theEndpoint the end point to talk to which should be of the form http(s)?//name:port, e.g. http://localhost:8000
	 * @param theContractRoot the contract root to talk to, which is of the form /name, e.g. /login
	 * @param theContractVersion the version of the contract which is a date of the form yyyyMMDD, e.g. 20140925
	 * @param theUserAgent the user agent that this client should use
	 * @param theClient the HttpClient to use
	 * @param theJsonFacility the JsonTypeFacility to use
	 */
	private ResourceClient( String theEndpoint, String theContractRoot, String theContractVersion, String theUserAgent, HttpClient theClient, boolean allowUntrustedSSL, JsonTranslationFacility theJsonFacility ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theEndpoint ), "need a valid service endpoint" );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theContractRoot ), "need a contract root" );
    	Preconditions.checkArgument( theContractRoot.startsWith( "/" ), "the contract root '%s' must be a reference from the root (i.e. start with '/')", theContractRoot );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theContractVersion ), "need a version for contract root '%s'", theContractRoot );
    	Preconditions.checkArgument( ContractVersion.isValidVersion( theContractVersion),  "the version string '%s' for contract root '%s' is not valid", theContractVersion, theContractRoot );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theUserAgent ), "need a user agent for this client" );

		endpoint = new HttpEndpoint( theEndpoint ); // this will do validation on the endpoint 
		contractRoot = theContractRoot; 
		contractVersion = theContractVersion;
		userAgent = theUserAgent;
		
		// use the client if sent in, but create a working one otherwise
		if( theClient == null ) {
			try {
			    SslContextFactory sslContextFactory = null;
	
			    if( endpoint.isSecure( ) ) {
			    	if( allowUntrustedSSL ) {
			    		// so we need SSL communication BUT we don't need to worry about it being valid, likley
			    		// because the caller is self-cert'ing or in early development ... we may need to do 
			    		// more here mind you
			    		
				    	// the following code was based https://code.google.com/p/misc-utils/wiki/JavaHttpsUrl
				    	// if we were to look into mutual SSL and overall key handling, I probably want to
				    	// take a closer look
				    	
				    	// We need to create a trust manager that essentially doesn't except/fail checks
					    final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					    	// this was based on the code found here:
		
							@Override
							public void checkClientTrusted(X509Certificate[] chain,
									String authType) throws CertificateException {
							}
							@Override
							public void checkServerTrusted(X509Certificate[] chain,
									String authType) throws CertificateException {
							}
							@Override
							public X509Certificate[] getAcceptedIssuers() {
								return null;
							}
					    } };
					    
					    // then we need to create an SSL context that uses lax trust manager 
					    SSLContext sslContext = SSLContext.getInstance( "SSL" );
						sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
	
						// and finally, create the SSL context that
						sslContextFactory = new SslContextFactory();
						sslContextFactory.setSslContext( sslContext );
			    	} else {
			    		// TODO: this needs to be tested against 
			    		//		 a) real certs with real paths that aren't expired
			    		//		 b) real certs with real paths that are expired
			    		sslContextFactory = new SslContextFactory( );
			    	}
					httpClient = new HttpClient( sslContextFactory );
			    } else {
					httpClient = new HttpClient(  );
			    }
			    httpClient.setFollowRedirects( false ); // tales doesn't have redirects (at least not yet)
				httpClient.start( );
			    displayClientConfiguration( httpClient );
			} catch (NoSuchAlgorithmException | KeyManagementException e) {
				throw new IllegalStateException( "unable to create the resource client due to a problem setting up SSL", e );
			} catch (Exception e ) {
				throw new IllegalStateException( "unable to create the resource client due to the inability to start the HttpClient", e );
			}
		} else {
			httpClient = theClient;
		}
		httpClient.setUserAgentField( new HttpField( HttpHeader.USER_AGENT, theUserAgent ) );

		if( theJsonFacility == null ) {
			NameValidator jsonHttpFieldNameValidator = new LowerCaseEntityNameValidator();
			NameValidator jsonHttpClassNameValidator = new SegmentedLowercaseEntityNameValidator();
			jsonFacility = new JsonTranslationFacility( 
					new DataContractTypeSource( new DataContractManager( ) ),
					Readability.MACHINE,
					jsonHttpClassNameValidator, 
					jsonHttpFieldNameValidator );
		} else {
			jsonFacility = theJsonFacility;
		}
		
		jsonParser = new JsonParser( );
		
		// now that we have the json facility, let's 
		// get the reference for the result type
		JsonTypeMap typeMap = jsonFacility.generateTypeMap( ResourceResult.class );
		resultTypeReference = new JsonTypeReference( 
				ResourceResult.class, typeMap.getReflectedType().getName(),
    			new JsonObjectToObjectTranslator( typeMap ),
    			new ObjectToJsonObjectTranslator( typeMap ) );				
	}
	
	private void displayClientConfiguration( HttpClient theClient ) {
		StringBuffer settingBuffer = new StringBuffer ();
		
		settingBuffer.append( "\n\tAddress Resolution Timeout: " );
		settingBuffer.append( theClient.getAddressResolutionTimeout( ) );

		settingBuffer.append( "\n\tConnect Timeout: " );
		settingBuffer.append( theClient.getConnectTimeout() );
	
		settingBuffer.append( "\n\tDispatch I/O: " );
		settingBuffer.append( theClient.isDispatchIO( ) );

		settingBuffer.append( "\n\tFollow Redirects: " );
		settingBuffer.append( theClient.isFollowRedirects( ) );

		settingBuffer.append( "\n\tIdle Timeout: " );
		settingBuffer.append( theClient.getIdleTimeout( ) );

		settingBuffer.append( "\n\tMax Connections Per Destination: " );
		settingBuffer.append( theClient.getMaxConnectionsPerDestination() );

		settingBuffer.append( "\n\tMax Redirects: " );
		settingBuffer.append( theClient.getMaxRedirects( ) );

		settingBuffer.append( "\n\tMax Requests Queued Per Destination: " );
		settingBuffer.append( theClient.getMaxRequestsQueuedPerDestination( ) );

		settingBuffer.append( "\n\tRemove Idle Destinations: " );
		settingBuffer.append( theClient.isRemoveIdleDestinations() );

		settingBuffer.append( "\n\tRequest Buffer Size: " );
		settingBuffer.append( theClient.getRequestBufferSize( ) );

		settingBuffer.append( "\n\tResponse Buffer Size: " );
		settingBuffer.append( theClient.getResponseBufferSize( ) );

		settingBuffer.append( "\n\tStrict Event Ordering: " );
		settingBuffer.append( theClient.isStrictEventOrdering( ) );

		settingBuffer.append( "\n\tTCP No Delay: " );
		settingBuffer.append( theClient.isTCPNoDelay( ));

		
		logger.info( "Client for contract '{}' on endpoint '{}' is using configuration: {}", this.contractRoot, this.endpoint.toString(), settingBuffer.toString( ) );
}

	/**
	 * The endpoint that this client will communicate with
	 * @return the endpoint that this client will communicate with
	 */
	public final HttpEndpoint getEndpoint( ) {
		return this.endpoint;		
	}
	
	/**
	 * The root of the contract that this client represents.
	 * It doesn't contain the scheme, domain or port, but the starting of the URL path.
	 * @return the root of the contract
	 */
	public final String getContractRoot( ) {
		return this.contractRoot;
	}
	
	/**
	 * The service contract version to be communicated with.
	 * @return the service contract version to be communicated with
	 */
	public final String getContractVersion( ) {
		return this.contractVersion;
	}
	
	/**
	 * The user agent being sent on all service requests.
	 * @return the user agent being sent on all service requests
	 */
	public final String getUserAgent( ) {
		return this.userAgent;
	}

	/**
	 * Returns the current value of a header that will be overridden.
	 * @param theName the header that was overridden
	 */
	public String getHeaderOverride( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a header name" );
		theName = "override.header." + theName; // tales does this by having 'override.header.' prepended to the header name
		return this.headerOverrides.get( theName );
	}
	
	/**
	 * Let's you set a header override. This is a feature of the Tales framework 
	 * which, if a service is configured to allow them, will allow headers to be 
	 * overridden by a parameter passed on the query string or POST body. This is 
	 * useful for debugging interesting situations, particularly when equipment
	 * like load balancers are the source of the header and therefore not easy 
	 * to reconfigure.
	 * @param theName the name of the header to override
	 * @param theValue the string value to use
	 * @return the ResourceClient again, so these can be strung together
	 */
	public ResourceClient setHeaderOverride( String theName, String theValue ) {
		// this implementation may seem heavy-weight BUT it should be called very
		// rarely and when it does it cannot interfere with any existing calls 
		// that may be using/iterating over the collection
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a header name" );

		theName = "override.header." + theName; // tales does this by having 'override.header.' prepended to the header name
		synchronized( this.overrideLock ) {
			Map<String,String > newOverrides = new HashMap<String, String>( this.headerOverrides );
			if( theValue == null ) {
				newOverrides.remove( theName );
			} else {
				newOverrides.put( theName,  theValue );
			}
			this.headerOverrides = newOverrides;
			this.externalHeaderOverrides = Collections.unmodifiableMap( newOverrides );
		}
		return this;
	}
	
	/**
	 * Returns the current set of known 
	 * @return the map of the current header overrides
	 */
	public Map<String,String> getHeaderOverrides( ) {
		return this.externalHeaderOverrides;
	}
	
	/**
	 * This is used to clear all the header overrides.
	 */
	public void clearHeaderOverrides( ) {
		// this implementation may seem heavy-weight BUT it should be called very
		// rarely and when it does it cannot interfere with any existing calls 
		// that may be using/iterating over the collection
		synchronized( this.overrideLock ) {
			Map<String,String > newOverrides = new HashMap<String, String>( );
			this.headerOverrides = newOverrides;
			this.externalHeaderOverrides = Collections.unmodifiableMap( newOverrides );
		}
	}
	
	/**
	 * Retrieves the method at the specified index.
	 * An exception is thrown if the index is out of bounds.
	 * @param theMethodIndex the index of the method to retrieve
	 * @return the method at the index specified
	 */
	public ResourceMethod getMethod( int theMethodIndex ) {
		Conditions.checkParameter( theMethodIndex >= 0 && theMethodIndex < methods.length, "theMethodIndex", "The specific method index is not within range." );
		
		return methods[ theMethodIndex ];
	}
	
	/**
	 * The underlying http communication client being used.
	 * @return the underlying communication client being used
	 */
	public HttpClient getHttpClient( ) {
		return httpClient;
	}
	
	/**
	 * The underlying json parsing being used.
	 * @return the underlying json parsing being used
	 */
	public JsonParser getJsonParser( ) {
		return jsonParser;
	}
	
	/**
	 * A special type used to validate and parse the response from all method requests.
	 * @return the special type representing all service responses
	 */
	public JsonTypeReference getResultType( ) {
		return resultTypeReference;
	}
	
	/**
	 * The JsonTranslationFacility being used to read and write json.
	 * @return the JsonTranslationFacility being used
	 */
	public JsonTranslationFacility getJsonFacility( ) {
		return this.jsonFacility;
	}
	
	/**
	 * This is called to define a method that can be communicated with on a service. The method path can 
	 * contain path parameters that are to be filled out during request creation.
	 * @param theName the name to given the method, this does not impact execution, but shows up in logs
	 * @param theReturnType the type of the object that is returned
	 * @param theHttpVerb the HTTP verb/method that will be communicated with
	 * @param theMethodPath the relative path (should not have a leading '/') off the contract root for the url to communicate with for the method 
	 * @return
	 */
	public ResourceMethod defineMethod( String theName, Class<?> theReturnType, HttpVerb theHttpVerb, String theMethodPath ) {
		return this.defineMethod(theName, theReturnType, null, theHttpVerb, theMethodPath);
	}

	/**
	 * This is called to define a method that can be communicated with on a service. The method path can 
	 * contain path parameters that are to be filled out during request creation.
	 * @param theName the name to given the method, this does not impact execution, but shows up in logs
	 * @param theReturnType the type of the object that is returned
	 * @param theReturnGenericType the generic type information for the return type of the method
	 * @param theHttpVerb the HTTP verb/method that will be communicated with
	 * @param theMethodPath the relative path (should not have a leading '/') off the contract root for the url to communicate with for the method 
	 * @return
	 */
	public ResourceMethod defineMethod( String theName, Class<?> theReturnType, Type theReturnGenericType, HttpVerb theHttpVerb, String theMethodPath ) {
		return new ResourceMethod( theName, theReturnType, theReturnGenericType, theHttpVerb, theMethodPath, this );
	}

	/**
	 * This is called to generate a request object
	 * @param theMethod the method that is going to be called
	 * @param thePathParameters the path parameters that are needed
	 * @return the ResourceRequest that can be used to talk to the service
	 */
	public ResourceRequest createRequest( ResourceMethod theMethod, Object ... thePathParameters ) {
		return new ResourceRequest( this, theMethod, thePathParameters );
	}
}
