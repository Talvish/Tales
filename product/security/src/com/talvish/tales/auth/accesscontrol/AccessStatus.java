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

/**
 * The enum describing the result of access checks.
 * @author jmolnar
 *
 */
public enum AccessStatus {
	/**
	 * Access request is valid and may be proceed.
	 */
	VERIFIED,
	/**
	 * Access request is invalid due to a missing token.
	 */
	MISSING_TOKEN,
	/**
	 * Access request is invalid due to a missing claim.
	 */
	MISSING_CLAIM,
	/**
	 * Access request is invalid due to a missing data related to a claim.
	 */
	MISSING_DATA,
	/**
	 * Access request is invalid due to a missing capabilities within a claim.
	 */
	MISSING_CAPABILITIES,
	/**
	 * Access request is invalid due to an invalid signature.
	 */
	INVALID_SIGNATURE,
	/**
	 * Access request is invalid because the request is either too earlier or too late.
	 */
	INVALID_TIMEFRAME,
	/**
	 * Access request is invalid because it is targeting a different audience.
	 */
	INVALID_AUDIENCE,
	/**
	 * Access request is invalid because it is contains incorrect data.
	 */
	INVALID_CLAIM,
	/**
	 * Access request is invalid for an unknown reason.
	 */
	UNKNOWN,
}