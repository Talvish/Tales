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
package com.tales.communication;


/**
 * Enum representing a set of standard success/failure codes. It represents 
 * the spectrum of responses seen by a standard service and its chain:
 * client[caller]->service[local]->dependent service[dependency]
 * @author jmolnar
 *
 */
public enum Status {
	// TODO: consider a generic grouping bucket for this that allows implementers to provide their own mechanisms for translation into HTTP status, etc, they just have to be tagged with the type
	
	/**
	 * Unknown state.
	 */
	UNKNOWN,
	
	/**
	 * The operation was successfully completed.
	 */
	OPERATION_COMPLETED,
	/**
	 * The operation was completed and an item created.
	 */
	OPERATION_CREATED,
	/**
	 * The operation was received and will process async.
	 */
	OPERATION_ASYNC,
	/**
	 * The operation was received and a retry is recommended.
	 */
	OPERATION_RETRY,
	
	
	/**
	 * The operation was received, but the item asked for wasn't modified.
	 */
	OPERATION_NOT_MODIFIED,
	
	
	/**
	 * The caller sent bad data.
	 */
	CALLER_BAD_INPUT,
	/**
	 * The caller sent data that on the surface seems good
	 * but which ran into some data issue with the data.
	 */
	CALLER_BAD_STATE,
	/**
	 * The caller sent an invalid/missing contract version.
	 */
	CALLER_BAD_VERSION,
	/**
	 * The caller did not finish sending data in time.
	 */
	CALLER_UNAUTHORIZED,
	/**
	 * The caller is making a request for something that 
	 * could not be found.
	 */
	CALLER_NOT_FOUND,
	/**
	 * The caller did not finish sending data in time.
	 */
	CALLER_TIMEOUT,
	
	/**
	 * The local code did not implement the request made.
	 */
	LOCAL_NOT_IMPLEMENTED,
	/**
	 * The local code is unable to handle the request.
	 * It is to busy or is in maintenance.
	 */
	LOCAL_UNAVAILABLE,
	/**
	 * The local code ran into an unknown error.
	 */
	LOCAL_ERROR,
	/**
	 * The local code timed out try to execute something.
	 */
	LOCAL_TIMEOUT,
	
	/**
	 * A remote service the local code is dependent upon indicate bad date was sent.
	 */
	DEPENDENCY_BAD_DATA,
	/**
	 * A remote service the local code is dependent upon did not respond in time to a request.
	 */
	DEPENDENCY_TIMEOUT,
	/**
	 * A remote service the local code is dependent upon could be connected to, but failed communicating.
	 */
	DEPENDENCY_CANNOT_COMMUNICATE,
	/**
	 * A remote service the local code is dependent upon could not be connected to.
	 */
	DEPENDENCY_CANNOT_CONNECT,
	/**
	 * A remote service the local code is dependent upon indicated it is to busy to process the request or is in maintenance.
	 */
	DEPENDENCY_UNAVAILABLE,
	/**
	 * A remote service the local code is dependent upon indicated an unknown error occurred.
	 */
	DEPENDENCY_ERROR;
	
	/**
	 * Indicates if the operation was effectively a success.
	 */
	public boolean isSuccess( ) {
		switch( this ) {
		case OPERATION_COMPLETED:
		case OPERATION_CREATED:
		case OPERATION_ASYNC:
		case OPERATION_RETRY:
			
		case OPERATION_NOT_MODIFIED:
			return true;
			
		default: 
			return false;
		}
	}

	/**
	 * Indicates if the operation was a failure.
	 * @return
	 */
	public boolean isFailure( ) {
		return !isSuccess( );
	}
}