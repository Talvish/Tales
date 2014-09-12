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
package com.tales.services.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tales.contracts.services.http.ResourceMethodResult;
import com.tales.serialization.Readability;
import com.tales.services.OperationContext;
import com.tales.services.OperationContext.Details;
import com.tales.system.Status;

/**
 * Simple class with static methods that provide methods for
 * writting back to the calling part of an http request.
 * @author jmolnar
 *
 */
public class ResponseHelper {
	private static final Logger logger = LoggerFactory.getLogger( ResponseHelper.class );
	
	private static final Gson machineGson = new GsonBuilder( ).serializeNulls( ).create();
	private static final Gson humanGson = new GsonBuilder( ).serializeNulls( ).setPrettyPrinting( ).create();

	/**
	 * Shared helper method to write a failure response to the caller.
	 * @param theRequest the request object used 
	 * @param theResponse The response object used to write back to the caller
	 * @param theStatusCode the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 */
	public static void writeFailure( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode, String theMessage ) {
		_writeResponse( theRequest, theResponse, null, theStatusCode, null, theMessage, null );
	}

	/**
	 * Shared helper method to write a failure response to the caller.
	 * @param theRequest the request object used 
	 * @param theResponse The response object used to write back to the caller
	 * @param theStatusCode the type of failure seen
	 * @param theSubcode the code, specific to the caller, to return
	 * @param theMessage the message to indicate back to the caller
	 */
	public static void writeFailure( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode, String theSubCode, String theMessage ) {
		_writeResponse( theRequest, theResponse, null, theStatusCode, theSubCode, theMessage, null );
	}

	/**
	 * Shared helper method to write a failure response to the caller.
	 * @param theRequest the request object used 
	 * @param theResponse The response object used to write back to the caller
	 * @param theStatusCode the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeFailure( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode,  String theMessage, Throwable theException ) {
		_writeResponse( theRequest, theResponse, null, theStatusCode, null, theMessage, theException );
	}

	/**
	 * Shared helper method to write a failure response to the caller.
	 * @param theRequest the request object used 
	 * @param theResponse The response object used to write back to the caller
	 * @param theStatusCode the type of failure seen
	 * @param theSubcode the code, specific to the caller, to return
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeFailure( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode, String theSubcode, String theMessage, Throwable theException ) {
		_writeResponse(theRequest, theResponse, null, theStatusCode, theSubcode, theMessage, theException);
	}

	/**
	 * Shared helper method to write a failure response to the caller.
	 * @param theRequest the request object used 
	 * @param theResponse The response object used to write back to the caller
	 * @param theFailure the type of failure seen
	 * @param theSubcode the code, specific to the caller, to return
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	private static void _writeResponse( HttpServletRequest theRequest, HttpServletResponse theResponse, JsonElement theObject, Status theCode, String theSubcode, String theMessage, Throwable theException ) {
		try {
			;
			Preconditions.checkNotNull( theResponse, "Need a response object." );
			Preconditions.checkNotNull( theCode, "Need a status code." );
			
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );

			// get the status code to use based on the error from the communication request
			theResponse.setStatus( HttpStatus.convert( theCode ).getCode( ) );
			setCommonHeaders( theResponse );

			JsonObject bodyObject = new JsonObject( );

			// add the main value/result to return
			bodyObject.add( "return", theObject );
			// now add all the operation related values
			addResultMetadata( theRequest, operationContext, theCode, theSubcode, theMessage, theException, bodyObject );			
			
			Gson targetGson = operationContext.getResponseTarget() == Readability.HUMAN ? humanGson: machineGson;
			theResponse.getWriter().write( targetGson.toJson( bodyObject ) );
			
		} catch( Exception e ) {
			// if we cannot write back, then we have to log
			// and we need to build up some form of alert and send as well
			logger.warn(
					String.format( "An error occurred while attempting to send a response of type '%s' with message '%s' to the caller.", theCode, theMessage ),
					e );
		}
		// IF we have DEBUG turned on then we can 
		//    send more over the wire
		//    so DEBUG option is something we record 
		//    for the response back
		// so we need categorization of the type of failures we want to
		// to send and the information we are going to send with it
		// document what it is

	}

	/**
	 * Shared helper method to write a success response to the caller, that has no data.
	 * @param theFailure the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeSuccess( HttpServletRequest theRequest, HttpServletResponse theResponse) {
		_writeResponse( theRequest, theResponse, new JsonObject( ), Status.OPERATION_COMPLETED, null, null, null );
	}
	
	/**
	 * Shared helper method to write a success response to the caller.
	 * @param theResponse The response object used to write back to the caller
	 * @param theFailure the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeSuccess( HttpServletRequest theRequest, HttpServletResponse theResponse, JsonElement theObject ) {
		_writeResponse( theRequest, theResponse, theObject, Status.OPERATION_COMPLETED, null, null, null );
	}
	
	/**
	 * Writes out the response as given by the result from a resource method call.
	 * @param theResult
	 */
	public static void writeResponse( HttpServletRequest theRequest, HttpServletResponse theResponse, ResourceMethodResult theResult ) {
		// always write any header we have and these
		// headers have to be added first in case 
		// there are cache control headers which will 
		// conflict with the headers added by the 
		// by the common header adding
		for( Entry<String,String> entry: theResult.getHeaders().entrySet() ) {
			theResponse.addHeader( entry.getKey(), entry.getValue() );
		}
		
		// adds cookies, if any
		for( Entry<String,Cookie> entry: theResult.getCookies().entrySet() ) {
			theResponse.addCookie( entry.getValue() );
		}
		
		_writeResponse(
				theRequest, 
				theResponse, 
				theResult.getValue( ),
				theResult.getCode( ), 
				theResult.getSubcode( ), 
				theResult.getMessage( ), 
				theResult.getException( ) );
	}
	
