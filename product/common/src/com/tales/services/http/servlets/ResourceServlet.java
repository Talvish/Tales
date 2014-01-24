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
package com.tales.services.http.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import com.tales.contracts.ContractVersion;
import com.tales.contracts.services.http.ResourceFacility;
import com.tales.contracts.services.http.ResourceMethod;
import com.tales.contracts.services.http.ResourceMethodResult;
import com.tales.contracts.services.http.ResourceOperation.Signed;
import com.tales.contracts.services.http.ResourceType;
import com.tales.services.KeySource;
import com.tales.services.Status;
import com.tales.services.OperationContext;
import com.tales.services.http.AttributeConstants;
import com.tales.services.http.FailureSubcodes;
import com.tales.services.http.ParameterConstants;
import com.tales.services.http.HttpStatus;
import com.tales.services.http.ResponseHelper;


/**
 * This servlet is the interface between the servlet engine
 * and an instance of a particular resource class.
 * @author jmolnar
 *
 */
@SuppressWarnings("serial")
public class ResourceServlet extends HttpServlet {
	// TODO: have the methods, from the resource type, listed per 
	//       contract 

	private Map<String,List<ResourceMethod>> getMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> postMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> putMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> deleteMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> headMethods = new HashMap<String,List<ResourceMethod>>( );

	private final KeySource<HttpServletRequest> keySource;
	
    private final Object resource;
    private final ResourceType resourceType;
    private final ResourceFacility resourceFacility;

    //http://www.java2s.com/Tutorial/Java/0490__Security/SimpleDigitalSignatureExample.htm
    //http://stackoverflow.com/questions/521101/using-sha1-and-rsa-with-java-security-signature-vs-messagedigest-and-cipher
    
    /**
     * Constructor taking the two main objects needed, the resource and the information
     * about the resource.
     */
    public ResourceServlet( Object theResource, ResourceType theResourceType, ResourceFacility theFacility, KeySource<HttpServletRequest > theKeySource ) {
    	Preconditions.checkNotNull(theResource, "need the resource");
    	Preconditions.checkNotNull(theResourceType, "need a resource type");
    	Preconditions.checkNotNull(theFacility, "needs a resource facility");
    	
    	resource = theResource;
    	resourceType = theResourceType;
    	resourceFacility = theFacility;
    	keySource = theKeySource;
    	
    	filterMethods( theResourceType.getGetMethods(), getMethods );
    	filterMethods( theResourceType.getPostMethods(), postMethods );
    	filterMethods( theResourceType.getPutMethods(), putMethods );
    	filterMethods( theResourceType.getDeleteMethods(), deleteMethods );
    	filterMethods( theResourceType.getHeadMethods(), headMethods );
    }
    
    /**
     * Filters methods into the particular right map
     * @param theMethods the methods to filter
     * @param theContractMap the map to filter into
     */
    private void filterMethods( List<ResourceMethod> theMethods, Map<String,List<ResourceMethod>> theContractMap ) {
    	for( ResourceMethod method : theMethods ) {
    		if( ( method.getSignedRequest() != Signed.NO || method.getSignedResponse() != Signed.NO ) && this.keySource == null ) {
    			// makes sure that if a method indicates it has signed, that we have a resource key to use
    			throw new IllegalStateException( String.format( "Resource method '%s.%s' supports signed but the resource was not bound with a key source.", resourceType.getName(), method.getName( ) ) );
    		}
    		for( ContractVersion contractVersion : method.getSupportedVersions( ) ) {
    			String stringContractVersion = contractVersion.getVersionString( );
    			List<ResourceMethod> contractMethods = theContractMap.get( stringContractVersion );
    			if( contractMethods == null ) {
    				contractMethods = new ArrayList<ResourceMethod>( 2 );
    				theContractMap.put( stringContractVersion, contractMethods );
    			}
    			// we maintain the order from the original list method
    			contractMethods.add( method );
    		}
    	}
    }
    
	/**
	 * Implementation of the get method.
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse, getMethods );
   	}

	/**
	 * Implementation of the post method.
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse, postMethods );
   	}

	/**
	 * Implementation of the put method.
	 */
	@Override
	protected void doPut(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse, putMethods );
   	}

	/**
	 * Implementation of the delete method.
	 */
	@Override
	protected void doDelete(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse, deleteMethods );
   	}
	
	/**
	 * Implementation of the head method.
	 */
	@Override
	protected void doHead(HttpServletRequest theRequest, HttpServletResponse theResponse) throws ServletException ,IOException {
		doCall( theRequest, theResponse, headMethods );
	}
	
