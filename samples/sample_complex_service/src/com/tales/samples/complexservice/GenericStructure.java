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

import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * This is a data structure that contains more than just
 * primitive types, but also other structures.
 * @author Joseph Molnar
 *
 */
@DataContract( name="com.tales.data.generic_structure" )
public class GenericStructure<T> {
	@DataMember( name = "generic_member" ) T genericMember1;
	@DataMember( name = "member2" ) int member2;
	
	@SuppressWarnings("unused")
	private GenericStructure( ) {		
	}
	
	public GenericStructure( T theMember1, int theMember2 ) {
		genericMember1 = theMember1;
		member2 = theMember2;
	}
}
