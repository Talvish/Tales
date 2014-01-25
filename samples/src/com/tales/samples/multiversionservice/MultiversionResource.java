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
package com.tales.samples.multiversionservice;

import com.tales.contracts.services.http.RequestParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;

/**
 * This resource contract shows how different versions can be defined and used. 
 * When specifying an operation, ranges can be used to indicate which versions 
 * of the contract the operation can be used within. This doesn't many the 
 * 'version' can be any value, since the version given must be one of the values
 * the contract states it supports. For example, version '20140101' is a valid 
 * value and will call the older echo operation, but version '20140121' is not
 * valid and will result in an error.
 * @author Joseph Molnar
 *
 */
@ResourceContract( name="com.tales.multiversion_contract", versions={ "20140101", "20140110", "20140120", "20140130" } )
public class MultiversionResource {
	/**
	 * An operation that can be called using HTTP GET or POST that requires a 'value' parameter sent 
	 * in the query string or post body. This version of echo will only be called if the 'version' parameter
	 * is any version up to '20140110'.
	 */
	@ResourceOperation( name="echo", path="GET | POST : echo", versions="-20140110", description="echo operation for all contract versions up through 20140110" ) 
	public String echo20140101( @RequestParam( name="value" )String theValue ) {
		return "echo (older version): " + theValue;
	}

	/**
	 * An operation that can be called using HTTP GET or POST that requires a 'value' parameter sent 
	 * in the query string or post body. This version of echo will only be called if the 'version' parameter
	 * is the value '20140120' or beyond. 
	 */
	@ResourceOperation( name="echo", path="GET | POST : echo", versions="20140120-", description="echo operation for all contract versions 20140120 and beyond" ) 
	public String echo20140130( @RequestParam( name="value" )String theValue ) {
		return "echo (newer version): " + theValue;
	}
}
