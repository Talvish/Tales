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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * Returns a polymorphic structure.
	 */
	@ResourceOperation( name="get_polymorphic_structure", path="GET : get_polymorphic_structure" )
	public PolymorphicStructure getPolymorphicStructure( ) {
		return new PolymorphicStructure(
				new SimpleStructure( "a value", 121231230090l, 3.14f ),
				new ComplexStructure( new SimpleStructure( "old", 32l, 14.4f ), 16 ) );
	}
	
	/**
	 * Receives a polymorphc structure.
	 */
	@ResourceOperation( name="set_polymorphic_structure", path="GET | POST : set_polymorphic_structure")
	public PolymorphicStructure setPolymorphicStructure( @RequestParam( name="value" )PolymorphicStructure theValue ) {
		return theValue;
	}

//	/**
//	 * Returns a polymorphic structure.
//	 */
//	@ResourceOperation( name="get_generic_structure", path="GET : get_generic_structure" )
//	public GenericStructure<String> getGenericStructure( ) {
//		return new GenericStructure<String>(
//				"string",
//				2 );
//	}
	
	/**
	 * Returns a list of strings. Demonstrates both Java generics and list responses.
	 */
	@ResourceOperation( name="get_list", path="GET : get_list" )
	public List<String> getList( ) {
		List<String> list = new ArrayList<String>( );
		
		list.add( "entry one" );
		list.add( "entry two" );
	
		return list;
	}

	/**
	 * Receives a list of strings. Demonstrates both Java generics and list responses.
	 */
	@ResourceOperation( name="set_list", path="GET | POST : set_list" )
	public List<String> getList( @RequestParam( name="list" )List<String> theList) {
		return theList;
	}
	
	/**
	 * Returns a map keyed by strings and holding a structure.
	 */
	@ResourceOperation( name="get_map", path="GET : get_map" )
	public Map<String,SimpleStructure> getMap( ) {
		Map<String,SimpleStructure> map = new HashMap<String,SimpleStructure>( );
		
		map.put( "key_one", new SimpleStructure( "string one", 1l, 1.0f) );
		map.put( "key_two", new SimpleStructure( "string two", 2l, 2.0f) );
	
		return map;
	}

	/**
	 * Receives a map keyed by strings and holding a structure.
	 */
	@ResourceOperation( name="set_map", path="GET | POST : set_map" )
	public Map<String,SimpleStructure> setMap( @RequestParam( name="map" )Map<String,SimpleStructure> theMap) {
		return theMap;
	}
}

