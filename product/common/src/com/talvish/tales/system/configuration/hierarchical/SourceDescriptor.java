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

// TODO: consider having a state on this that manages through creation, load and then clean-up
// TODO: I don't like the name 


/**
 * The class represents the main source for a set of profiles, blocks and settings.
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.config_descriptor")
public class SourceDescriptor {
	@DataMember( name="includes" )
	private String[] includeArray;	
	@DataMember( name="profiles" )
	private ProfileDescriptor[] declaredProfileArray;
	
	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private String sourcePath;
	private final Map<String,ProfileDescriptor> declaredProfileMap = new HashMap<>( );
	private final List<String> includeList = new ArrayList<>( );
	
	/**
	 * The additional sources to include within this source.
	 * @return returns the list of includes or an empty list if none
	 */
	public Collection<String >getIncludes( ) {
		return includeList;		
	}

	/**
	 * Returns the list of profiles declared within this source.
	 * @return the list of profiles declared or an empty list if none
	 */
	public Collection<ProfileDescriptor> getDeclaredProfiles( ) {
		return declaredProfileMap.values( );
	}
	
	/**
	 * Gets the profile declared within this source.
	 * @param theName the name of the declared profile to get
	 * @return returns the declared profile if found here, null otherwise
	 */
	public ProfileDescriptor getDeclaredProfile( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a profile from config source '%s'.", this.sourcePath );
		return declaredProfileMap.get( theName );
	}

	/**
	 * The source path.
	 * @return the source path
	 */
	public String getSourcePath( ) {
		return sourcePath;
	}
	
	/**
	 * Helper method called after the source loads. 
	 * This is also how the source path is set on the source. 
	 * It does some simple validation and sets up additional data to help 
	 * with runtime support.
	 * @param theSourcePath the path of the source
	 */
	protected void onDeserialized( String theSourcePath ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSourcePath ) );
		Preconditions.checkState( sourcePath == null, "Config source '%s' is getting the source path reset to '%s'.", sourcePath, theSourcePath );
		sourcePath = theSourcePath;

		if( includeArray != null ) {
			for( String include : includeArray ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Config source '%s' is attempting to include a blank or missing source (check source for trailing commas, etc).", sourcePath );
				Conditions.checkConfiguration( !sourcePath.equals( include ), "Config source '%s' is attempting to include itself.", sourcePath );
				Conditions.checkConfiguration( !includeList.contains( include ), "Config source '%s' is attempting to include '%s' more than once.", sourcePath, include );
				includeList.add( include );
			}
		}

		if( declaredProfileArray != null ) {
			for( ProfileDescriptor profile : declaredProfileArray ) {
				Conditions.checkConfiguration( profile != null, "Config descriptor '%s' is attempting to include a missing profile descriptor (check source for trailing commas, etc).", sourcePath );
				Conditions.checkConfiguration( !declaredProfileMap.containsKey( profile.getName( ) ), "Config descriptor '%s' is attempting to add profile descriptor '%s' more than once.", sourcePath, profile.getName( ) );
				declaredProfileMap.put( profile.getName( ), profile );
				profile.onDeserialized( this );
			}
		}
	}
}
