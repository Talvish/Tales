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
package com.talvish.tales.services.http.servlets;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.talvish.tales.communication.HttpEndpoint;
import com.talvish.tales.contracts.ContractVersion;
import com.talvish.tales.contracts.Subcontract;
import com.talvish.tales.contracts.services.ServiceContract;
import com.talvish.tales.contracts.services.http.HttpResourceContract;
import com.talvish.tales.contracts.services.http.ResourceMethod;
import com.talvish.tales.contracts.services.http.ResourceMethodParameter;
import com.talvish.tales.contracts.services.http.ServletContract;
import com.talvish.tales.contracts.services.http.ResourceMethodParameter.ParameterSource;
import com.talvish.tales.serialization.json.JsonMemberMap;
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.serialization.json.JsonTypeMap;
import com.talvish.tales.services.Interface;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.services.Service;
import com.talvish.tales.services.OperationContext.Details;
import com.talvish.tales.services.http.AttributeConstants;
import com.talvish.tales.services.http.HttpInterfaceBase;
import com.talvish.tales.services.http.ResponseHelper;


/**
 * This is a simple servlet that shows the contracts used by the associated service.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.contracts", versions="20121221")
@SuppressWarnings("serial")
public class ContractsServlet extends AdministrationServlet {
	private static final Logger logger = LoggerFactory.getLogger( ContractsServlet.class );
    
    /**
     * Empty, default constructor.
     */
    public ContractsServlet( ) {
    }
    
	/**
	 * Implementation of the get method to get contract information.
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Implementation of the post method to get contract information.
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Private implementation of the request method which gets the contract information.
	 */ 
	private void doCall( HttpServletRequest theRequest, HttpServletResponse theResponse ) {
//		if( theRequest.getParameterMap().size() != 0 ) {
//			HttpService.writeFailure( 
//					theRequest,
//					theResponse, 
//					Failure.CALLER_BAD_DATA, 
//					"Parameters are not expected.",
//					null );
//		} else {
			logger.info( "Request made to list the contracts." );
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );

			Service service = getService( );
			JsonTranslationFacility jsonFacility = service.getFacility( JsonTranslationFacility.class );

			JsonObject serviceObject = new JsonObject( );
			JsonArray interfacesArray = new JsonArray( );
			JsonObject interfaceObject;
			
			serviceObject.addProperty( "name", service.getCanonicalName( ) );
//			if( operationContext.getResponseDetails() == Details.ALL ) {
//				serviceObject.addProperty( "friendly_name", service.getFriendlyName( ) );
//				serviceObject.addProperty( "description", service.getDescription( ) );
//			}

			for( Interface serviceInterface : getService( ).getInterfaceManager().getInterfaces() ) {
				interfaceObject = jsonifyInterface( serviceInterface, jsonFacility, operationContext );
				interfacesArray.add( interfaceObject );
			}
			serviceObject.add( "interfaces", interfacesArray );

//			TODO: consider another way to get this data
//			for( Interface httpInterface : service.getInterfaceManager().getInterfaces() ) {
//				QueuedThreadPool pool = ( QueuedThreadPool )( ( HttpInterface )httpInterface ).getHttpServletServer().getThreadPool();
//
//				pool.setDetailedDump( true );
//				logger.info( "LOOK AT ME I'M DUMPING FOR '{}': {}", httpInterface.getName(), pool.dump() );
//			}
			

			
			ResponseHelper.writeSuccess(theRequest, theResponse, serviceObject );
