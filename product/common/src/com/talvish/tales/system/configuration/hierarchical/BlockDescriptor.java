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
package com.talvish.tales.system.configuration.hierarchical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.system.Conditions;

// TODO: consider an 'abstract' marker on the blocks which means they cannot be used directly, but included

/**
 * This class represents the block structure as found in the configuration. 
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.block_descriptor")
public class BlockDescriptor {
	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	
	@DataMember( name="override" )
	private Boolean override;

	@DataMember( name="includes" )
	private String[] includeArray;

	@DataMember( name="settings" )
	private SettingDescriptor[] declaredSettingArray;

	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private ProfileDescriptor profile;
	private final Map<String,SettingDescriptor> declaredSettingMap = new HashMap<>( );
	private final List<String> includeList = new ArrayList<>( );

	
	/**
	 * The name given to the block. 
	 * This is required.
	 * @return the block name
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The description given to the block.
	 * This may not be set in which case the overridden block may have the value to use (assuming it is set). 
	 * @return the description of the block
	 */
	public String getDescription( ) {
		return description;
	}
	
	/**
	 * Indicates if this block is overriding another block of the same name in a parent profile.
	 * This value may not be set in the config source which means this method will return false.
	 * @return true means overriding, false means not overriding
	 */
	public boolean isOverride( ) {
		return override == null ? false : override.booleanValue();
	}
	
	/**
	 * The names of the blocks to include (aggregate) into this block.
	 * @return the list of includes, or an empty list
	 */
	public Collection<String> getIncludes( ) {
		return includeList;
	}

	/**
	 * Checks to see if a particular setting was declared directly in this block.
	 * @param theName the name to check for
	 * @return true if declared here, false otherwise
	 */
	public boolean hasDeclaredSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a setting from block '%s'.", this.name );
		return declaredSettingMap.containsKey( theName );
	}

	/**
	 * Gets a setting declared directly in this block.
	 * @param theName the name of the setting to get
	 * @return returns the setting if found on this block, null if not found
	 */
	public SettingDescriptor getDeclaredSetting( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a setting from block '%s'.", this.name );
		return declaredSettingMap.get( theName );
	}
	
	/**
	 * Returns the list of all settings directly declared in this block.
	 * @return the collection of declared settings or empty list of none
	 */
	public Collection<SettingDescriptor> getDeclaredSetting( ) {
		return declaredSettingMap.values();
	}
	
	/**
	 * Returns the profile that this block was declared within.
	 * @return the profile this block was declared within.
	 */
	public ProfileDescriptor getProfile( ) {
		return profile;
	}
	
	/**
	 * Helper method called after the source loads. 
	 * This is also how the profile is set on the block. 
	 * It does some simple validation and sets up additional data to help 
	 * with runtime support.
	 * @param theProfile the profile that is the parent for this block
	 */
	protected void onDeserialized( ProfileDescriptor theProfile ) {
		Preconditions.checkArgument( theProfile != null, "Block '%s' is getting the profile set to null.", name );
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( name ), "A block without a name was loaded from profile '%s'.", theProfile.getName( ) );
		Preconditions.checkState( profile == null, "Block '%s.%s' is getting profile reset to '%s'.", profile == null ? "<empty>" : profile.getName( ), name, theProfile.getName() );
		profile = theProfile;
		
		if( includeArray != null ) {
			for( String include : includeArray ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Block '%s.%s' is attempting to include a blank or missing block (check source for trailing commas, etc).", profile.getName(), name );
				Conditions.checkConfiguration( !name.equals( include ), "Block '%s.%s' is attempting to include itself.", profile.getName(), name );
				Conditions.checkConfiguration( !includeList.contains( include ), "Block '%s.%s' is attempting to include block '%s' more than once.", profile.getName(), name, include );
				includeList.add( include );
			}
		}

		if( declaredSettingArray != null ) {
			for( SettingDescriptor setting : declaredSettingArray ) {
				Conditions.checkConfiguration( setting != null, "Block '%s.%s' is attempting to include a missing setting (check source for trailing commas, etc).", profile.getName(), name );
				Conditions.checkConfiguration( !declaredSettingMap.containsKey( setting.getName( ) ), "Block '%s.%s' is attempting to add setting '%s' more than once .", profile.getName(), name, setting.getName( ) );
				declaredSettingMap.put( setting.getName( ), setting );
				setting.onDeserialized( this );
			}
		}
	}
}
