// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.auth.jwt;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.auth.capabilities.Capabilities;
import com.talvish.tales.serialization.TypeFormatAdapter;

/**
 * Details about claims that were registered with the token manager and how 
 * to translator the data for the claim.
 * @author jmolnar
 *
 */
public class ClaimDetails {
	private final String name;
	private final TypeFormatAdapter typeAdapter;
	private final String capabilityFamily;

	/**
	 * The constructor taking just the claim name and type information.
	 * @param theName the name of the claim
	 * @param theTypeAdapter the type and translation details for the claim
	 */
	public ClaimDetails( String theName, TypeFormatAdapter theTypeAdapter ) {
		this( theName, null, theTypeAdapter );
	}
	
	/**
	 * The constructor for capabilities, which means taking the claim name, type information and the capability family.
	 * @param theName the name of the claim
	 * @param theCapabilityFamily the family for the capability
	 * @param theTypeAdapter the type and translation details for the claim
	 */
	public ClaimDetails( String theName, String theCapabilityFamily, TypeFormatAdapter theTypeAdapter ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a claim name" );
		Preconditions.checkNotNull( theTypeAdapter, "claim '%s' needs a type reference", theName );
		Preconditions.checkArgument( theCapabilityFamily == null || theTypeAdapter.getType().getUnderlyingClass().equals( Capabilities.class ), "claim '%s' indicates it is a capability but type given is '%s'", theName, theTypeAdapter.getType().getUnderlyingClass().getSimpleName( ) );

		name = theName;
		typeAdapter = theTypeAdapter;
		capabilityFamily = theCapabilityFamily;
	}
	
	/**
	 * The claim name.
	 * @return the claim name
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The adapter to translator to/from json for a particular type.
	 * @return the type format adapter
	 */
	public TypeFormatAdapter getTypeAdapter( ) {
		return typeAdapter;
	}
	
	/**
	 * The family for for the capabilities in the claim.
	 * @return the capability family of the claim
	 */
	public String getCapabilityFamily( ) {
		return capabilityFamily;
	}
}