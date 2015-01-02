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

import com.google.common.base.Strings;
import com.talvish.tales.system.Conditions;

/**
 * For settings validation the fact I can insert a replacement block into the mix changes things and 
 * means there are interesting side effects like the insert block CAN have members that are new
 * so that other blocks that use it don't say they are overrides BUT they are overriding something
 * and therefore we have a problem ... meaning you cannot add something that someone else already
 * has.
 * 
 * The same block cannot be declared in the same profile, so these interesting issues are capped.
 * 
 * Also now curious if we have a block issue because of this.
 * 
 * @author josep_000
 *
 */

public class Setting {
	private final String name;
	private final String description;
	private final boolean override;
	private final String value;
	private final String type;
	private final boolean sensitive;
	private final Profile declaredProfile;
	private final Block declaredBlock;

	public Setting( 
			String theName, 
			String theDescription,
			String theValue,
			String theType,
			Boolean isOverride,
			Boolean isSensitive,
			Block theDeclaredBlock,
			Profile theDeclaredProfile ) {
		// the code below does a lot of checking if previous setting is null or not, this is because the validation
		// code is done by member at a time to ease understanding, so it takes an extremely minor performance hit

		// TODO: consider name validation

		// make sure we have a name since it cannot be overridden 
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( theName ), "Attempting to create a setting without a name." );
		name = theName;

		// now we do some profile/block checks, which also requires we grab the previous setting
		Conditions.checkConfiguration( theDeclaredBlock != null, "Setting '%s' is being created without a block.", name );
		Conditions.checkConfiguration( theDeclaredProfile != null, "Setting '%s' is being created without a profile.", name );
		declaredBlock = theDeclaredBlock;
		declaredProfile = theDeclaredProfile;

		Setting previousSetting = null; //declaredProfile.getSetting( theName );

		if( previousSetting != null ) {
			// check if this is an existing setting within the same profile
			Conditions.checkConfiguration( !previousSetting.getDeclaredProfile().getName().equals( declaredProfile.getName( ) ), "Setting '%s' from '%s.%s' already has the setting declared in the same profile.", name, declaredProfile.getName(), declaredBlock.getName( ) );
			// check if this is an existing setting within the same block
			Conditions.checkConfiguration( !previousSetting.getDeclaredBlock().getName().equals( declaredBlock.getName( ) ), "Setting '%s' from '%s.%s' already has the setting declared in the same block.", name, declaredProfile.getName(), declaredBlock.getName( ) );
		}
		
		
		// if we don't have a description and do have a previous setting, then we grab from the previous setting
		// there are no particular checks here
		if( Strings.isNullOrEmpty( theDescription ) && previousSetting != null ) {
			description = previousSetting.getDescription( );
		} else {
			description = theDescription;
		}
		
		// value has no checks nor overrides 
		value = theValue;

		// so now we deal with the type, which we can grab from the previous setting if not set but if set it must be the same as the previous setting
		if( previousSetting != null ) {
			if( Strings.isNullOrEmpty( theType ) ) {
				type = previousSetting.getType( );
			} else {
				// TODO: this comparison is string based, which isn't great (need to create a normalized version and store it)
				Conditions.checkConfiguration( theType.equals( previousSetting.getType( ) ), "Setting '%s' from '%s.%s' is type '%s' but overrides from '%s.%s' which indicates it is type '%s'.", name, declaredProfile.getName(), declaredBlock.getName( ), theType, previousSetting.getDeclaredProfile().getName(), previousSetting.getDeclaredBlock().getName(), previousSetting.getType( ) );
				type = previousSetting.getType( );
			}
		} else {
			type = theType;
		}

		// for override if a previous setting exists then this setting must indicate it is an override 
		// and if previous setting doesn't exist then this setting must indicate it is not an override
		if( previousSetting != null ) {
			Conditions.checkConfiguration( isOverride != null && isOverride.booleanValue(), "Setting '%s' from '%s.%s' does not indicate it is an override but the setting exists from '%s.%s'.", name, declaredProfile.getName(), declaredBlock.getName( ), previousSetting.getDeclaredProfile().getName(), previousSetting.getDeclaredBlock().getName());
			override = true;
		} else {
			Conditions.checkConfiguration( isOverride == null || !isOverride.booleanValue(), "Setting '%s' from '%s.%s' indicates it is an override but is not overriding anything.", name, declaredProfile.getName(), declaredBlock.getName( ) );
			override = false;			
		}

		
		// for sensitive, if the previous setting exists and indicates it is sensitive then we must either not have received a sensitive or it must also be sensitive
		// and if previous setting doesn't exist then we take the value received or set to false if not received
		if( previousSetting != null ) {
			Conditions.checkConfiguration( isSensitive == null || !previousSetting.isSensitive() || isSensitive, "Setting '%s' from '%s.%s' does not indicate the setting is sensitive but the setting it overrides from '%s.%s' indicates it is.", name, declaredProfile.getName(), declaredBlock.getName( ), previousSetting.getDeclaredProfile().getName(), previousSetting.getDeclaredBlock().getName());
			sensitive = isSensitive == null ? previousSetting.isSensitive() : isSensitive;
		} else {
			sensitive = isSensitive == null ? false : isSensitive;			
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
	
	public String getValue( ) {
		return value;
	}
	
	public String getType( ) {
		return type;
	}
	
	public boolean isSensitive( ) {
		return sensitive;
	}
	
	public Block getDeclaredBlock( ) {
		return declaredBlock;
	}
	
	public Profile getDeclaredProfile( ) {
		return declaredProfile;
	}
}