//		}
	}
	
	/**
	 * A helper method to get information from the interface and turn into a json object.
	 * @param theInterface the interface to json-ify
	 * @param theArray the array to play the json objects info
	 */
	private JsonObject jsonifyInterface( Interface theInterface, JsonTranslationFacility theJsonFacility, OperationContext theOperationContext ) {
		JsonObject interfaceObject;
		JsonArray contractArray;
		JsonObject contractObject;
		
		interfaceObject = new JsonObject( );
		interfaceObject.addProperty( "name", theInterface.getName() );
		interfaceObject.addProperty( "type", theInterface.getClass().getSimpleName() );
		
		if( theInterface instanceof HttpInterfaceBase ) {
			JsonArray endpointArray;
			JsonObject endpointObject;
			
			HttpInterfaceBase httpInterface = ( HttpInterfaceBase )theInterface;
			Collection<HttpEndpoint> endpoints = httpInterface.getEndpoints( );
			
			endpointArray = new JsonArray( );
			
			for( HttpEndpoint endpoint : endpoints ) {
				endpointObject = new JsonObject( );
				endpointObject.addProperty( "scheme", endpoint.getScheme( ) );
				endpointObject.addProperty( "host", endpoint.getHost( ) );
				endpointObject.addProperty( "port", endpoint.getPort( ) );
				endpointArray.add( endpointObject );
			}
			interfaceObject.add( "endpoints", endpointArray );
		}
		
		contractArray = new JsonArray( );
		for( ServiceContract contract : theInterface.getBoundContracts( ) ) {
			contractObject = jsonifyContract( contract, theJsonFacility, theOperationContext );
			contractArray.add( contractObject );
		}
		interfaceObject.add( "contracts", contractArray );
		return interfaceObject;
	}
	
	/**
	 * Takes a contract object and turns it into a JSON string.
	 * @param theContract the contract to jsonify
	 * @param theOperationContext the context it is running within
	 * @return the json object representing the contract
	 */
	private JsonObject jsonifyContract( ServiceContract theContract, JsonTranslationFacility theJsonFacility, OperationContext theOperationContext ) {
		JsonObject contractObject;
		JsonArray versionArray;
		
		JsonArray subcontractArray;
		JsonObject subcontractObject;
		
		JsonArray typeArray;
		JsonObject typeObject;
		JsonArray typeMemberArray;
		JsonObject typeMemberObject;
		
		JsonObject parameterObject;
		JsonArray parameterArray;
		
		JsonArray verbArray;
		
		Set<JsonTypeMap> jsonTypeMaps = new HashSet<JsonTypeMap>( );

		// NOTE: probably need a 'type' on the contract that can be shared and used to figure out how to handle the json result contract object

		contractObject = new JsonObject( );
		contractObject.addProperty( "name", theContract.getName( ) );
		if( theOperationContext.getResponseDetails() == Details.ALL ) {
			contractObject.addProperty( "description", theContract.getDescription( ) );
		}
		if( theContract instanceof HttpResourceContract ) {
			HttpResourceContract resourceContract = ( HttpResourceContract )theContract;
			
			contractObject.addProperty( "path", resourceContract.getBoundPath( ) );
		}
		//contractObject.addProperty( "url", String.format( "http://%1$s:%2$s%3$s", !Strings.isNullOrEmpty( theInterface.getHost( ) ) ? theInterface.getHost() : "{all}", theInterface.getPort(), contract.getBoundPath( ) ) );
		versionArray = new JsonArray( );
		for( ContractVersion version : theContract.getSupportedVersions( ) ) {
			versionArray.add( new JsonPrimitive( version.getVersionString( ) ) );
		}
		contractObject.add( "versions", versionArray );
		
		if( theContract.getSubcontracts( ).size( ) > 0 ) {
			subcontractArray = new JsonArray( ); 
			for( Subcontract subcontract : theContract.getSubcontracts() ) {
				subcontractObject = new JsonObject( );
				subcontractObject.addProperty( "name", subcontract.getName( ) );
				if( theOperationContext.getResponseDetails() == Details.ALL ) {
					subcontractObject.addProperty( "description", subcontract.getDescription( ) );
				}
				if( subcontract instanceof ResourceMethod ) {
					ResourceMethod method = ( ResourceMethod )subcontract;
					
					// TODO: look at this once signed responses are in
					//subcontractObject.addProperty( "signed_request", method.getSignedRequest( ).toString( ) );
					//subcontractObject.addProperty( "signed_response", method.getSignedResponse( ).toString( ) );

					verbArray = new JsonArray();
					for( String verb : method.getVerbs( ) ){
						verbArray.add( new JsonPrimitive( verb ) );
					}
					subcontractObject.add( "verbs", verbArray );
					subcontractObject.addProperty( "path", method.getParameterPath( ) );
					subcontractObject.addProperty( "return_type", theJsonFacility.generateTypeName( method.getReturn().getType( ), jsonTypeMaps ) );
					parameterArray = new JsonArray( );
					for( ResourceMethodParameter parameter : method.getParameters( ) ) {
						if( parameter.getSource( ) != ParameterSource.CONTEXT ) {
							parameterObject = new JsonObject( );
							parameterObject.addProperty( "name", parameter.getValueName( ) );
							parameterObject.addProperty( "type", theJsonFacility.generateTypeName( parameter.getType( ), jsonTypeMaps ) );
							parameterObject.addProperty( "source", parameter.getSource( ).toString( ) );
							parameterArray.add( parameterObject );
						}
					}
					subcontractObject.add( "parameters", parameterArray );
				}
				versionArray = new JsonArray( );
				for( ContractVersion version : subcontract.getSupportedVersions( ) ) {
					versionArray.add( new JsonPrimitive( version.getVersionString( ) ) );
				}
				subcontractObject.add( "versions", versionArray );
				subcontractArray.add( subcontractObject );
			}
			contractObject.add( "subcontracts", subcontractArray );
			
			typeArray = new JsonArray( );
			for( JsonTypeMap typeMap : jsonTypeMaps ) {
				typeObject = new JsonObject( );
				typeObject.addProperty( "name", typeMap.getReflectedType().getName( ) );
				typeMemberArray = new JsonArray( ); 
				for( JsonMemberMap memberMap : typeMap.getMembers( ) ) {
					typeMemberObject = new JsonObject( );
					typeMemberObject.addProperty( "name", memberMap.getReflectedField().getName( ) );
					if( memberMap.getReflectedField().isMap( ) ) { // we are a hashmap of some kind
						// we have key types, value types
					} else if( memberMap.getReflectedField( ).isCollection() ) { // we are an array or collection
						// we have value types
					} else { // we are an object
						//
					}
				
					// TODO: more than one type actually exists here, so we need to re-think this
					typeMemberObject.addProperty( "type", theJsonFacility.generateTypeName( memberMap.getReflectedField().getSite().getType(), null ) );
					typeMemberArray.add( typeMemberObject );
				}
				typeObject.add( "members", typeMemberArray );
				typeArray.add( typeObject );
			}
			contractObject.add( "types", typeArray );
		}
		
		return contractObject;
	}
}
