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
import com.talvish.tales.auth.capabilities.CapabilityDefinitionManager;
import com.talvish.tales.auth.jwt.JsonWebToken;
import com.talvish.tales.auth.jwt.TokenManager;

/**
 * An implementation of the access control manager that uses as well known
 * hard-fixed string as the secret for checking the signature of access tokens. 
 * @author jmolnar
 *
 */
public class SimpleAccessControlManager extends AccessControlManager<AccessResult> {
	private String secret;

	/**
	 * Constructor taking the capability definition family and needed secret.
	 * @param theSecret the secret to use to verify tokens
	 * @param theDefinitionManager the capability definition manager.
	 * @param theTokenManager the token manager
	 */
	public SimpleAccessControlManager( String theSecret, CapabilityDefinitionManager theDefinitionManager, TokenManager theTokenManager ) {
		super( theDefinitionManager, theTokenManager );
		secret = theSecret;
	}
	
	/**
	 * Verifies that the json web token, including a proper signature using the fixed secret, 
	 * and then checks the token has the claims and capabilities that the specified method
	 * requires. 
	 */
	@Override
	public AccessResult verifyAccess( MethodAccessDescriptor theMethod, JsonWebToken theToken ) {
		Preconditions.checkNotNull( theMethod, "need a method" );
		AccessResult result = new AccessResult( );
			
		if( theToken == null ) {
			result.setResult( AccessStatus.MISSING_TOKEN );
		} else {
			if( !theToken.isValidSignature( secret ) ) {
				result.setResult( AccessStatus.INVALID_SIGNATURE, "the token signature is invalid" );
			} else if( !theToken.isValidTimeframe( ) ) {
				result.setResult( AccessStatus.INVALID_TIMEFRAME, "the token is not valid for the current time" );
			} else if( !theToken.isValidAudience( null ) ) {
				result.setResult( AccessStatus.INVALID_AUDIENCE, "the token is not valid due to target audience" );
			} else {
				theMethod.verifyAccess( theToken, result );
			}
		}
		return result;
	}	
}
