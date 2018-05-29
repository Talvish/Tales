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
package com.talvish.tales.contracts.services.http;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.talvish.tales.communication.Status;

/**
 * This class represents the result of an execution of a resource method.
 * This is meant for internal framework use only. Users of the framework
 * should use ResourceResult.
 * It covers both successful and unsuccessful cases.
 * @author jmolnar
 *
 */
public class ResourceMethodResult extends HttpResult<JsonElement,ResourceMethodResult> {
	/**
	 * Modified copy constructor taking a different value.
	 * @param theValue the value to use
	 * @param theOriginal the original result to clone
	 */
	ResourceMethodResult( JsonElement theValue, HttpResult<?,?> theOriginal ) {
		Preconditions.checkNotNull( theOriginal, "the original result must not be null" );
		this.value = theValue;
		this.headers = theOriginal.headers;
		this.cookies = theOriginal.cookies;
		this.code = theOriginal.code;
		this.subcode = theOriginal.subcode;
		this.subject = theOriginal.subject;
		this.message = theOriginal.message;
		this.exception = theOriginal.exception;
	}


	ResourceMethodResult( JsonElement theValue, Status theStatus ) {
		Preconditions.checkNotNull( theValue, "need a value" );
		this.value = theValue;
		this.code = theStatus;
		this.subcode = null;
		this.subject = null;
		this.message = null;
		this.exception = null;
	}

	/**
	 * Constructor typically for the common failure case.
	 * @param theFailure the reason for the failure
	 * @param theSubcode a code, handler specific, outlining the problem
	 * @param theMessage the failure message to display
	 * @param theException the exception, which may be null
	 */
	ResourceMethodResult( Status theCode, String theSubcode, String theSubject, String theMessage, Throwable theException ) {
		Preconditions.checkNotNull( theCode, "need a status code" );
		this.value = null;
		this.code = theCode;
		this.subcode = theSubcode;
		this.subject = theSubject;
		this.message = theMessage;
		this.exception = theException;
	}

	/**
	 * Constructor typically for the failure case.
	 * @param theFailure the reason for the failure
	 * @param theSubcode a code, handler specific, outlining the problem
	 * @param theMessage the failure message to display
	 * @param theException the exception, which may be null
	 */
	ResourceMethodResult( JsonElement theValue, Status theCode, String theSubcode, String theSubject, String theMessage, Throwable theException ) {
		Preconditions.checkNotNull( theValue, "need a value" );
		Preconditions.checkNotNull( theCode, "need a status code" );
		this.value = theValue;
		this.code = theCode;
		this.subcode = theSubcode;
		this.subject = theSubject;
		this.message = theMessage;
		this.exception = theException;
	}
}
