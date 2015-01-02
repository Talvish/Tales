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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Conditions;

// TODO: the thing I need to consider is that when block can be overridden which means
//		 within the context of that profile the block is in, it will replace
//		 all items EVEN if the another block that uses it doesn't get used in this
//		 profile, it will show up ... another option is that for a block to be 
//		 accessible in a profile it must be mentioned??
public class Block {
	private final String name;
	private final String description;
	private final boolean override;
	
	private final Profile declaredProfile;

	private final Map<String,Setting> declaredSettings = new HashMap<>( );
//	private final List<String> includes; // set in the constructor 

	public Block(
			String theName, 
			String theDescription, 
			Boolean isOverride,
//			String[] theIncludes,
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
		
		// now we look to see if we have a block we are overriding
		// this works because the parent profile has been loaded
		// and has been set on declare profile and all blocks
		// it exposed are available, so we can look for it here
		Block previousBlock = declaredProfile.getBlock( theName );

		// if we don't have a description and do have a previous block, then we grab from the previous block
		// there are no particular checks here
		if( Strings.isNullOrEmpty( theDescription ) && previousBlock != null ) {
			description = previousBlock.getDescription( );
		} else {
			description = theDescription;
		}
		
//		if( theIncludes != null ) {
//			includes = Arrays.asList( theIncludes );
//		} else {
//			includes = new ArrayList<String>( 0 );
//		}
		
		// for override if a previous block exists then this block must indicate it is an override 
		// and if previous block doesn't exist then this block must indicate it is not an override
		if( previousBlock != null ) {
			Conditions.checkConfiguration( isOverride != null && isOverride.booleanValue(), "Block '%s' from profile '%s' does not indicate it is an override but the block does previously exist in profile '%s'.", name, declaredProfile.getName(), previousBlock.getDeclaredProfile().getName() );
			override = true;
		} else {
			Conditions.checkConfiguration( isOverride == null || !isOverride.booleanValue(), "Block '%s' from profile '%s' indicates it is an override but the block does not previously exist..", name, declaredProfile.getName() );
			override = false;			
		}
	}
	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	public boolean isOverride( ) {
		return override;
	}
	
	public Profile getDeclaredProfile( ) {
		return declaredProfile;
	}

	
	
	
	public boolean hasDeclaredSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty setting name to retrieve from block '%s'.", this.name );
		return declaredSettings.containsKey( theName );
	}
	
	public Setting getDeclaredSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty setting name to retrieve from block '%s'.", this.name );
		return declaredSettings.get( theName );
	}
	
	public void addDeclaredSetting( Setting theSetting ) {
		Preconditions.checkNotNull( theSetting, "Sent an empty setting to set in block '%s'.", this.name );
		Conditions.checkConfiguration( !declaredSettings.containsKey( theSetting.getName( ) ), "Cannot add setting '%s' to '%s.%s' since it has already been declared.", theSetting.getName( ), this.declaredProfile.getName(), this.name );
		declaredSettings.put( theSetting.getName( ), theSetting );
	}
	
	
	public Setting getSetting( Profile theCallingProfile, String theSettingName ) {
		Preconditions.checkNotNull( theCallingProfile, "Need a calling profile to get a setting from '%s.%s'.", this.declaredProfile.getName( ), this.name );
		Preconditions.checkNotNull( theSettingName, "Need a setting name to get a setting from '%s.%s'.", this.declaredProfile.getName( ), this.name );
		
		Setting setting = null;
		
		if( declaredSettings.containsKey( theSettingName ) ) {
			setting = declaredSettings.get( theSettingName );
		} else {
			// if we don't have the setting declared here, we go back to the calling profile
			// context since we may have overridden blocks to consider, so we do loop
			// through the added blocks here to know which to use, but we use the name
			// of the block to find any over-written blocks
			Block includedBlock;
			
//			for( String includedBlockName : includes ) {
//				includedBlock = theCallingProfile.getBlock( includedBlockName );
//				Preconditions.checkState( includedBlock != null, "Could not find block '%s' while attempting to get setting '%s.%s.%s' using profile '%s'.", includedBlockName, declaredProfile.getName( ), this.name, theSettingName, theCallingProfile.getName( ) );
//				setting = includedBlock.getSetting( theCallingProfile, theSettingName );
//				if( setting != null ) {
//					break;
//				}
//			}
		}
		return setting;
	}
}
