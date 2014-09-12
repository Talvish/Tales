// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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
package com.tales.client.http;

import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;
import com.tales.system.Status;

/**
 * The status information of a request to a Tales-enabled service.
 * This is returned in the response.
 * @author jmolnar
 *
 */
@DataContract( name="com.tales.response.status" )
public class ResponseStatus {
	@DataMember( name="code" )private Status code; // TODO: consider renaming this to something other than code 
	@DataMember( name="subcode" )private String subcode;
	@DataMember( name="message" )private String message;
	@DataMember( name="exception" )private ResponseExceptionDetails exception;
	
	/**
	 * Constructor used by reflection.
	 */
	protected ResponseStatus( )  {		
	}

	/**
	 * The primary status code.
	 * For HTTP requests, these tend to map to HTTP status codes but are a bit more descriptive.
	 * @return The primary status code.
	 */
	public Status getCode( ) {
		return code;
	}
	
	/**
	 * An optional subcode whose values will depending on the request.
	 * @return An optional subcode
	 */
	public String getSubcode( ) {
		return subcode;
	}
	
	/**
	 * An optional message returned with the request.
	 * @return An optional message
	 */
	public String getMessage( ) {
		return message;
	}
	
	/**
	 * The optional exception details. If there is a server problem an exception
	 * may be generated and returned.
	 * @return the exception details
	 */
	public ResponseExceptionDetails getExceptionDetails( ) {
		return exception;
	}
}
