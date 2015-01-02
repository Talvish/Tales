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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.system.Conditions;
 
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
	private SettingDescriptor[] declaredSettingDescriptorArray;

	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private ProfileDescriptor profileDescriptor;
	private final Map<String,SettingDescriptor> declaredSettingDescriptorMap = new HashMap<>( );
	private final List<String> includeList = new ArrayList<>( );

	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	public Boolean isOverride( ) {
		return override;
	}
	
	public Collection<String> getIncludes( ) {
		return includeList;
	}
	
	// TODO: must be consistent on these names (declared/descriptor) across profile, block, setting
	
	public boolean hasDeclaredSettingDescriptor( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a setting descriptor from block descriptor '%s'.", this.name );
		return declaredSettingDescriptorMap.containsKey( theName );
	}

	public SettingDescriptor getDeclaredSettingDescriptor( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a setting descriptor from block descriptor '%s'.", this.name );
		return declaredSettingDescriptorMap.get( theName );
	}
	
	public Collection<SettingDescriptor> getDeclaredSettingDescriptors( ) {
		return declaredSettingDescriptorMap.values();
	}
	
	public ProfileDescriptor getProfileDescriptor( ) {
		return profileDescriptor;
	}
	
	protected void cleanup( ProfileDescriptor theProfileDescriptor ) {
		Preconditions.checkArgument( theProfileDescriptor != null, "Block descriptor '%s' is getting the profile descriptor set to null.", name );
		Preconditions.checkState( profileDescriptor == null, "Block descriptor '%s.%s' is getting profile descriptor reset to '%s'.", profileDescriptor == null ? "<empty>" : profileDescriptor.getName( ), name, theProfileDescriptor.getName() );
		profileDescriptor = theProfileDescriptor;
		
		if( includeArray != null ) {
			for( String include : includeArray ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Block descriptor '%s.%s' is attempting to include a blank or missing block (check source for trailing commas, etc).", profileDescriptor.getName(), name );
				Conditions.checkConfiguration( !name.equals( include ), "Block descriptor '%s.%s' is attempting to include itself.", profileDescriptor.getName(), name );
				Conditions.checkConfiguration( !includeList.contains( include ), "Block descriptor '%s.%s' is attempting to include block '%s' more than once.", profileDescriptor.getName(), name, include );
				includeList.add( include );
			}
		}

		if( declaredSettingDescriptorArray != null ) {
			for( SettingDescriptor settingDescriptor : declaredSettingDescriptorArray ) {
				Conditions.checkConfiguration( settingDescriptor != null, "Block descriptor '%s.%s' is attempting to include a missing setting descriptor (check source for trailing commas, etc).", profileDescriptor.getName(), name );
				Conditions.checkConfiguration( !declaredSettingDescriptorMap.containsKey( settingDescriptor.getName( ) ), "Block descriptor '%s.%s' is attempting to add setting descriptor '%s' more than once .", profileDescriptor.getName(), name, settingDescriptor.getName( ) );
				declaredSettingDescriptorMap.put( settingDescriptor.getName( ), settingDescriptor );
				settingDescriptor.cleanup( this );
			}
		}
	}
}
