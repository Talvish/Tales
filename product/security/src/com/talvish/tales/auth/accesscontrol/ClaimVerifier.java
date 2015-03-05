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

import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * Interface outlining a method that is used to check claims for particular conditions.
 * Implementations can check one or more than one claim and the result of the method
 * should never be null. 
 * @author jmolnar
 *
 */
public interface ClaimVerifier {
	/**
	 * Verifies that the json web token is valid for whatever
	 * claims this verifier believes are important. Parameters
	 * will not be null.
	 * @param theToken the token to check
	 * @param theAccessResult the class that the implementor writes the success, this is passed in and not returned to minimize potential performance impact 
	 * @return the status of the check, should not be null
	 */
	void verify( JsonWebToken theToken, AccessResult theAccessResult );
}


