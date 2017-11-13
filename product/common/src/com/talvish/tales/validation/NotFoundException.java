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
package com.talvish.tales.validation;

/**
 * A {@code RuntimeException} thrown when there is something missing that was excepted to exist. 
 * @author jmolnar
 *
 */
public class NotFoundException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9112814286184594345L;
	
	private final String identifier;
	private final String code;

	/**
	 * Empty constructor.
	 */
	public NotFoundException() {
		code = null;
		this.identifier = null;
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 */
	public NotFoundException(String identifier, String message) {
		super(message);
		code = null;
		this.identifier = identifier;
	}

	/**
	 * Constructor taking the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 */
	public NotFoundException(String identifier, Throwable cause) {
		super(cause);
		code = null;
		this.identifier = identifier;
	}

	/**
	 * Constructor taking a message and the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 * @param message the message for the exception
	 */
	public NotFoundException(String identifier, String message, Throwable cause) {
		super(message, cause);
		code = null;
		this.identifier = identifier;
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 * @param code a specific code the thrower decided to add
	 */
	public NotFoundException(String identifier, String message, String code ) {
		super(message);
		this.code = code;
		this.identifier = identifier;
	}

	/**
	 * Constructor taking a message and the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 * @param code a specific code the thrower decided to add
	 * @param message the message for the exception
	 */
	public NotFoundException(String identifier, String message, String code, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.identifier = identifier;
	}
	
	/**
	 * Returns an extra code parameter provided when the exception was created.
	 * @return a code from the thrower
	 */
	public String getCode( ) {
		return code;
	}
	
	/**
	 * Returns the identifier of the item that could not be found.
	 * @return the identifier 
	 */
	public String getIdentifier( ) {
		return identifier;
	}
}
