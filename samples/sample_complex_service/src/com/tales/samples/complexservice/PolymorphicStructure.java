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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * This is a data structure that contains more than just
 * primitive types, but also other structures.
 * @author Joseph Molnar
 *
 */
@DataContract( name="com.tales.data.polymorphic_structure" )
public class PolymorphicStructure {
	@DataMember( name = "polymorphic_member1", valueTypes = { ComplexStructure.class, SimpleStructure.class } ) Object polymorphicMember1;
	@DataMember( name = "polymorphic_member2", valueTypes = { ComplexStructure.class, SimpleStructure.class } ) Object polymorphicMember2;
	@DataMember( name = "polymorphic_member3", valueTypes = { ComplexStructure.class, SimpleStructure.class } ) Collection<Object> polymorphicMember3;
	@DataMember( name = "polymorphic_member4", valueTypes = { ComplexStructure.class, SimpleStructure.class } ) Object[] polymorphicMember4;
	@DataMember( name = "polymorphic_member5", keyTypes = { Integer.class, String.class }, valueTypes = { ComplexStructure.class, SimpleStructure.class } ) Map<Object, Object> polymorphicMember5;
	
	@SuppressWarnings("unused")
	private PolymorphicStructure( ) {		
	}
	
	public PolymorphicStructure( Object theMember1, Object theMember2 ) {
		polymorphicMember1 = theMember1;
		polymorphicMember2 = theMember2;
		
		polymorphicMember3 = new ArrayList<>( );
		polymorphicMember3.add( theMember1 );
		polymorphicMember3.add( theMember2 );
		
		polymorphicMember4 = new Object[ 2 ];
		polymorphicMember4[ 0 ] = theMember2;
		polymorphicMember4[ 1 ] = theMember1;
		
		polymorphicMember5 = new HashMap<Object,Object>( );
		polymorphicMember5.put( 1, theMember1 );
		polymorphicMember5.put( "item2", theMember2 );
	}
}
