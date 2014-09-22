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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * The overall structured response from a Tales-enabled service
 * in addition to the header and cookie information.
 * @author jmolnar
 *
 * @param <T> the type of the result 
 */
@DataContract( name="com.tales.response.result" )
public class ResourceResult<T> {
	private T result;
	private @DataMember( name="status" )ResponseStatus status;
	private @DataMember( name="operation")ResponseOperation operation;
	
	private final Map<String,String> headers = new HashMap<String,String>();
	private final Map<String,String> externalHeaders = Collections.unmodifiableMap( headers );
	
	/**
	 * The protected constructor used for reflection. 
	 */
	protected ResourceResult( ) {
	}
	
	/**
	 * The main result of the request.
	 * @return the main result of the request
	 */
	public T getResult( ) {
		return result;
	}

	// TODO: need to do HTTP header and cookie support

	/**
	 * Returns a read-only version of the headers.
	 * While you can see cases of the same header existing more than once
	 * the Tales services should never have that happen.
	 * @return the headers
	 */
	public Map<String,String> getHeaders( ) {
		return externalHeaders;
	}
	
	/**
	 * Internal method to add readers to return.
	 * Technically someone can be iterating over the
	 * collection while items are being added, but 
	 * in practice it shouldn't happen since items
	 * aren't added except during raw response handling
	 * and no one will be reading at that time.
	 * @param theName the header to set
	 * @param theValue the value to set
	 */
	protected void setHeader( String theName, String theValue ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name to save the header" );
		headers.put( theName,  theValue );
		
	}

	/**
	 * Internal method to the main result.
	 * This is protected since it should only be called by the Tales client source.
	 * @param theResult the main result to set
	 */
	protected void setResult( T theResult ) {
		result = theResult;
	}
	

	/**
	 * Gets the overall status information on the request.
	 * @return the status of the request
	 */
	public ResponseStatus getStatus( ) {
		return status;
	}
	
	/**
	 * Get the operation information on the request.
	 * @return the operation information reguarding the request.
	 */
	public ResponseOperation getOperation( ) {
		return operation;
	}
}
