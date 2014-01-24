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
package com.tales.parts;

/**
 * Exception thrown when validation fails on an object.
 * @author jmolnar
 *
 */
public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 5577132805125099284L;

	/**
	 * Default constructor
	 */
	public ValidationException() {
	}

	/**
	 * Constructor taking a message describing the error. 
	 * @param message the message describing the error
	 */
	public ValidationException(String message) {
		super(message);
	}

	/**
	 * Constructing taking the exception that caused the error.
	 * @param cause the exception that caused the error
	 */
	public ValidationException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Constructing taking the exception that caused the error and a string description.
	 * @param message the message describing the error
	 * @param cause the exception that caused the error
	 */
	public ValidationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
