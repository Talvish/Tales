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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;

@DataContract( name="talvish.tales.configuration.hierarchical.config_descriptor")
public class ConfigDescriptor {
	@DataMember( name="includes" )
	private String[] includes;	
	@DataMember( name="profiles" )
	private ProfileDescriptor[] profiles;
	
	public String[] getIncludes( ) {
		return includes;		
	}
	
	public ProfileDescriptor[] getProfiles( ) {
		return profiles;
	}
	
	public ProfileDescriptor getProfile( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a profile name" );
		
		for( ProfileDescriptor profile : profiles ) {
			if( profile.getName().equals( theName ) ) {
				return profile;
			}
		}
		return null;
	}
}
