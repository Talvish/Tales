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
package com.tales.samples.complexservice;

import com.google.common.base.Strings;
import com.tales.communication.Status;
import com.tales.contracts.services.http.RequestParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;
import com.tales.contracts.services.http.ResourceResult;
import com.tales.services.DependencyException;
import com.tales.services.DependencyException.Problem;
import com.tales.system.Conditions;

/***
 * This contract shows customizing the return response.
 * @author Joseph Molnar
 *
 */
@ResourceContract( name="com.tales.response_contract", versions={ "20140124" } )
public class ResponseResource {
	/**
	 * A custom success, in this case HTTP status code 201. The Status enum values map to HTTP status codes.
	 */
	@ResourceOperation( name="return_custom_success", path="GET : return_custom_success" )
	public ResourceResult<String> returnCustomSuccess( @RequestParam( name="value" )String theValue ) {
		ResourceResult<String> result = new ResourceResult<String>( );
	
		result.setResult( theValue, Status.OPERATION_CREATED );
		return result;
	}
	
	/**
	 * Sets headers in the result and the HTTP status code is a standard 200.
	 */
	@ResourceOperation( name="set_headers", path="GET : set_headers" )
	public ResourceResult<Void> setHeaders( ) {
		ResourceResult<Void> result = new ResourceResult<Void>( );
	
		result.addHeader( "Custom-Header", "custom value" );
		result.addHeader( "Accept-Language", "en-ca" );
		result.setResult( Status.OPERATION_COMPLETED ); 
		return result;
	}
	
	/**
	 * A custom error response. The Status enum values map to HTTP status codes.
	 */
	@ResourceOperation( name="return_custom_failure", path="GET : return_custom_failure" )
	public ResourceResult<String> returnCustomFailure( ) {
		ResourceResult<String> result = new ResourceResult<String>( );
	
		result.setResult( Status.CALLER_UNAUTHORIZED, "custom_error", "This call is unauthorized.", null);
		return result;
	}

	/**
	 * Throws an InvalidParameterException which will result in a 400-level HTTP status code.
	 * This also demonstrates the Conditions helper methods.
	 */
	@ResourceOperation( name="throw_parameter_exception", path="GET : throw_parameter_exception" )
	public void throwParameterException( @RequestParam( name="value" )String theValue ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theValue ), "value", "The 'value' parameter must not be null or empty." );
		Conditions.checkParameter( Strings.isNullOrEmpty( theValue ), "value", "The 'value' parameter must be null." );
	}
	
	/**
	 * Throws an InvalidStateException which will result in a 400-level HTTP status code.
	 * This also demonstrates the Conditions helper methods.
	 */
	@ResourceOperation( name="throw_state_exception", path="GET : throw_state_exception" )
	public void throwStateException( ) {
		Conditions.checkState( false, "not in a good state" );
	}

	/**
	 * Throws an DependencyException which will result in a 500-level HTTP status code 
	 * with a subcode indicating the type of problem based on the Problem enum.
	 */
	@ResourceOperation( name="throw_dependency_exception", path="GET : throw_dependency_exception" )
	public void throwDependencyException( ) {
		throw new DependencyException( Problem.UNAVAILABLE );
	}	
}

