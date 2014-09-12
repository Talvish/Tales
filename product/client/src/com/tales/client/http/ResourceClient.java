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
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;

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


public class ResourceClient {
	private static final Logger logger = LoggerFactory.getLogger( ResourceClient.class );
	
	// TODO: add debugging support
	// TODO: add SSL supports
	
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
	
	// need a stringtranslator set
	// need a jsontranslator set
	
	// need to consider the needed headers that are returned
	// these aren't formalized in the contract and ideally they
	// are so we can generate clients
	
	// communication parts
	// Hostname/Port
	// SSL
	// URL
	// there are potentially translation needs as well
	
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

	
	protected final String serviceBase; 	// e.g. http://localhost:8000
	protected final String contractRoot;	// e.g. login
	protected final String contractVersion;	// e.g. 20140901
	
	protected final String userAgent; // the user agenet to use
	
	
	// TODO: need to make a comment indicating these shoudl be url encoded, if needed
	public ResourceClient( String theServiceBase, String theContractRoot, String theContractVersion, String theUserAgent ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theServiceBase ), "theServiceBase" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theContractRoot ), "theContractRoot" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theContractVersion ), "theContractVersion" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theUserAgent ), "theUserAgent" );

		
		serviceBase = theServiceBase; // TODO: check for properly url base, but remove the trailing /
		contractRoot = theContractRoot; // TODO: make sure it doesn't have a root / (or does...need to match the server-side)
		contractVersion = theContractVersion; // TODO: make sure this makes the shap we expected
		userAgent = theUserAgent;

		httpClient = new HttpClient( );
		httpClient.setUserAgentField( new HttpField( HttpHeader.USER_AGENT, theUserAgent ) );
		try {
			httpClient.start( );
		} catch (Exception e) {
			// TODO: should throw an exception here
		}
		sslContextFactory = null;
		
		NameValidator jsonHttpFieldNameValidator = new LowerCaseEntityNameValidator();
		NameValidator jsonHttpClassNameValidator = new SegmentedLowercaseEntityNameValidator();
		jsonFacility = new JsonTranslationFacility( 
				new DataContractTypeSource( new DataContractManager( ) ),
				Readability.MACHINE,
				jsonHttpClassNameValidator, 
				jsonHttpFieldNameValidator );
		
		jsonParser = new JsonParser( );
		
		// now that we have the json facility, let's 
		// get the reference for the result type
		JsonTypeMap typeMap = jsonFacility.generateTypeMap( ResourceResult.class );
		resultTypeReference = new JsonTypeReference( 
				ResourceResult.class, typeMap.getReflectedType().getName(),
    			new JsonObjectToObjectTranslator( typeMap ),
    			new ObjectToJsonObjectTranslator( typeMap ) );
	}
	
	public final String getServiceBase( ) {
		return this.serviceBase;		
	}
	
	public final String getContractRoot( ) {
		return this.contractRoot;
	}
	
	public final String getContractVersion( ) {
		return this.contractVersion;
	}
	
	public final String getUserAgent( ) {
		return this.userAgent;
	}
	
	public ResourceClient( String theServiceBase, String theContractRoot, String theContractVersion, String theUserAgent, HttpClient theClient, JsonTranslationFacility theJsonFacility ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theServiceBase ), "theServiceBase" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theContractRoot ), "theContractRoot" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theContractVersion ), "theContractVersion" );
		Conditions.checkParameter( !Strings.isNullOrEmpty( theUserAgent ), "theUserAgent" );
		Conditions.checkParameter( theClient != null, "theClient" );
		Conditions.checkParameter( theJsonFacility != null,  "theJsonFacility" );
		
		serviceBase = theServiceBase; // TODO: check for properly url base, but remove the trailing /
		contractRoot = theContractRoot; // TODO: make sure it doesn't have a root / (or does...need to match the server-side)
		contractVersion = theContractVersion; // TODO: make sure this makes the shap we expected
		userAgent = theUserAgent;

		httpClient = theClient;
		sslContextFactory = null;
		
		jsonFacility = theJsonFacility;
		
		jsonParser = new JsonParser( );
		
		// now that we have the json facility, let's 
		// get the reference for the result type
		JsonTypeMap typeMap = jsonFacility.generateTypeMap( ResourceResult.class );
		resultTypeReference = new JsonTypeReference( 
				ResourceResult.class, typeMap.getReflectedType().getName(),
    			new JsonObjectToObjectTranslator( typeMap ),
    			new ObjectToJsonObjectTranslator( typeMap ) );		
	}
	
	
	public ResourceMethod getMethod( int theMethodIndex ) {
		Conditions.checkParameter( theMethodIndex >= 0 && theMethodIndex < methods.length, "theMethodIndex", "The specific method index is not within range." );
		
		return methods[ theMethodIndex ];
	}
	
	public HttpClient getHttpClient( ) {
		return httpClient;
	}
	
	public JsonParser getJsonParser( ) {
		return jsonParser;
	}
	
	public JsonTypeReference getResourceType( ) {
		return resultTypeReference;
	}
	
	public JsonTranslationFacility getJsonFacility( ) {
		return this.jsonFacility;
	}
	
	public ResourceMethod defineMethod( String theName, Class<?> theReturnType, HttpMethod theHttpVerb, String theMethodPath ) {
		return new ResourceMethod( theName, theReturnType, theHttpVerb, theMethodPath, this );
	}
	
	/**
	 * This is called to generate a request object
	 * @param theMethod
	 * @param thePathParameters
	 * @return
	 */
	public ResourceRequest createRequest( ResourceMethod theMethod, Object ... thePathParameters ) {
		return new ResourceRequest( this, theMethod, thePathParameters );
	}
}
