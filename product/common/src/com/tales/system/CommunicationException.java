//***************************************************************************
//*  Copyright 2014 Joseph Molnar
//*
//*  Licensed under the Apache License, Version 2.0 (the "License");
//*  you may not use this file except in compliance with the License.
//*  You may obtain a copy of the License at
//*
//*      http://www.apache.org/licenses/LICENSE-2.0
//*
//*  Unless required by applicable law or agreed to in writing, software
//*  distributed under the License is distributed on an "AS IS" BASIS,
//*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//*  See the License for the specific language governing permissions and
//*  limitations under the License.
//***************************************************************************
package com.tales.system;

/**
* Exception thrown when there is a communication failure to some online service/location.
* @author jmolnar
*
*/
public class CommunicationException extends RuntimeException {

	private static final long serialVersionUID = 1527444399374581360L;

	/**
	 * Default constructor
	 */
	public CommunicationException() {
	}

	/**
	 * Constructor taking a message describing the error. 
	 * @param message the message describing the error
	 */
	public CommunicationException(String message) {
		super(message);
	}

	/**
	 * Constructing taking the exception that caused the error.
	 * @param cause the exception that caused the error
	 */
	public CommunicationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructing taking the exception that caused the error and a string description.
	 * @param message the message describing the error
	 * @param cause the exception that caused the error
	 */
	public CommunicationException(String message, Throwable cause) {
		super(message, cause);
	}

}

