// ***************************************************************************
// *  Copyright 2017 Joseph Molnar
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
import java.util.List;
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
import com.talvish.tales.communication.HttpStatus;
import com.talvish.tales.communication.Status;
import com.talvish.tales.contracts.services.http.ResourceFacility;
import com.talvish.tales.contracts.services.http.ResourceMethod;
import com.talvish.tales.contracts.services.http.ResourceMethodResult;
import com.talvish.tales.contracts.services.http.ResourceOperation;
import com.talvish.tales.contracts.services.http.ResourceType;
import com.talvish.tales.contracts.services.http.ResourceOperation.Mode;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.services.http.AttributeConstants;
import com.talvish.tales.services.http.FailureSubcodes;
import com.talvish.tales.services.http.ResponseHelper;


/**
 * This servlet is the interface between the servlet engine
 * and an instance of a particular resource class.
 * @author jmolnar
 *
 */
@SuppressWarnings("serial")
public class ResourcePathServlet extends HttpServlet {
	// TODO: have the methods, from the resource type, listed per contract 

	private final List<ResourceMethod> getMethods;
	private final List<ResourceMethod> postMethods;
	private final List<ResourceMethod> putMethods;
	private final List<ResourceMethod> deleteMethods;
	private final List<ResourceMethod> headMethods;

    private final Object resource;
    private final ResourceType resourceType;
    private final ResourceFacility resourceFacility;
    
