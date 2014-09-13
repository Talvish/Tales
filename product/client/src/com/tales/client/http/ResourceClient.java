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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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
//	// so we need to see if we have SSL settings for this interface
//	String sslKeyStoreConfigName = String.format( ConfigurationConstants.HTTP_INTERFACE_SSL_KEY_STORE, theName );
//	String sslCertAliasConfigName = String.format( ConfigurationConstants.HTTP_INTERFACE_SSL_CERT_ALIAS, theName );
//	// if we have a key store defined on the  service and if so create the ssl factory
//	if( service.getConfigurationManager().contains( sslKeyStoreConfigName ) ) {
//		String keyStoreName = service.getConfigurationManager().getStringValue( sslKeyStoreConfigName ) ;
//		String certAlias = service.getConfigurationManager().getStringValue( sslCertAliasConfigName, "" );
//		try {
//			KeyStore keyStore = service.getKeyStoreManager().getKeyStore( keyStoreName );
//			
//			if( keyStore == null ) {
//				throw new ConfigurationException( String.format( "Interface '%s' is attempting to use a non-existent key store called '%s'.", theName, keyStoreName ) );
//			} else {
//				sslFactory = new SslContextFactory();
//				sslFactory.setKeyStore( keyStore );
//				// if we have the cert alias available, then we use
//
//				
//				if( !Strings.isNullOrEmpty( certAlias ) ) {
//					if( !keyStore.containsAlias( certAlias ) ) {
//						throw new ConfigurationException( String.format( "Interface '%s' is attempting to use a non-existent certificate alias '%s' on key store '%s'.", theName, certAlias, keyStoreName ) );
//					} else {
//						sslFactory.setCertAlias( certAlias );
//					}
//				}
//				// oddly we need to grab the key again, even though the store is open
//				// I'm not very happy with having to do this, but Jetty needs the password again
//				sslFactory.setKeyStorePassword( service.getConfigurationManager().getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_PASSWORD_FORMAT, keyStoreName ) ) );
//			}
//		} catch( KeyStoreException e ) {
//			throw new IllegalStateException( String.format( "Interface '%s' is using an invalid key store called '%s'.", theName, keyStoreName ) );
//		}
//	} else {
//		sslFactory = null;
//	}
	
	// make sure there is a way to share connection/thread pools across
	// so if talking to a lot of services, there isn't a huge overhead
	
	// options
	// - header: overrides (we can format the names)
	// - header: X-Root-Request-Id
	// - header: X-Parent-Request-Id
	// - query param: override.response.details
	// - query param: override.response.readability
	// 
	
	// need to have an array for the different methods
	protected final HttpClient httpClient;
	protected final SslContextFactory sslContextFactory; // this likely isn't needed since it could be done on setup

	protected ResourceMethod[] methods;
	
	protected final JsonTranslationFacility jsonFacility;
	protected final JsonTypeReference resultTypeReference;
	
	protected final JsonParser jsonParser;
	
	protected final HttpEndpoint endpoint; 	// e.g. http://localhost:8000
	protected final String contractRoot;	// e.g. login
	protected final String contractVersion;	// e.g. 20140901
	
	protected final String userAgent; // the user agent to use
	

	/**
	 * Creates a resource client that will create the underlying HttpClient to talk to the
	 * service and a default JsonTypeFacility to read and generate json. The endpoint and contract root 
	 * should already have url encoded anything that needs url encoding.
	 * @param theEndpoint the end point to talk to which should be of the form http(s)?//name:port, e.g. http://localhost:8000
	 * @param theContractRoot the contract root to talk to, which is of the form /name, e.g. /login
	 * @param theContractVersion the version of the contract which is a date of the form yyyyMMDD, e.g. 20140925
	 * @param theUserAgent the user agent that this client should use
	 */
	public ResourceClient( String theEndpoint, String theContractRoot, String theContractVersion, String theUserAgent ) {
		this( theEndpoint, theContractRoot, theContractVersion, theUserAgent, null, null );
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
	public ResourceClient( String theEndpoint, String theContractRoot, String theContractVersion, String theUserAgent, HttpClient theClient, JsonTranslationFacility theJsonFacility ) {
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
			httpClient = new HttpClient( );
			try {
				httpClient.start( );
			} catch (Exception e ) {
				throw new IllegalStateException( "unable to create the resource client due to the inability to start the HttpClient", e );
			}
			sslContextFactory = null;
		} else {
			httpClient = theClient;
			sslContextFactory = null;
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
		return new ResourceMethod( theName, theReturnType, theHttpVerb, theMethodPath, this );
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