	/**
	 * Simple helper method to add operation information to return to the caller.
	 * @param theContext the operation context of request 
	 * @param theContainer the container json object to write the operation information into
	 */
	private static void addResultMetadata( HttpServletRequest theRequest, OperationContext theContext, Status theCode, String theSubcode, String theMessage, Throwable theException, JsonObject theContainer ) {
		JsonObject operationObject = new JsonObject( );

		// first we work on the status object to add to the container
		JsonObject statusObject = new JsonObject( );
		
		statusObject.addProperty( "code", theCode.toString() );

		if( theSubcode != null ) {
			statusObject.addProperty( "subcode", theSubcode );
		}
		if( theMessage != null ) {
			statusObject.addProperty( "message", theMessage );
		}
		if( theException != null ) {
			JsonObject exceptionObject = new JsonObject( );
			
			exceptionObject.addProperty( "type", theException.getClass().getName( ) );
			exceptionObject.addProperty( "message", theException.getMessage( ) );

			if( theContext.getResponseDetails().equals( Details.ALL)) {
				  StringWriter stackTraceWriter = new StringWriter( );
				  theException.printStackTrace( new PrintWriter( stackTraceWriter ) );
				  exceptionObject.addProperty( "stack_trace", stackTraceWriter.toString( ) );
			}
			statusObject.add( "exception", exceptionObject );
		}
		theContainer.add( "status", statusObject );
		
		// then we look at the operation object to add to the container
		operationObject.addProperty( "request_id", theContext.getCurrentRequestId( ) );
		if( theContext.getResponseDetails() == Details.ALL ) {
			operationObject.addProperty( "root_request_id", theContext.getRootRequestId( ) );
			operationObject.addProperty( "parent_request_id", theContext.getParentRequestId( ) );
			// TODO: consider what to do with these
//			operationObject.addProperty( "caller_url", theContext.getCallingUrl( ) );
//			operationObject.addProperty( "caller_user_agent", theContext.getCallingUserAgent( ) );
			operationObject.addProperty( "response_details", theContext.getResponseDetails( ).toString( ) );
			operationObject.addProperty( "response_readability", theContext.getResponseTarget( ).toString( ) );
			operationObject.addProperty( "host_address", theRequest.getLocalAddr( ) );
		}
		//NOTE: this is effectively sharing the same value
		operationObject.addProperty( "elapsed_time", ( ( double )theContext.calculateElapsedTime( ) ) * 0.000001 ); // elapsed time is nanoseconds, but we show as milliseconds
		theContainer.add( "operation", operationObject );
	}
	
	/**
	 * Helper method that writes common headers into the response.
	 * @param theResponse the response object to set headers into
	 */
	public static void setCommonHeaders( HttpServletResponse theResponse ) {
		theResponse.setContentType( "application/json; charset=UTF-8" ); // facebook does text/json
		// if we do not have cache control headers, we set the default
		if( !theResponse.containsHeader( HeaderConstants.CACHE_CONTROL ) ) {
			theResponse.setHeader( HeaderConstants.CACHE_CONTROL, HeaderConstants.CACHE_CONTROL_DEFAULT_DIRECTIVE ); // NOTE: this could be derived based on the data going back
			theResponse.setHeader( HeaderConstants.PRAGMA, HeaderConstants.PRAGMA_DEFAULT_DIRECTIVE );
			theResponse.setHeader( HeaderConstants.EXPIRES, HeaderConstants.EXPIRES_DEFAULT_VALUE );
		}
	}
}