//	/**
//	 * Private method that implements the work for the http verb methods by running against the methods. 
//	 * @param theRequest the http request object
//	 * @param theResponse the http response object
//	 * @param theMethods the methods which will be looked at to try to find one to run
//	 */
//	private void doCall(HttpServletRequest theRequest, HttpServletResponse theResponse, Collection<ResourceMethod> theMethods ) throws ServletException, IOException {
//		ResourceMethodResult result = null;
//		for( ResourceMethod method : theMethods ) {
//			result = method.execute( resource, theRequest, theResponse, resourceFacility );
//			if( result != null ) {
//				try {
//					if( !result.failed() ) {
//						ResponseHelper.writeSuccess( theRequest, theResponse, result.getValue() );
//					} else {
//						ResponseHelper.writeFailure(theRequest, theResponse, result.getFailure(), result.getHandlerCode(), result.getHandlerMessage(), result.getException() );
//					}
//					break;
//				} finally {
//					// update status, which we only do if we have a match
//					updateStatus( method, theResponse );
//					
//				}
//			}
//		}
//	    // if one of the methods didn't run then we throw a failure back
//		if( result == null ) {
//			ResponseHelper.writeFailure(theRequest, theResponse, Failure.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Could not find an appropriate operation to execte on '%s'.", resourceType.getRootPath( ) ), null );
//		}
//   	}
	
	/**
	 * Private method that implements the work for the http verb methods by running against the methods. 
	 * @param theRequest the http request object
	 * @param theResponse the http response object
	 * @param theMethods the methods which will be looked at to try to find one to run
	 */
	private void doCall(HttpServletRequest theRequest, HttpServletResponse theResponse, Map<String,List<ResourceMethod>> theMethods ) throws ServletException, IOException {
		ResourceMethodResult result = null;
		ResourceMethod.MatchStatus bestStatus = null;
		ResourceMethod.MatchStatus currentStatus = null;
		int pathIndex = 0;

		// grab the version of the resource methods that are appropriate
		List<ResourceMethod> specificMethods = theMethods.get( theRequest.getParameter( ParameterConstants.VERSION_PARAMETER ) );

		// if we got the methods, then find the particular one
		if( specificMethods != null ) {
			for( ResourceMethod method : specificMethods ) {
				currentStatus = method.match(theRequest, pathIndex );
				if( currentStatus != null ) {
					if( ( bestStatus == null ) || 
						( bestStatus.getParameterMisses() > currentStatus.getParameterMisses( ) && bestStatus.getParameterMatches() <= currentStatus.getParameterMatches( ) ) || 
						( bestStatus.getParameterMisses() >= currentStatus.getParameterMisses( ) && bestStatus.getParameterMatches() < currentStatus.getParameterMatches( ) ) ) {
						bestStatus = currentStatus;
//						if( bestStatus.getParameterMisses() == 0 && bestStatus.getParameterMatches() == method.getParameters().size( ) ) {
//							// short circuit if we have an exact match <- this didn't work because I don't have a count for non-used parameters
//							break;
//						}
					}
				}
				pathIndex += 1;
			}
		}
		// if we found the particular method, let's run it
		if( bestStatus != null ) {
			ResourceMethod method = specificMethods.get( bestStatus.getPathIndex( ) );
//			ResourceOperation.Signed signedRequest = method.getSignedRequest( );
//			// if no and we have a signature, complain
//			// if yes and we don't have a signature, complain
//			// if optional, store if we got one to verify later
//			String requestSignature = theRequest.getParameter( ParameterConstants.SIGNATURE_PARAMETER );
//			if( requestSignature == null && signedRequest == Signed.REQUIRED || 
//				requestSignature != null && signedRequest == Signed.NO ) {
//				// TODO: indicate we have a problem
//			}
			
//			// at this point if we have a signature then we know it can be signed
//			if( requestSignature != null ) {
//				// TODO: verify the signature
//				// this will need to be pulled out so can be used in response
//				KeyPair keyPair = this.keySource.getKeys( theRequest );
//			}
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			result = method.execute( resource, theRequest, theResponse, operationContext, bestStatus.getPathMatcher(), resourceFacility );
			if( result != null ) {
				try {
//					ResourceOperation.Signed signedResponse = method.getSignedResponse();
//					// if no, do nothing
//					// if yes, then sign
//					// if optional, then sign if incoming was signed
//					if( signedResponse == Signed.REQUIRED || ( signedResponse == Signed.OPTIONAL && requestSignature != null ) ) {
//						// TODO: sign the response
//					} else {
						// else no need to sign the response
						ResponseHelper.writeResponse(theRequest, theResponse, result);
//					}
				} finally {
					// update status, which we only do if we have a match
					updateStatus( method, theResponse );
					
				}
			}
		}

		// if one of the methods didn't run then we throw a failure back
		if( result == null ) {
			ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Could not find an appropriate operation to execte on '%s'.", resourceType.getRootPath( ) ), null );
		}
   	}
	
	/**
	 * Private helper method use to track the success or failure of call.
	 * @param theMethod the method containing the status to update for
	 * @param theResponse the response to track
	 */
	private void updateStatus( ResourceMethod theMethod, HttpServletResponse theResponse ) {
		int status = theResponse.getStatus( );
		
		if( !HttpStatus.isError( status ) ) {
			theMethod.getStatus( ).recordSuccess();
			
		} else if( HttpStatus.isClientError( status ) ) {
			theMethod.getStatus( ).recordClientError();
			
		} else if( HttpStatus.isDependentError( status ) ) {
			theMethod.getStatus( ).recordDependentError();
			
		} else if( HttpStatus.isUnavailableError( status ) ) {
			theMethod.getStatus( ).recordUnavailableError();
			
		} else { // presume local error for all others
			theMethod.getStatus( ).recordLocalError();
		}
	}
}
