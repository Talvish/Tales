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
package com.talvish.tales.communication;


/**
 * An exception that can be thrown by the servlets or resources that indicate there is a problem
 * with a dependency.
 * @author jmolnar
 *
 */
public class DependencyException extends RuntimeException {
	public enum Problem {
		/**
		 * A remote service the local code is dependent upon indicated bad date was sent.
		 */
		BAD_DATA,
		/**
		 * A remote service the local code is dependent upon did not respond in time to a request.
		 */
		TIMEOUT,
		/**
		 * A remote service the local code is dependent upon could be connected to, but failed communicating.
		 */
		CANNOT_COMMUNICATE,
		/**
		 * A remote service the local code is dependent upon could not be connected to.
		 */
		CANNOT_CONNECT,
		/**
		 * A remote service the local code is dependent upon indicated it is to busy to process the request or is in maintenance.
		 */
		UNAVAILABLE,
		/**
		 * A remote service the local code is dependent upon indicated an unknown error occurred.
		 */
		UNKNOWN_ERROR;
		
		/**
		 * This method converts a problem to a failure.
		 */
		public static Status convert( Problem theProblem ) {
			switch( theProblem ) {
			case BAD_DATA:
				return Status.DEPENDENCY_BAD_DATA;
			case TIMEOUT:
				return Status.DEPENDENCY_TIMEOUT;
			case CANNOT_COMMUNICATE:
				return Status.DEPENDENCY_CANNOT_COMMUNICATE;
			case CANNOT_CONNECT:
				return Status.DEPENDENCY_CANNOT_CONNECT;
			case UNAVAILABLE:
				return Status.DEPENDENCY_UNAVAILABLE;
			default:
				return Status.DEPENDENCY_ERROR;
			
			}
		}

		/**
		 * This method takes a status code and converts it into an estimated problem value.
		 * @param theState the state to convert
		 * @return the problem it converts to
		 */
		public static Problem convert( Status theState ) {
			switch( theState ) {
			case CALLER_BAD_INPUT:
			case CALLER_BAD_STATE:
			case DEPENDENCY_BAD_DATA:
				return BAD_DATA;
			
			case CALLER_FORBIDDEN:
			case CALLER_NOT_FOUND:
			case CALLER_BAD_VERSION:
			case CALLER_UNAUTHORIZED:
			case DEPENDENCY_CANNOT_COMMUNICATE:
			case LOCAL_NOT_IMPLEMENTED:
				return CANNOT_COMMUNICATE;

			case DEPENDENCY_CANNOT_CONNECT:
				return CANNOT_CONNECT;

			case DEPENDENCY_TIMEOUT:
			case CALLER_TIMEOUT:
			case LOCAL_TIMEOUT:
				return TIMEOUT;
				
			case DEPENDENCY_UNAVAILABLE: 
			case LOCAL_UNAVAILABLE:
				return UNAVAILABLE;


			case DEPENDENCY_ERROR:
			case LOCAL_ERROR: 
			case OPERATION_ASYNC:
			case OPERATION_COMPLETED:
			case OPERATION_CREATED:
			case OPERATION_NOT_MODIFIED:
			case OPERATION_RETRY:
			default:
				return UNKNOWN_ERROR;
			}
		}
	}
	
	private final Problem problem;
	/**
	 * serialization id
	 */
	private static final long serialVersionUID = -1942149355273534557L;

	/**
	 * Default constructor
	 */
	public DependencyException() {
		problem = Problem.UNKNOWN_ERROR;
	}

	/**
	 * Basic constructor.
	 * @param theProblem the type of failure
	 */
	public DependencyException(Problem theProblem ) {
		problem = theProblem;
	}

	/**
	 * Constructor taking a message describing the error.
	 * @param theProblem the type of failure 
	 * @param message the message describing the error
	 */
	public DependencyException( Problem theProblem, String message) {
		super(message);
		problem = theProblem;
	}

	/**
	 * Constructing taking the exception that caused the error.
	 * @param theProblem the type of failure 
	 * @param cause the exception that caused the error
	 */
	public DependencyException( Problem theProblem, Throwable cause) {
		super(cause);
		problem = theProblem;
	}

	/**
	 * Constructing taking the exception that caused the error and a string description.
	 * @param theProblem the type of failure 
	 * @param message the message describing the error
	 * @param cause the exception that caused the error
	 */
	public DependencyException( Problem theProblem, String message, Throwable cause) {
		super(message, cause);
		problem = theProblem;
	}
	
	/**
	 * Indicates the type of dependency failure that occurred.
	 * @return
	 */
	public Problem getProblem( ) {
		return problem;
	}
}
