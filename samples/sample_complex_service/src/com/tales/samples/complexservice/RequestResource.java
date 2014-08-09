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

import com.tales.contracts.services.http.HeaderParam;
import com.tales.contracts.services.http.PathParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;

/***
 * A contract showing various options on the request side, including use
 * of path and header parameters, regex path matching and overloading. 
 * @author Joseph Molnar
 *
 */
@ResourceContract( name="com.tales.request_contract", versions={ "20140124" } )
public class RequestResource {
	/**
	 * This method shows a simple hard fixed path.
	 */
	@ResourceOperation( name="overlap", path="GET : overlap/element_one" )
	public String overlapFixedPath1( ) {
		return "overlap with fixed path value 'element_one'";
	}
	
	/**
	 * This method shows a path where a string parameter can be in the path.
	 */
	@ResourceOperation( name="overlap", path="GET : overlap/{value}" )
	public String overlapOneStringPathParameter( @PathParam(name = "value") String theValue ) {
		return "overlap with one string path parameter: " +  theValue;
	}
	
	/**
	 * This method shows a path where a parameter can be in the path and will match based on a regular expression.
	 * Even the the 'overlapOnePathParameter' overlaps, if the value that comes in is a number with 3 to 4 digits
	 * this method will be called. This also shows the type of the parameter being something other than a string. 
	 * In this case, an int.
	 */
	@ResourceOperation( name="overlap", path="GET : overlap/{value:[0-9]{3,4}}" )
	public String overlapRegexPathParameter( @PathParam(name = "value") int theValue ) {
		return "overlap with regex int path path parameter: " + theValue;
	}

	/**
	 * This method shows a path where a string parameter can be in the path and will match based on a regular expression.
	 * Even the the 'overlapOnePathParameter' overlaps this will still be used.
	 */
	@ResourceOperation( name="overlap", path="GET : overlap/element_two" )
	public String overlapFixedPath2( ) {
		return "overlap with fixed path value 'element_two'";
	}
	
	/**
	 * This method shows a path with two values and while it can overlap with the above, having
	 * additional path elements will ensure this gets called.
	 */
	@ResourceOperation( name="overlap", path="GET : overlap/{value_one}/{value_two}" )
	public String overlapTwoStringPathParameter( @PathParam(name = "value_one") String theValueOne, @PathParam(name = "value_two") String theValueTwo  ) {
		return "overlap with two string path parameters: " +  theValueOne + " | " + theValueTwo;
	}
	
	/**
	 * This demonstrates getting a header as a parameter.
	 */
	@ResourceOperation( name="get_header", path="GET : get_header" )
	public String getHeader( @HeaderParam( name="Accept-Language" )String theLanguage ) {
		return theLanguage;
	}
}
