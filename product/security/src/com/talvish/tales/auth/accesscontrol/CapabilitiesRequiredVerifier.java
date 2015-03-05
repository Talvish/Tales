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

import java.util.BitSet;

import com.google.common.base.Preconditions;
import com.talvish.tales.auth.capabilities.Capabilities;
import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * A <code>ClaimVerifier</code> that checks for the existence of a set of capabilities from a particular
 * capability family. If the goal is to check for a only one, maybe two, capabilities from a family then 
 * <code>CapabilityRequiredVerify</code> may be a better choice due to overhead.
 * @author jmolnar
 *
 */
public class CapabilitiesRequiredVerifier implements ClaimVerifier {
	private Capabilities capabilities;

	/**
	 * Constructor taking the capabilities to be checked for.
	 * @param theCapabilities the capabilities to be checked for.
	 */
	public CapabilitiesRequiredVerifier( Capabilities theCapabilities ) {
		Preconditions.checkArgument( theCapabilities != null, "need capabilities to check for" );
		// TODO: consider a clone operation since these are modifiable and we return in the getter below
		capabilities = theCapabilities;
	}

	/**
	 * The required capabilities.
	 * @return the required capabilities
	 */
	public Capabilities getCapabilities( ) {
		return capabilities;
	}
	
	/**
	 * Verifies that the json web token has a certain set of capabilities. 
	 */
	@Override
	public void verify( JsonWebToken theToken, AccessResult theResult ) {
		// TODO: need to figure out the family to claim conversion
		// TODO: consider naming claim and capability (need to understand which is appropriate here)
		Capabilities tokenCapabilities = ( Capabilities )theToken.getClaims( ).get( capabilities.getFamily( ) );
		if( tokenCapabilities == null ) {
			theResult.setResult( AccessStatus.MISSING_CLAIM, "claim '%s' is missing", capabilities.getFamily( ) );
		} else {
			BitSet clone = ( BitSet )tokenCapabilities.getCapabilityBits().clone( );
			clone.and( capabilities.getCapabilityBits( ) );
			if( !clone.equals( capabilities.getCapabilityBits( ) ) ) {
				theResult.setResult( AccessStatus.MISSING_CAPABILITIES, "capabilities from claim '%s' are missing", capabilities.getFamily( ) );
			} else {
				theResult.setResult( AccessStatus.VERIFIED );
			}
		}
	}

}
