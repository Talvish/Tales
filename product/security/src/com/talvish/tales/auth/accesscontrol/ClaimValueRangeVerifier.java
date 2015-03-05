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
import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * Verifier that checks to see if a particular claim exists, is an integer
 * and is between the specified minimum and maximum values.
 * @author jmolnar
 *
 */
public class ClaimValueRangeVerifier implements ClaimVerifier {
	private final String claim;
	
	private final int minimumValue;
	private final int maximumValue;
	
	/**
	 * The constructor taking the needed values.
	 * @param theClaim the claim to look for
	 * @param theMinimumValue the minimum value for the range
	 * @param theMaximumValue the maximum value for the range
	 */
	public ClaimValueRangeVerifier( String theClaim, int theMinimumValue, int theMaximumValue ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theClaim ), "need a claim name" );
		
		claim = theClaim;
		minimumValue = theMinimumValue;
		maximumValue = theMaximumValue;
	}

	/**
	 * The claim being looked for.
	 * @return the claim being looked for
	 */
	public String getClaim( ) {
		return claim;
	}
	
	/**
	 * The minimum value for the claim.
	 * @return the minimum value for the claim
	 */
	public int getMinimumValue( ) {
		return minimumValue;
	}

	/**
	 * The maximum value for the claim.
	 * @return the maximum value for the claim.
	 */
	public int getMaximumValue( ) {
		return maximumValue;
	}

	/**
	 * Checks that the specified claim has a value between the specified range.
	 */
	@Override
	public void verify( JsonWebToken theToken, AccessResult theResult ) {
		Object objectValue = theToken.getClaims( ).get( claim );
		if( objectValue == null ) {
			theResult.setResult( AccessStatus.MISSING_CLAIM, "claim '%s' is missing", claim );
		} else if( !( objectValue instanceof Integer ) ) {
			theResult.setResult( AccessStatus.INVALID_CLAIM, "claim '%s' does not have the write shape", claim );
		} else {
			int value = ( int )objectValue;
			if( value < minimumValue || value > maximumValue ) {
				theResult.setResult( AccessStatus.INVALID_CLAIM, "claim '%s' is not within range", claim );
			} else {
				theResult.setResult( AccessStatus.VERIFIED );
			}
		}
	}
}
