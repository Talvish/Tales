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

/**
 * The overall structured response from a Tales-enabled service.
 * @author jmolnar
 *
 * @param <T> the type of the result 
 */
@DataContract( name="com.tales.response.result" )
public class ResourceResult<T> {
	private T result;
	private @DataMember( name="status" )ResponseStatus status;
	private @DataMember( name="operation")ResponseOperation operation;
	
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
	
	/**
	 * Sets the main result.
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
