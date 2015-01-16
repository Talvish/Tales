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
package com.talvish.tales.auth.capabilities;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;

/**
 * A definition for a particular capability that is part of a group of capabilities.
 * The name given to the capability is expected to be unique amongst the capabilities
 * within the group it is part, but not necessarily between groups.
 * @author jmolnar
 *
 */
@DataContract( name="com.talvish.tales.auth.capability_definition" )
public class CapabilityDefinition {
	public static final String NAME_VALIDATOR = "tales.auth.capability_name";
	
	static {
		if( !NameManager.hasValidator( NAME_VALIDATOR ) ) {
			NameManager.setValidator( NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}
	
	@DataMember( name="name" )
	private String name;
	@DataMember( name="index" )
	private int index;
	@DataMember( name="family" )
	private String family; 
	
	/**
	 * The definition of the capability.
	 * @param theName the name, which should be unique within the group
	 * @param theDescription the description of the capability
	 * @param theIndex the bit index within the group 
	 * @param theFamily the name of the group this capability is part of
	 */
	public CapabilityDefinition( String theName, int theIndex, String theFamily ) {
		NameValidator nameValidator = NameManager.getValidator( NAME_VALIDATOR );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkArgument( theIndex >= 0, "capability '%s' is trying to use invalid index '%s'", theName, theIndex );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "capability '%s' needs a family", theFamily );
		
		family = theFamily;
		name = theName;
		index = theIndex;
	}
	
	/**
	 * The name of the capability.
	 * @return the name of the capability
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The index of the capability within a group. It represents
	 * the bit offset in the set of bits that represent the group.
	 * @return
	 */
	public int getIndex( ) {
		return index;
	}

	/**
	 * The name of the group the capability is part of.
	 * @return
	 */
	public String getFamily( ) {
		return family;
	}
}