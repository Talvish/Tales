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
package com.tales.services;

/**
 * A {@code RuntimeException} thrown when there is an invalid parameter going into a method, operations, etc. 
 * @author jmolnar
 *
 */
public class InvalidParameterException extends RuntimeException {
	private final String name;
	private final String code;

	/**
	 * 
	 */
	private static final long serialVersionUID = -4874090416452846180L;

	/**
	 * Empty constructor.
	 */
	public InvalidParameterException() {
		this.code = null;
		this.name = null;
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 */
	public InvalidParameterException(String name, String message) {
		super(message);
		this.code = null;
		this.name = name;
	}
	
	/**
	 * Constructor taking the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 */
	public InvalidParameterException(String name, Throwable cause) {
		super(cause);
		this.code = null;
		this.name = name;
	}


	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 * @param code a specific code the thrower decided to add
	 */
	public InvalidParameterException( String name, String message, String code ) {
		super(message);
		this.code = code;
		this.name = name;
	}

	/**
	 * Constructor taking a message and the exception that likely describes the failure.
	 * @param cause the exception that describes the failure
	 * @param code a specific code the thrower decided to add
	 * @param message the message for the exception
	 */
	public InvalidParameterException( String name, String message, String code, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.name = name;
	}
	
	/***
	 * Returns the name of the parameter.
	 * @return the name of the parameter
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * Returns an extra code parameter provided when the exception was created.
	 * @return a code from the thrower
	 */
	public String getCode( ) {
		return code;
	}
}
