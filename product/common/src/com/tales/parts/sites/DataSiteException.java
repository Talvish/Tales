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
package com.tales.parts.sites;

/**
 * A {@code RuntimeException} thrown when data site gettign/sitting fails. 
 * @author jmolnar
 *
 */
public class DataSiteException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4803865123177460076L;

	/**
	 * Empty constructor.
	 */
	public DataSiteException() {
	}

	/**
	 * Constructor taking a message.
	 * @param message the message for the exception.
	 */
	public DataSiteException(String message) {
		super(message);
	}

	/**
	 * Constructor taking the exception that likely describes the translation failure.
	 * @param cause the exception that describes the translation failure
	 */
	public DataSiteException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor taking a message and the exception that likely describes the translation failure.
	 * @param cause the exception that describes the translation failure
	 * @param message the message for the exception
	 */
	public DataSiteException(String message, Throwable cause) {
		super(message, cause);
	}
}
