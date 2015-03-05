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
package com.talvish.tales.auth.accesscontrol;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.auth.capabilities.Capabilities;
import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * Checks a json web token for the existence of a particular 
 * capability. If you are looking to check three, maybe two, or more
 * capabilities from a particular family, use <code>CapabilitiesRequiredVerifier</code>
 * @author jmolnar
 *
 */
public class CapabilityRequiredVerifier implements ClaimVerifier {
	private String family;
	private String capabilityName;
	private int capabilityIndex;

	/**
	 * The constructor taking the needed data.
	 * @param theFamily the family of the capability to check for
	 * @param theCapabilityName the name given to the index, this is only used for error reporting
	 * @param theCapabilityIndex the index of the capability to check for
	 */
	public CapabilityRequiredVerifier( String theFamily, String theCapabilityName, int theCapabilityIndex ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need the family to check for" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( capabilityName ), "cannot check in family '%s' for an unknown capability", theFamily );
		Preconditions.checkArgument( theCapabilityIndex > 0, "cannot check in family '%s' for capability '%s' with index '%s' since it is not greater than zero", theFamily, theCapabilityName, theCapabilityIndex );
		
		family = theFamily;
		capabilityIndex = theCapabilityIndex;
		capabilityName = theCapabilityName;
	}
	
	/**
	 * Checks the token for a particular capability.
	 */
	@Override
	public void verify( JsonWebToken theToken, AccessResult theResult ) {
		// TODO: need to figure out the family to claim conversion
		Capabilities tokenCapabilities = ( Capabilities )theToken.getClaims( ).get( family );
		if( tokenCapabilities == null ) {
			theResult.setResult( AccessStatus.MISSING_CLAIM, "claim '%s' is missing", family );
		} else if( !tokenCapabilities.hasCapability( capabilityIndex ) ) {
			theResult.setResult( AccessStatus.MISSING_CAPABILITIES, "capability '%s.%s' is missing", family, capabilityName );
		} else {
			theResult.setResult( AccessStatus.VERIFIED );
		}
	}
}
