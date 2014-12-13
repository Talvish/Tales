// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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

/**
 * This class sets up a set of configuration claims or headers
 * that will be commonly used in the json web tokens. Not all 
 * possible JWT spec'ed public names are listed here and that 
 * is because there is an expectation that they will change 
 * somewhat frequently. The items in this class are less 
 * likely to be changing as tokens are generated.
 * <p>
 * Other than the signing algorithm, the configuration items
 * here can be set to null/false. This means that custom values
 * for the claims can be placed directly into the header and
 * claims map parameters sent to the generate token call.
 * @author jmolnar
 *
 */
public class GenerationConfiguration {
	private final Long validDuration;
	private final Long validDelayDuration;
	
	private final boolean includeIssuedTime;
	private final boolean generateId;
	private final SigningAlgorithm signingAlgorithm;
	
	private final String issuer;

	/**
	 * The basic configuration constructor.
	 * Using this constructor means that the issued time will not be set, 
	 * id generation will not happen time and the not-before time stamp
	 * will not be set automatically.
	 * @param shouldExpireIn the amount of time the token be valid for, where null means forever
	 * @param theSigningAlgorithm the signing algorithm to use, where null means no signing
	 */
	public GenerationConfiguration( 
			Long shouldExpireIn,
			SigningAlgorithm theSigningAlgorithm ) {
		this( null, shouldExpireIn, null, false, false, theSigningAlgorithm );
	}
	
	/**
	 * The full constructor settings.
	 * @param theIssuer the name to indicate who is issuing the token
	 * @param theValidDuration the amount of time the token be valid for, where null means forever
	 * @param theValidDelayDuration the amount of time to wait before the token is valid
	 * @param shouldIncludeIssuedTime indicates if the time the token was issued should be added as a claim
	 * @param shouldGenerateId indicates if a unique id should be automatically added to the token as a claim
	 * @param theSigningAlgorithm the signing algorithm to use, where null means no signing
	 */
	public GenerationConfiguration( 
			String theIssuer,
			Long theValidDuration,
			Long theValidDelayDuration,
			boolean shouldIncludeIssuedTime,
			boolean shouldGenerateId,
			SigningAlgorithm theSigningAlgorithm ) {
		issuer = theIssuer;
		signingAlgorithm = theSigningAlgorithm;
		validDuration = theValidDuration;
		validDelayDuration = theValidDelayDuration;
		includeIssuedTime = shouldIncludeIssuedTime;
		generateId = shouldGenerateId;
	}
	
	/**
	 * The value to use as the issuer for the json web token.
	 * @return the value to use as the token issuer
	 */
	public String getIssuer( ) {
		return issuer;
	}
	
	/**
	 * The number of seconds that the token will be valid for.
	 * This means, when a token is created the time stamp will be set to
	 * now + valid_delay_duration + valid_duration.
	 * If this is null, there is no expiration set and the claim isn't set.
	 * @return
	 */
	public Long getValidDuration( ) { 
		return validDuration;
	}
	
	/**
	 * The number of seconds from now that the token will start to be valid.
	 * This means, when a token is created the time stamp we be set to
	 * now + valid_delay_duration + valid_duration.
	 * If this is null then there is no delay and the claim isn't set.
	 * @return
	 */
	public Long getValidDelayDuration( ) { 
		return validDelayDuration;
	}
	
	/**
	 * Indicates if the time the token was issues should
	 * be included as a claim.
	 * @return true if to be included, false otherwise
	 */
	public boolean shouldIncludeIssuedTime( ) {
		return includeIssuedTime;
	}
	
	/**
	 * Indicates if a unique id should be added as a claim.
	 * @return true if id should be included, false otherwise
	 */
	public boolean shouldGenerateId( ) {
		return generateId;
	}
	
	/**
	 * Indicates what should be used for signing, including null if not signing is to happen.
	 * @return the algorithm to use for signing, or null if signing is not to happen
	 */
	public SigningAlgorithm getSigningAlgorithm( ) {
		return signingAlgorithm;
	}
}
