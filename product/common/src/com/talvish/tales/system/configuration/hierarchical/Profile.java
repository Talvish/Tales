// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.system.configuration.hierarchical;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Profile {
	private final String name;
	private final String description;

	private Map<String,Block> blocks = new HashMap<>( );
	private Map<String,Setting> settings = new HashMap<>( );
	
	public Profile( String theName, String theDescription ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "The profile needs a name" );
		// TODO: consider name validation
		
		name = theName;
		description = theDescription;
	}
	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	public boolean hasBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return blocks.containsKey( theName );
	}
	
	public Block getBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return blocks.get( theName );
	}
	
	public void addBlock( Block theBlock ) {
		Preconditions.checkNotNull( theBlock, "Sent an empty block to add to profile '%s'.", this.name );
		blocks.put( theBlock.getName( ), theBlock );
		// TODO: technically we could do some validation here like making sure that override is set, that pe-existing 
	}

	// TODO: need to figure out how to handle the inherited profiles
}
