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

@DataContract( name="talvish.tales.configuration.hierarchical.config_descriptor")
public class ConfigDescriptor {
	@DataMember( name="includes" )
	private String[] includeArray;	
	@DataMember( name="profiles" )
	private ProfileDescriptor[] profileDescriptorArray;
	
	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private String source;
	private final Map<String,ProfileDescriptor> profileDescriptorMap = new HashMap<>( );
	private final List<String> includeList = new ArrayList<>( );
	
	public Collection<String >getIncludes( ) {
		return includeList;		
	}

	public Collection<ProfileDescriptor> getProfileDescriptors( ) {
		return profileDescriptorMap.values( );
	}
	
	public ProfileDescriptor getProfileDescriptor( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a profile descriptor from config descriptor '%s'.", this.source );
		return profileDescriptorMap.get( theName );
	}

	public String getSource( ) {
		return source;
	}
	
	protected void cleanup( String theSource ) {
		// TODO: if we have state, then we can make sure it is right here
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ) );
		Preconditions.checkState( source == null, "Config descriptor '%s' is getting the source reset to '%s'.", source, theSource );
		source = theSource;

		if( includeArray != null ) {
			for( String include : includeArray ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Config descriptor '%s' is attempting to include a blank or missing source (check source for trailing commas, etc).", source );
				Conditions.checkConfiguration( !source.equals( include ), "Config descriptor '%s' is attempting to include itself.", source );
				Conditions.checkConfiguration( !includeList.contains( include ), "Config descriptor '%s' is attempting to include '%s' more than once.", source, include );
				includeList.add( include );
			}
		}

		if( profileDescriptorArray != null ) {
			for( ProfileDescriptor profileDescriptor : profileDescriptorArray ) {
				Conditions.checkConfiguration( profileDescriptor != null, "Config descriptor '%s' is attempting to include a missing profile descriptor (check source for trailing commas, etc).", source );
				Conditions.checkConfiguration( !profileDescriptorMap.containsKey( profileDescriptor.getName( ) ), "Config descriptor '%s' is attempting to add profile descriptor '%s' more than once.", source, profileDescriptor.getName( ) );
				profileDescriptorMap.put( profileDescriptor.getName( ), profileDescriptor );
				profileDescriptor.cleanup( this );
			}
		}
	}
}
