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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;
import com.tales.communication.HttpStatus;
import com.tales.communication.Status;
import com.tales.contracts.ContractVersion;
import com.tales.contracts.services.http.ResourceFacility;
import com.tales.contracts.services.http.ResourceMethod;
import com.tales.contracts.services.http.ResourceMethodResult;
import com.tales.contracts.services.http.ResourceOperation;
import com.tales.contracts.services.http.ResourceOperation.Mode;
import com.tales.contracts.services.http.ResourceType;
import com.tales.services.OperationContext;
import com.tales.services.http.AttributeConstants;
import com.tales.services.http.FailureSubcodes;
import com.tales.services.http.ParameterConstants;
import com.tales.services.http.ResponseHelper;


/**
 * This servlet is the interface between the servlet engine
 * and an instance of a particular resource class.
 * @author jmolnar
 *
 */
@SuppressWarnings("serial")
public class ResourceServlet extends HttpServlet {
	// TODO: have the methods, from the resource type, listed per contract 

	private Map<String,List<ResourceMethod>> getMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> postMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> putMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> deleteMethods = new HashMap<String,List<ResourceMethod>>( );
	private Map<String,List<ResourceMethod>> headMethods = new HashMap<String,List<ResourceMethod>>( );

    private final Object resource;
    private final ResourceType resourceType;
    private final ResourceFacility resourceFacility;
    
    private final Executor executor;
    private final long executionTimeout; // TODO: curious about timing out the non-async calls

    
    /**
     * Constructor taking the two main objects needed, the resource and the information
     * about the resource.
     */
    public ResourceServlet( Object theResource, ResourceType theResourceType, ResourceFacility theFacility, Executor theExecutor, long theExecutionTimeout ) {
    	Preconditions.checkNotNull( theResource, "need the resource" );
    	Preconditions.checkNotNull( theResourceType, "need a resource type" );
    	Preconditions.checkNotNull(theFacility, "the resource type '%s' needs a resource facility", theResourceType.getName( ) );
    	Preconditions.checkNotNull( theExecutor, "the resource type '%s' needs an executor to run against", theResourceType.getName() );
    	Preconditions.checkArgument( theExecutionTimeout >= 0, "the resource type '%s' needs an execution timeout greater than or equal to 0", theResourceType.getName() );
    	
    	resource = theResource;
    	resourceType = theResourceType;
    	resourceFacility = theFacility;
    	
    	filterMethods( theResourceType.getGetMethods(), getMethods );
    	filterMethods( theResourceType.getPostMethods(), postMethods );
    	filterMethods( theResourceType.getPutMethods(), putMethods );
    	filterMethods( theResourceType.getDeleteMethods(), deleteMethods );
    	filterMethods( theResourceType.getHeadMethods(), headMethods );

    	executor = theExecutor;
    	executionTimeout = theExecutionTimeout;
    }
    
    /**
     * Filters methods into the particular right map
     * @param theMethods the methods to filter
     * @param theContractMap the map to filter into
     */
    private void filterMethods( List<ResourceMethod> theMethods, Map<String,List<ResourceMethod>> theContractMap ) {
    	for( ResourceMethod method : theMethods ) {
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
			Matcher pathMatcher = bestStatus.getPathMatcher();
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			ResourceOperation.Mode executionMode = method.getUsableMode();
			
			// so at this point we need to collect up the 
			// request into an object and queue it, if it is async
			// the queue will have a limit on it though so if the 
			// limit is reached it will report back a 503
			if( executionMode == Mode.NONBLOCKING ) {
				try {
					// need to indicate we are going async
					AsyncContext asyncContext = theRequest.startAsync();
					
					asyncContext.setTimeout( executionTimeout );
					asyncContext.addListener( new AsyncListener( ) {
						@Override
						public void onTimeout(AsyncEvent theEvent) throws IOException {
							ResponseHelper.writeFailure( theRequest, theResponse, Status.LOCAL_TIMEOUT, null, String.format( "Timed-out executing resource method '%s.%s'.", resourceType.getName( ), method.getName( ) ), null );
							theEvent.getAsyncContext().complete( );
						}
						@Override
						public void onStartAsync(AsyncEvent theEvent) throws IOException {
							// nothing to do here
						}
						
						@Override
						public void onError(AsyncEvent theEvent) throws IOException {
							ResponseHelper.writeFailure( theRequest, theResponse, Status.LOCAL_ERROR, FailureSubcodes.UNHANDLED_EXCEPTION, String.format( "Unknown exception executing resource method '%s.%s'.", resourceType.getName( ), method.getName( ) ), null );
							theEvent.getAsyncContext().complete( );
						}
						
						@Override
						public void onComplete(AsyncEvent theEvent) throws IOException {
							updateStatus( method, ( HttpServletResponse )theEvent.getSuppliedResponse( ) );
						}
					});

					// at this point we queue for execution, which is 
					executor.execute( ( ) -> {
							ResourceMethodResult asyncResult = null;
							asyncResult = method.execute( resource, theRequest, theResponse, operationContext, pathMatcher, resourceFacility );
							if( asyncResult != null ) {
								ResponseHelper.writeResponse (theRequest, theResponse, asyncResult);
							} else {
								ResponseHelper.writeFailure( theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path '%s' maps to resource '%s.%s' but execution did not return a result.", theRequest.getRequestURL().toString( ), resourceType.getName( ), method.getName( ) ), null );
							}

							asyncContext.complete( );
					} );

				} catch( RejectedExecutionException e ) {
					// TODO: at his point it makes sense to give a retry header back on when to 
					//       come back given how busy things are ... may want to give control on 
					//       this mind you
					ResponseHelper.writeFailure(theRequest, theResponse, Status.LOCAL_UNAVAILABLE, null, String.format( "Service too busy to execute '%s.", theRequest.getRequestURL().toString( ) ), null );
				}
				

			} else {
				result = method.execute( resource, theRequest, theResponse, operationContext, pathMatcher, resourceFacility );
				if( result != null ) {
					try {
						ResponseHelper.writeResponse(theRequest, theResponse, result);
					} finally {
						// update status, which we only do if we have a match
						updateStatus( method, theResponse );
					}
				} else {
					ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path '%s' maps to resource '%s.%s' but execution did not return a result.", theRequest.getRequestURL().toString( ), this.resourceType.getName( ), method.getName( ) ), null );
				}
			}
		} else {
			ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path '%s' maps to resource '%s' but an operation could not be found.", theRequest.getRequestURL().toString( ), this.resourceType.getName( ) ), null );
		}
   	}
	
	/**
	 * Private helper method use to track the success or failure of a particular method on the resource.
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