    private final Executor executor;
    private final long executionTimeout; // TODO: curious about timing out the non-async calls

    
    /**
     * Constructor taking the two main objects needed, the resource and the information
     * about the resource.
     */
    public ResourcePathServlet( Object theResource, ResourceType theResourceType, ResourceFacility theFacility, Executor theExecutor, long theExecutionTimeout ) {
    	Preconditions.checkNotNull( theResource, "need the resource" );
    	Preconditions.checkNotNull( theResourceType, "need a resource type" );
    	Preconditions.checkNotNull(theFacility, "the resource type '%s' needs a resource facility", theResourceType.getName( ) );
    	Preconditions.checkNotNull( theExecutor, "the resource type '%s' needs an executor to run against", theResourceType.getName() );
    	Preconditions.checkArgument( theExecutionTimeout >= 0, "the resource type '%s' needs an execution timeout greater than or equal to 0", theResourceType.getName() );
    	
    	resource = theResource;
    	resourceType = theResourceType;
    	resourceFacility = theFacility;
    	
    	getMethods = theResourceType.getGetMethods();
    	postMethods = theResourceType.getPostMethods();
    	putMethods = theResourceType.getPutMethods();
    	deleteMethods = theResourceType.getDeleteMethods();
    	headMethods = theResourceType.getHeadMethods();

    	executor = theExecutor;
    	executionTimeout = theExecutionTimeout;
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
	 * Implementation of the options method.
	 */
	@Override
	protected void doOptions(HttpServletRequest theRequest, HttpServletResponse theResponse) throws ServletException ,IOException {
		// let's see if this is a COR-preflight request
		
		// TODO: consider if we are doing CORS ...
		//       if so then we need to check for request method headers, origin needs to exists, etc 
		// String accessControlRequestMethod = theRequest.getHeader( HeaderConstants.ACCESS_CONTROL_REQUEST_METHOD );
		// String origin = theRequest.getHeader( HeaderConstants.ORIGIN );
		


		// so for options we find a matching method and if we can find one
		// we indicate which verbs can be used with the method
		ResourceMethod matchingMethod = null;
		
		matchingMethod = getMatchedMethod( theRequest, getMethods );
		if( matchingMethod != null ) {
			ResponseHelper.writeOptionsResponse( theRequest, theResponse, matchingMethod );
			return;
		}
		matchingMethod = getMatchedMethod( theRequest, postMethods );
		if( matchingMethod != null ) {
			ResponseHelper.writeOptionsResponse( theRequest, theResponse, matchingMethod );
			return;
		}
		matchingMethod = getMatchedMethod( theRequest, putMethods );
		if( matchingMethod != null ) {
			ResponseHelper.writeOptionsResponse( theRequest, theResponse, matchingMethod );
			return;
		}
		matchingMethod = getMatchedMethod( theRequest, deleteMethods );
		if( matchingMethod != null ) {
			ResponseHelper.writeOptionsResponse( theRequest, theResponse, matchingMethod );
			return;
		}
		matchingMethod = getMatchedMethod( theRequest, headMethods );
		if( matchingMethod != null ) {
			ResponseHelper.writeOptionsResponse( theRequest, theResponse, matchingMethod );
			return;
		}
	}
	
	/**
	 * A helper method used by the OPTIONS verb to help determine which method
	 * matches the request brough in.
	 * @param theRequest the request itself
	 * @param theMethods the set of methods to go through
	 * @return the matching method, if found
	 */
	private ResourceMethod getMatchedMethod( HttpServletRequest theRequest, List<ResourceMethod> theMethods ) {
		ResourceMethod method = null;
		ResourceMethod.MatchStatus bestStatus = null;
		
		// if we got the methods, then find the particular one
		bestStatus = getMatchStatus( theRequest, theMethods );
		if( bestStatus != null ) {
			method = theMethods.get( bestStatus.getPathIndex( ) );
		}
		return method;
	}
	
	/**
	 * Helper method that gets the best fit method based on a set of methods.
	 * @param theRequest the request, to help make comparisons
	 * @param theSpecificMethods the methods that will be analyzed to find a best fit
	 * @return the best fit method information
	 */
	private ResourceMethod.MatchStatus getMatchStatus( HttpServletRequest theRequest, List<ResourceMethod> theSpecificMethods ) {
		ResourceMethod.MatchStatus currentStatus = null;
		ResourceMethod.MatchStatus bestStatus = null;
		int pathIndex = 0;
		
		// if we got the methods, then find the particular one
		if( theSpecificMethods != null ) {
			for( ResourceMethod method : theSpecificMethods ) {
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
		return bestStatus;
	}

	/**
	 * Private method that implements the work for the http verb methods by running against the methods. 
	 * @param theRequest the http request object
	 * @param theResponse the http response object
	 * @param theMethods the methods which will be looked at to try to find one to run
	 */
	private void doCall(HttpServletRequest theRequest, HttpServletResponse theResponse, List<ResourceMethod> theMethods ) throws ServletException, IOException {
		ResourceMethodResult result = null;
		ResourceMethod.MatchStatus bestStatus = null;
		
		// if we got the methods, then find the particular one
		bestStatus = getMatchStatus( theRequest, theMethods );

		// if we found the particular method, let's run it
		if( bestStatus != null ) {
			ResourceMethod method = theMethods.get( bestStatus.getPathIndex( ) );
			Matcher pathMatcher = bestStatus.getPathMatcher();
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			ResourceOperation.Mode executionMode = method.getUsableMode();

			// so at this point we need to collect up the 
			// request into an object and queue it, if it is async
			// the queue will have a limit on it though so if the 
			// limit is reached it will report back a 503
			if( executionMode == Mode.NONBLOCKING ) {
				// TODO: explore if the onTimeout/Error occurs on a shared thread
				//       and therefore can slow things down if these write failures
				//		 are talking to a slow enough client
				// TODO: since the work still executes when time-outd out, consider
				//       if we need a way to abort/interrupt the working thread to
				//		 shut it down, though behaviour may be call specific

				// need to indicate we are going async
				AsyncContext asyncContext = theRequest.startAsync();
				ResourceAsyncState asyncState = new ResourceAsyncState( asyncContext );
				
				asyncContext.setTimeout( executionTimeout );
				asyncContext.addListener( new AsyncListener( ) {
					@Override
					public void onTimeout(AsyncEvent theEvent) throws IOException {
						// we set completed, and this call was the call to set it
						// then we can write our failures and set to completed
						if( asyncState.setCompleted( ) ) {
							ResponseHelper.writeFailure( theRequest, theResponse, Status.LOCAL_TIMEOUT, null, String.format( "Timed-out executing resource method %s.%s.", resourceType.getName( ), method.getName( ) ), null );
							theEvent.getAsyncContext().complete( );
						}
					}
					@Override
					public void onStartAsync(AsyncEvent theEvent) throws IOException {
						// nothing to do here
					}
					
					@Override
					public void onError(AsyncEvent theEvent) throws IOException {
						// we set completed, and this call was the call to set it
						// then we can write our failures and set to completed
						if( asyncState.setCompleted( ) ) {
							ResponseHelper.writeFailure( theRequest, theResponse, Status.LOCAL_ERROR, FailureSubcodes.UNHANDLED_EXCEPTION, String.format( "Unknown exception executing resource method %s.%s.", resourceType.getName( ), method.getName( ) ), theEvent.getThrowable() );
							theEvent.getAsyncContext().complete( );
						}
					}
					
					@Override
					public void onComplete(AsyncEvent theEvent) throws IOException {
						updateCompletionStatus( method, ( HttpServletResponse )theEvent.getSuppliedResponse( ) );
					}
				});

				// now we place it in the queue for background handling
				// which, if we have hit our limit, will throw the
				// RejectedExecutionException
				try {
					// update we have a call attempt being made
					updateAttemptStatus( method );;
					// at this point we queue for execution 
					executor.execute( ( ) -> {
						ResourceMethodResult asyncResult = null;
						asyncResult = method.execute( resource, theRequest, theResponse, operationContext, pathMatcher, resourceFacility, asyncState );
						// check to make sure that an error/timeout/response has already happened
						if( asyncState.setCompleted( ) ) {
							if( asyncResult != null ) {
								ResponseHelper.writeResponse( theRequest, theResponse, asyncResult );
							} else {
								ResponseHelper.writeFailure( theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path %s maps to resource %s.%s but execution did not return a result.", theRequest.getRequestURL().toString( ), resourceType.getName( ), method.getName( ) ), null );
							}
							asyncContext.complete( );
						}
					} );

				} catch( RejectedExecutionException e ) {
					// TODO: it makes sense, if we can approximate time period, to give a retry header back on when  
					//       to come back given how busy things are ... may want to give control on this mind you
					//       curious if the time period for retry could be a combination of length of queue and 
					//		 average length of execution on the contract along with some other factor
					
					// we set completed, and this call was the call to set it
					// then we can write our failures and set to completed
					if( asyncState.setCompleted( ) ) {
						ResponseHelper.writeFailure(theRequest, theResponse, Status.LOCAL_UNAVAILABLE, null, String.format( "Service too busy to execute '%s.", theRequest.getRequestURL().toString( ) ), null );
						asyncContext.complete( );
					}
				}

			} else {
				// update we have a call attempt being made
				updateAttemptStatus( method );;
				result = method.execute( resource, theRequest, theResponse, operationContext, pathMatcher, resourceFacility, null );
				if( result != null ) {
					try {
						ResponseHelper.writeResponse(theRequest, theResponse, result);
					} finally {
						// update status, which we only do if we have a match
						updateCompletionStatus( method, theResponse );
					}
				} else {
					ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path %s maps to resource %s.%s but execution did not return a result.", theRequest.getRequestURL().toString( ), this.resourceType.getName( ), method.getName( ) ), null );
				}
			}
		} else {
			ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path %s maps to resource %s but an operation could not be found.", theRequest.getRequestURL().toString( ), this.resourceType.getName( ) ), null );
		}
   	}
	
	/**
	 * Private helper method that tracks that a method was called.
	 * @param theMethod the method being called
	 */
	private final void updateAttemptStatus( final ResourceMethod theMethod ) {
		theMethod.getStatus( ).recordReceivedRequest();
	}
	
	/**
	 * Private helper method use to track the success or failure of a particular method on the resource.
	 * @param theMethod the method containing the status to update for
	 * @param theResponse the response to track
	 */
	private final void updateCompletionStatus( final ResourceMethod theMethod, final HttpServletResponse theResponse ) {
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
