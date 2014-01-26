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

import com.tales.contracts.services.http.RequestParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;

/***
 * This is a contract demonstrating both simple structure (contain just primitive types)
 * and complex types (contains other structures).
 * @author Joseph Molnar
 *
 */
@ResourceContract( name="com.tales.data_structure_contract", versions={ "20140124" } )
public class DataStructureResource {
	/**
	 * Returns a structure that contains just primitive types.
	 */
	@ResourceOperation( name="get_simple_structure", path="GET : get_simple_structure" )
	public SimpleStructure getSimpleStructure( ) {
		return new SimpleStructure( "a value", 121231230090l, 3.14f );
	}
	
	/**
	 * Receives a structure that contains just primitive types.
	 */
	@ResourceOperation( name="set_simple_structure", path="GET | POST : set_simple_structure")
	public SimpleStructure setSimpleStructure( @RequestParam( name="value" )SimpleStructure theValue ) {
		return theValue;
	}
	
	/**
	 * Returns a structure that contains just another structure.
	 */
	@ResourceOperation( name="get_complex_structure", path="GET : get_complex_structure" )
	public ComplexStructure getComplexStructure( ) {
		return new ComplexStructure( new SimpleStructure( "a value", 121231230090l, 3.14f ), 10 );
	}
	
	/**
	 * Receives a structure that contains another structure types.
	 */
	@ResourceOperation( name="set_complex_structure", path="GET | POST : set_complex_structure")
	public ComplexStructure setComplexStructure( @RequestParam( name="value" )ComplexStructure theValue ) {
		return theValue;
	}
}

