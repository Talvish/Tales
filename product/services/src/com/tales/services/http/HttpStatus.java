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

import com.tales.system.Status;

/**
 * Enum representing the supported status codes. 
 * @author jmolnar
 *
 */
public enum HttpStatus {
	/**
	 * Service is responding that things are okay.
	 */
	OK( 200 ),	

	/**
	 * The request was received and the item was created.
	 */
	OK_CREATED( 201 ),

	/**
	 * The request was received but is running asynchronously and may success or fail later.
	 */
	OK_ACCEPTED( 202 ),
	
	/**
	 * The request was received but the conditional item requested was not modified.
	 */
	REDIRECT_NOT_MODIFIED( 304 ),
	
	/**
	 * Client of the service sent data we had an issue with. 
	 */
	CLIENT_BAD_DATA( 400 ),
	/**
	 * Client of the service is not authorized to make the request, 
	 */
	CLIENT_UNAUTHORIZED( 401 ),
	/**
	 * Client of the service requested something that could not be found.
	 */
	CLIENT_NOT_FOUND( 404 ),
	/**
	 * Client of the service timed out trying to give data to the service.
	 */
	CLIENT_TIMEOUT( 408 ),
	
	/**
	 * Service had an internal problem trying to run the request.
	 */
	SERVICE_ERROR( 500 ),
	/**
	 * Service does not recognize or cannot perform the request.
	 */
	SERVICE_NOT_IMPLEMENTED( 501 ),
	/**
	 * Service is too busy handling the request or is in maintenance.
	 */
	SERVICE_UNAVAILABLE( 503 ),
	
	/**
	 * A service this service is using had an error.
	 */
	GATEWAY_ERROR( 502 ),
	/**
	 * A service this service is using had a timeout.
	 */
	GATEWAY_TIMEOUT( 504 );
	
	private final int code;
	
	/**
	 * Enum constructor.
	 */
	private HttpStatus( int theCode ) {
		code = theCode;
	}
	
	/**
	 * The status code for the enum value.
	 * @return the status code
	 */
	public int getCode( ) {
		return code;
	}
	
	/**
	 * Indicates if the status code is an error.
	 * @return true if the status is an error, false otherwise
	 */
	public boolean isError( ) {
		return isError( this.code );
	}

	/**
	 * Indicates if the status code is a client error.
	 * @return true if the status is a client error, false otherwise
	 */
	public boolean isClientError( ) {
		return isClientError( this.code );
	}
	
	/**
	 * Indicates if the status code is a local service error.
	 * @return true if the status is a local service error, false otherwise
	 */
	public boolean isLocalError( ) {
		return isLocalError( this.code );
	}
	
	/**
	 * Indicates if the status code is an unavailable error.
	 * @return true if the status is an unavailable error, false otherwise
	 */
	public boolean isUnavailableError( ) {
		return isUnavailableError( this.code );
	}
	
	/**
	 * Indicates if the status code is a dependent service error.
	 * @return true if the status is a dependent service error, false otherwise
	 */
	public boolean isDependentError( ) {
		return isDependentError( this.code );
	}
	/**
	 * This method takes status from execution and turns it into an 
	 * HTTP Status code.
	 * @param theStatus the execution status
	 * @return the resulting HTTP status code
	 */
	public static HttpStatus convert( Status theStatus ) {
		switch( theStatus ) {
		case OPERATION_COMPLETED:
			return HttpStatus.OK;
		case OPERATION_CREATED:
			return HttpStatus.OK_CREATED;
		case OPERATION_ASYNC:
			return HttpStatus.OK_ACCEPTED;
		case OPERATION_RETRY:
			return HttpStatus.OK_ACCEPTED;
			
		case OPERATION_NOT_MODIFIED:
			return HttpStatus.REDIRECT_NOT_MODIFIED;
		
		case CALLER_BAD_INPUT:
			return HttpStatus.CLIENT_BAD_DATA;
		case CALLER_BAD_STATE:
			return HttpStatus.CLIENT_BAD_DATA;
		case CALLER_BAD_VERSION:
			return HttpStatus.CLIENT_BAD_DATA;
		case CALLER_UNAUTHORIZED:
			return HttpStatus.CLIENT_UNAUTHORIZED;
		case CALLER_NOT_FOUND:
			return HttpStatus.CLIENT_NOT_FOUND;
		case CALLER_TIMEOUT:
			return HttpStatus.CLIENT_TIMEOUT;
			
		case DEPENDENCY_BAD_DATA:
			return HttpStatus.SERVICE_ERROR;
		case DEPENDENCY_CANNOT_COMMUNICATE:
			return HttpStatus.GATEWAY_ERROR;
		case DEPENDENCY_CANNOT_CONNECT:
			return HttpStatus.GATEWAY_ERROR;
		case DEPENDENCY_ERROR:
			return HttpStatus.GATEWAY_ERROR;
		case DEPENDENCY_TIMEOUT:
			return HttpStatus.GATEWAY_TIMEOUT;
		case DEPENDENCY_UNAVAILABLE:
			return SERVICE_UNAVAILABLE;
			
		case LOCAL_ERROR:
			return HttpStatus.SERVICE_ERROR;
		case LOCAL_NOT_IMPLEMENTED:
			return HttpStatus.SERVICE_NOT_IMPLEMENTED;
		case LOCAL_TIMEOUT:
			return HttpStatus.SERVICE_ERROR;
		case LOCAL_UNAVAILABLE:
			return HttpStatus.SERVICE_UNAVAILABLE;
		
		default:
			return HttpStatus.SERVICE_ERROR;
		}
	}
	
	/**
	 * Indicates if the status code is an error.
	 * @param theStatus the numeric status to check
	 * @return true if the status is an error, false otherwise
	 */
	public static boolean isError( int theStatus ) {
		return !( theStatus >= 200 && theStatus <= 399 );
	}

	/**
	 * Indicates if the status code is a client error.
	 * @param theStatus the numeric status to check
	 * @return true if the status is a client error, false otherwise
	 */
	public static boolean isClientError( int theStatus ) {
		return theStatus >= 400 && theStatus <= 499;
	}
	
	/**
	 * Indicates if the status code is a local service error.
	 * @param theStatus the numeric status to check
	 * @return true if the status is a local service error, false otherwise
	 */
	public static boolean isLocalError( int theStatus ) {
		return theStatus == 500 || theStatus == 501;
	}

	/**
	 * Indicates if the status code is an unavailable error.
	 * @param theStatus the numeric status to check
	 * @return true if the status is an unavailable error, false otherwise
	 */
	public static boolean isUnavailableError( int theStatus ) {
		return theStatus == 503;
	}

	/**
	 * Indicates if the status code is a dependent service error.
	 * @param theStatus the numeric status to check
	 * @return true if the status is a dependent service error, false otherwise
	 */
	public static boolean isDependentError( int theStatus ) {
		return theStatus == 502 || theStatus == 504;
	}
}