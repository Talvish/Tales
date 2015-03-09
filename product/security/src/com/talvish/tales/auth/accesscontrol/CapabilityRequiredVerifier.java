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
 * Checks a json web token for the existence of a particular claim that has a particular 
 * capability set. If you are looking to check three, maybe two, or more capabilities 
 * from a particular family, use <code>CapabilitiesRequiredVerifier</code>
 * @author jmolnar
 *
 */
public class CapabilityRequiredVerifier implements ClaimVerifier {
	private String claim;
	private String family;
	private String capabilityName;
	private int capabilityIndex;

	/**
	 * The constructor taking the needed data.
	 * @param theFamily the family of the capability to check for
	 * @param theCapabilityName the name given to the index, this is only used for error reporting
	 * @param theCapabilityIndex the index of the capability to check for
	 */
	public CapabilityRequiredVerifier( String theClaim, String theFamily, String theCapabilityName, int theCapabilityIndex ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theClaim ), "need a claim to check for" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need the family associated with the claim '%s'", theClaim );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theCapabilityName ), "cannot check in family '%s', associated with claim '%s', for an unknown capability", theFamily, theClaim );
		Preconditions.checkArgument( theCapabilityIndex > 0, "cannot check in family '%s', associated with claim '%s', for capability '%s' with index '%s' since it is not greater than zero", theFamily, theClaim, theCapabilityName, theCapabilityIndex );
		
		claim = theClaim;
		family = theFamily;
		capabilityIndex = theCapabilityIndex;
		capabilityName = theCapabilityName;
	}
	
	/**
	 * The claim the capability is found within. 
	 * @return the claim
	 */
	public String getClaim( ) {
		return claim;
	}
	
	/**
	 * The family that is associated with the specified claim.
	 * @return the family
	 */
	public String getFamily( ) {
		return family;
	}
	
	/**
	 * The capability name of the capability to check for within the claim.
	 * @return the capability name
	 */
	public String getCapabilityName( ) {
		return capabilityName;
	}

	/**
	 * The index of the capability to check for within the claim.
	 * @return the capability index
	 */
	public int getCapabilityIndex( ) {
		return capabilityIndex;
	}
	
	/**
	 * Checks the token for a particular capability.
	 */
	@Override
	public void verify( JsonWebToken theToken, AccessResult theResult ) {
		Capabilities tokenCapabilities = ( Capabilities )theToken.getClaims( ).get( claim );
		if( tokenCapabilities == null ) {
			theResult.setResult( AccessStatus.MISSING_CLAIM, "claim '%s' is missing", claim );
		} else if( !tokenCapabilities.hasCapability( capabilityIndex ) ) {
			theResult.setResult( AccessStatus.MISSING_CAPABILITIES, "capability '%s.%s' (index '%s') is missing from claim '%s'", family, capabilityName, capabilityIndex, claim );
		} else {
			theResult.setResult( AccessStatus.VERIFIED );
		}
	}
}
