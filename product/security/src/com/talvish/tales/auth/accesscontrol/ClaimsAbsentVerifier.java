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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * A verifier that checks to make sure that certain claims do not exist on 
 * a json web token.
 * @author jmolnar
 *
 */
public class ClaimsAbsentVerifier implements ClaimVerifier {
	private List<String> claims;

	/**
	 * The constructor taking the required claims to look for.
	 * @param theClaims the claims to look for
	 */
	public ClaimsAbsentVerifier( String... theClaims ) {
		Preconditions.checkArgument( theClaims != null && theClaims.length > 0, "need claims" );
		claims = Arrays.asList( theClaims );
	}

	/**
	 * Returns the list of claims that are being looked for.
	 * @return the list of claims being looked for
	 */
	public List<String >getClaims( ) {
		return claims;
	}

	/**
	 * Verifies that the json web token does not contain the identified claims.
	 */
	@Override
	public void verify( JsonWebToken theToken, AccessResult theResult ) {
		for( String claim : claims ) {
			if( theToken.getClaims().containsKey( claim ) ) {
				theResult.setResult( AccessStatus.INVALID_CLAIM, "claim '%s' should not be here", claim );
				return;
			}
		}
		theResult.setResult( AccessStatus.VERIFIED );
	}
}
