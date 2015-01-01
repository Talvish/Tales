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
import com.talvish.tales.system.Conditions;

public class Block {
	private final String name;
	private final String description;
	
	private final Profile declaredProfile;

	private final Map<String,Setting> settings = new HashMap<>( );
	private final Map<String,Block> aggregates = new HashMap<>( ); 

	public Block(
			String theName, 
			String theDescription, 
			Profile theDeclaredProfile ) {
		// the code below does a lot of checking if previous block is null or not, this is because the validation
		// code is done by member at a time to ease understanding, so it takes an extremely minor performance hit

		// TODO: consider name validation
		// make sure we have a name since it cannot be overridden 
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( theName ), "The block needs a name." );
		name = theName;

		// now we check we have profiles and look for a previous block which we may be overriding
		Conditions.checkConfiguration( theDeclaredProfile != null, "Block '%s' did not receive the declared profile.", theName );
		declaredProfile = theDeclaredProfile;
		
		Block previousBlock = declaredProfile.getBlock( theName );
		if( previousBlock != null ) {
			Conditions.checkConfiguration( !previousBlock.getDeclaredProfile().getName().equals( declaredProfile.getName( ) ), "Block '%s' from profile '%s' already has the setting declared in the same profile.", name, declaredProfile.getName()  );
		}

		// if we don't have a description and do have a previous block, then we grab from the previous block
		// there are no particular checks here
		if( Strings.isNullOrEmpty( theDescription ) && previousBlock != null ) {
			description = previousBlock.getDescription( );
		} else {
			description = theDescription;
		}
		
		// for override if a previous block exists then this block must indicate it is an override 
		// and if previous block doesn't exist then this block must indicate it is not an override
//		if( previousBlock != null ) {
//			Conditions.checkConfiguration( isOverride != null && isOverride.booleanValue(), "Block '%s' from profile '%s' does not indicate it is an override but the block in '%s'.", name, declaredProfile.getName(), previousBlock.getDeclaredProfile().getName() );
//			override = true;
//		} else {
//			Conditions.checkConfiguration( isOverride == null || !isOverride.booleanValue(), "Block '%s' from profile '%s' indicates it is an override but is not overriding anything.", name, declaredProfile.getName() );
//			override = false;			
//		}
	}
	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	public Profile getDeclaredProfile( ) {
		return declaredProfile;
	}

	
	
	
	public boolean hasSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty setting name to retrieve from block '%s'.", this.name );
		return settings.containsKey( theName );
	}
	
	public Setting getSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty setting name to retrieve from block '%s'.", this.name );
		return settings.get( theName );
	}
	
	public void addSetting( Setting theSetting ) {
		/// TODO: for now we dont' do the checking if replacing here is good or not since we assume it was done at load time
		Preconditions.checkNotNull( theSetting, "Sent an empty setting to set in block '%s'.", this.name );
		settings.put( theSetting.getName( ), theSetting );
	}
}
