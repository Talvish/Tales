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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.tales.contracts.services.http.ResourceMethodResult;
import com.tales.serialization.Readability;
import com.tales.services.Status;
import com.tales.services.OperationContext;
import com.tales.services.OperationContext.Details;

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
		writeFailure( theRequest, theResponse, theStatusCode, null, theMessage, null );
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
		writeFailure( theRequest, theResponse, theStatusCode, theSubCode, theMessage, null );
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
		writeFailure( theRequest, theResponse, theStatusCode, null, theMessage, theException );
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
		_writeFailure(theRequest, theResponse, theStatusCode, theSubcode, theMessage, theException);
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
	private static void _writeFailure( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode, String theSubcode, String theMessage, Throwable theException ) {
		try {
			;
			// TODO: ensure it is an error code coming in . . .
			Preconditions.checkNotNull( theResponse, "Need a response object." );
			Preconditions.checkNotNull( theStatusCode, "Need a failure status code." );
			
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );

			// get the status code to use based on the error from the communication request
			theResponse.setStatus( HttpStatus.convert( theStatusCode ).getCode( ) );
			setCommonHeaders( theResponse );
			
			JsonObject bodyObject = new JsonObject( );
			JsonObject errorObject = new JsonObject( );
			
			errorObject.addProperty( "code", theStatusCode.toString() );
			if( !Strings.isNullOrEmpty( theSubcode ) ) {
				errorObject.addProperty( "subcode", theSubcode );
			}
			errorObject.addProperty( "message", theMessage );
			if( theException != null ) {
				JsonObject exceptionObject = new JsonObject( );
				
				exceptionObject.addProperty( "type", theException.getClass().getName( ) );
				exceptionObject.addProperty( "message", theException.getMessage( ) );

				if( operationContext.getResponseDetails().equals( Details.ALL)) {
					  StringWriter stackTraceWriter = new StringWriter( );
					  theException.printStackTrace( new PrintWriter( stackTraceWriter ) );
					  exceptionObject.addProperty( "stack_trace", stackTraceWriter.toString( ) );
				}
				errorObject.add( "exception", exceptionObject );
			}
			bodyObject.add( "error", errorObject );

			// now add the operation object and send out the resposne
			Gson targetGson = operationContext.getResponseTarget() == Readability.HUMAN ? humanGson: machineGson;

			addOperation( theRequest, operationContext, bodyObject );			
			theResponse.getWriter().write( targetGson.toJson( bodyObject ) );
			
		} catch( Exception e ) {
			// if we cannot write back, then we have to log
			// and we need to build up some form of alert and send as well
			logger.warn(
					String.format( "An error occurred while attempting to send a failure of type '%s' with message '%s' to the caller.", theStatusCode, theMessage ),
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

	// TODO: consider a Success object which is taken the success case
	// 		 they are: OK, OK CREATED, OK_ASYC
	/**
	 * Shared helper method to write a success response to the caller, that has no data.
	 * @param theFailure the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeSuccess( HttpServletRequest theRequest, HttpServletResponse theResponse) {
		writeSuccess( theRequest, theResponse, new JsonObject( ) );
	}
	
	/**
	 * Shared helper method to write a success response to the caller.
	 * @param theResponse The response object used to write back to the caller
	 * @param theFailure the type of failure seen
	 * @param theMessage the message to indicate back to the caller
	 * @param theException the exception that indicates the the failure
	 */
	public static void writeSuccess( HttpServletRequest theRequest, HttpServletResponse theResponse, JsonElement theObject ) {
		try {
			Preconditions.checkNotNull( theResponse, "Need a response object." );
			Preconditions.checkNotNull( theObject, "Need an object to write back." );

			_writeSuccess( theRequest, theResponse, Status.OPERATION_COMPLETED, theObject );
			
		} catch( Exception e ) {
			// if we cannot write back, then we have to log
			// and we need to build up some form of alert and send as well
			logger.warn( "An error occurred while attempting to send a success to the caller", e ); 
		}
	}
	
	/**
	 * Shared helper method to write a success response to the caller.
	 * @param theResponse The response object used to write back to the caller
	 */
	private static void _writeSuccess( HttpServletRequest theRequest, HttpServletResponse theResponse, Status theStatusCode, JsonElement theObject ) {
		try {
			theResponse.setStatus( HttpStatus.convert( theStatusCode ).getCode( ) );
			setCommonHeaders( theResponse );
			
			JsonObject bodyObject = new JsonObject( );
			bodyObject.add( "return", theObject );			

			// now save out the operation and write the response
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			Gson targetGson = operationContext.getResponseTarget() == Readability.HUMAN ? humanGson: machineGson;

			addOperation( theRequest, operationContext, bodyObject );
			theResponse.getWriter().write( targetGson.toJson( bodyObject ) );
			
		} catch( Exception e ) {
			// if we cannot write back, then we have to log
			// and we need to build up some form of alert and send as well
			logger.warn( "An error occurred while attempting to send a success to the caller", e ); 
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

		if( theResult.failed() ) {
			_writeFailure(
					theRequest, 
					theResponse, 
					theResult.getStatusCode( ), 
					theResult.getFailureSubcode(), 
					theResult.getFailureMessage(), 
					theResult.getFailureException() );
		} else {
			_writeSuccess( 
					theRequest,
					theResponse,
					theResult.getStatusCode( ), 
					theResult.getValue() );
		}
	}
	
	/**
	 * Simple helper method to add operation information to return to the caller.
	 * @param theContext the operation context of request 
	 * @param theContainer the container json object to write the operation information into
	 */
	private static void addOperation( HttpServletRequest theRequest, OperationContext theContext, JsonObject theContainer ) {
		JsonObject operationObject = new JsonObject( );

		operationObject.addProperty( "request_id", theContext.getCurrentRequestId( ) );
		if( theContext.getResponseDetails() == Details.ALL ) {
			operationObject.addProperty( "root_request_id", theContext.getRootRequestId( ) );
			operationObject.addProperty( "parent_request_id", theContext.getParentRequestId( ) );
			// TODO: consider what to do with these
//			operationObject.addProperty( "caller_url", theContext.getCallingUrl( ) );
//			operationObject.addProperty( "caller_user_agent", theContext.getCallingUserAgent( ) );
			operationObject.addProperty( "response_details", theContext.getResponseDetails( ).toString( ) );
			operationObject.addProperty( "response_target", theContext.getResponseTarget( ).toString( ) );
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
