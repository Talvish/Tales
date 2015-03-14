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

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Base64.Decoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * The in-memory version of the JsonWebToken. It allows for
 * retrieving headers and claims, checking validity and 
 * checking individual failures like signatures and time.
 * @author jmolnar
 *
 */
public class JsonWebToken {
	private final static Charset utf8 = Charset.forName( "UTF-8" );
	private final static Decoder base64Decoder = Base64.getUrlDecoder();

	private final Map<String,Object> headers;
	private final Map<String,Object> claims;
	private final String token;
	private String[] segments;
	
	private final Long expirationTimestamp;
	private final Long notBeforeTimestamp;


	
	/**
	 * Constructor meant only to be called by the manager.
	 * @param theHeaders the headers to use, they aren't copied but are made unmodifiable.
	 * @param theClaims the claims to use, they aren't copied but are made unmodifiable.
	 * @param theToken the string version of the token
	 */
	protected JsonWebToken( Map<String,Object> theHeaders, Map<String,Object> theClaims, String theToken ) {
		headers = Collections.unmodifiableMap( theHeaders );
		claims = Collections.unmodifiableMap( theClaims );
		token = theToken;
		segments = null;
		
		expirationTimestamp = extractLong( "exp" ); // TODO: make constants
		notBeforeTimestamp = extractLong( "nbf" );
	}

	/**
	 * Constructor meant only to be called by the manager.
	 * @param theHeaders the headers to use, they aren't copied but are made unmodifiable.
	 * @param theClaims the claims to use, they aren't copied but are made unmodifiable.
	 * @param theToken the string version of the token
	 * @param theSegments the segments that make up the tokens (this is an optimization)
	 */
	protected JsonWebToken( Map<String,Object> theHeaders, Map<String,Object> theClaims, String theToken, String[] theSegments ) {
		headers = Collections.unmodifiableMap( theHeaders );
		claims = Collections.unmodifiableMap( theClaims );
		token = theToken;
		segments = theSegments;
		
		expirationTimestamp = extractLong( "exp" ); // TODO: make constants
		notBeforeTimestamp = extractLong( "nbf" );
	}
	/**
	 * Helper method for the constructor, it takes the claim name
	 * and attempts to get a number from it. An exception is thrown
	 * if a value exists BUT it is not something we can use as a 
	 * long.
	 * @param theClaim the name of the claim
	 * @return null if not found, the value if found
	 */
	private Long extractLong( String theClaim ) {
		Object extractedObject = claims.get( theClaim );
		Long extractedValue = null;
		
		if( extractedObject != null ) {
			if( extractedObject instanceof Number ) {
				extractedValue = ( ( Number )extractedObject ).longValue();
			} else {
				throw new IllegalArgumentException( String.format( "The claim '%s' has value '%s' instead of a long", theClaim, extractedObject ) );
			}
		}
		return extractedValue;
	}

	/**
	 * Returns the string representation of the token.
	 * @return the token as a string
	 */
	public String getTokenString( ) {
		return token;
	}
	
	/**
	 * Protected method for use by the token manager.
	 * It is an optimization so that if we have split
	 * the segments already, we aren't doing it again. 
	 * @return the segments that make up the token
	 */
	protected String[] getSegments( ) {
		if( segments == null ) {
			segments = token.split( "\\." );
		}
		return segments;
	}
	
	/**
	 * Returns any headers that were used to create the token.
	 * @return the headers of the token
	 */
	public Map<String,Object> getHeaders( ) {
		return headers;
	}
	
	/**
	 * Returns any claims that were used to create the token.
	 * @return the claims of the token
	 */
	public Map<String,Object> getClaims( ) {
		return claims;
	}

	/**
	 * Checks the token to see if, from a time perspective only, the token is valid.
	 * This takes the current time and ensures it is less than or equal to the expiration ('exp') claim and
	 * greater than or equal to the not before ('nbf') claim.
	 * @return true if the time frame is valid, false otherwise
	 */
	public boolean isValidTimeframe( ) {
		long currentTimestamp = System.currentTimeMillis() / 1000l;
		boolean isValid = true;

		if( expirationTimestamp != null && expirationTimestamp < currentTimestamp ) {
			isValid = false;
		} else if( notBeforeTimestamp != null && notBeforeTimestamp > currentTimestamp ) {
			isValid = false;
		}
		
		return isValid;
	}
	
	/**
	 * Checks the token to see if, from a signature perspective only, the token is valid.
	 * This means either there is no signature or that the signing was checked and was valid.
	 * @return true if the signature is valid, false otherwise
	 */
	public boolean isValidSignature( String theSecret ) {
		Preconditions.checkArgument( segments.length >= 2, "token contains wrong number of segments" ); 

		// now we verify the signature
		Object signgingAlgorithmObject = this.headers.get( "alg" );
		Preconditions.checkNotNull( signgingAlgorithmObject, "the token is missing the signing algorithm" );
		String signingAlgorithmString = signgingAlgorithmObject.toString( );
		// it is possible that the fromString call below returns null which means 
		// 'alg' was 'none' and there wasn't any signing (and that is okay), and 
		// the fromString will ensure it is a valid value
		SigningAlgorithm signingAlgorithm = SigningAlgorithm.fromString( signingAlgorithmString );
		// now that we know the signing algorithm we can verify the segment count (which will change when we do encryption)
		Preconditions.checkArgument( ( signingAlgorithm == null && segments.length == 2 ) || ( signingAlgorithm != null && segments.length == 3 ), "token contains wrong number of segments" ); 
		
		// and finally we check the signatures (assuming it was signed)
		boolean isValid;
		if( signingAlgorithm != null ) {
			Preconditions.checkArgument( !Strings.isNullOrEmpty( theSecret ), "signing of type '%s' was indicated but the secret is missing", signingAlgorithm.name( ) );
			isValid = verifySignature( 
					String.join( ".", segments[ 0 ], segments[ 1 ] ), // we combine these since the signature requires it
					segments[ 2 ], 
					theSecret, 
					signingAlgorithm );
		} else {
			isValid = true;
		}
		return isValid;
	}
	
	/**
	 * Helper method that verifies the signature that was part of the of string token.
	 * @param theCombinedString the data string portions to process for re-signing
	 * @param theSignature the signature to check against
	 * @param theSecret the secret to use to reconstruct the signature
	 * @param theSigningAlgorithm the signing algorithm to use to reconstruct the signature
	 * @return true means the signature was verified and match, false means it didn't
	 */
	private boolean verifySignature( String theCombinedString, String theSignature, String theSecret, SigningAlgorithm theSigningAlgorithm ) {
		Mac mac;
		
		try {
			mac = Mac.getInstance( theSigningAlgorithm.getJavaName( ) );
			mac.init( new SecretKeySpec( theSecret.getBytes( utf8 ), theSigningAlgorithm.getJavaName( ) ) );
			
			// TODO: consider how to handle secret rotation

			return Arrays.equals( 
					mac.doFinal( theCombinedString.getBytes( ) ), 
					base64Decoder.decode( theSignature ) );

		} catch( NoSuchAlgorithmException e ) {
			throw new IllegalArgumentException( String.format( "Could not find the algorithm to used for the token." ), e );
		} catch( InvalidKeyException e ) {
			throw new IllegalStateException( String.format( "Key issues attempting to generate token." ), e );
		}
	}
	
	/**
	 * Checks the token to see if the provided audience is included in the token's audience claim.
	 * @param theAudience the audience value to check against the token's audience claim
	 * @return true if the audience is on the token's audience claim, false otherwise
	 */
	public boolean isValidAudience( String theAudience ) {
		// the spec seems to indicate that as long as 
		// the 'aud' claim is set that the audience must
		// be check no matches (or no local audience) 
		// then we have to reject the validity
		Object extractedObject = claims.get( "aud" );
		if( extractedObject != null ) {
			boolean found = false;		
			if( extractedObject instanceof String[] ) {
				String[] extractedValue = ( String[] )extractedObject;
				for( String item : extractedValue ) {
					if( item.equals( theAudience ) ) {
						found = true;
						break;
					}
				}
			} else if( extractedObject instanceof String ) {
				// translator may not make this necessary, but
				// we will do this to be safe
				String extractedValue = ( String )extractedObject;
				found = extractedValue.equals( theAudience ); 
			} else {
				throw new IllegalStateException( String.format( "The claim '%s' has value '%s' instead of a String[]", "aud", extractedObject ) );
			}
			return found;
		} else {
			return true;
		}
	}

//	@SuppressWarnings("unchecked")
//	private <T> T getHeader( String theClaimName, Class<T> theType ) {
//		try {
//			return ( T )this.headers.get( theClaimName );
//		} catch( ClassCastException e ) {
//			throw new IllegalStateException( String.format( "Attempting to get header '%s' with type '%s' but failed.", theClaimName, theType.getSimpleName( ) ), e );
//		}
//	}
//	
//	@SuppressWarnings("unchecked")
//	private <T> T getClaim( String theClaimName, Class<T> theType ) {
//		try {
//			return ( T )this.claims.get( theClaimName );
//		} catch( ClassCastException e ) {
//			throw new IllegalStateException( String.format( "Attempting to get claim '%s' with type '%s' but failed.", theClaimName, theType.getSimpleName( ) ), e );
//		}
//	}
	
	/**
	 * Checks that the token is valid by checking the time frame, signature and audience.
	 * The audience check looks to see if the token contains a non-empty audience claim and if so
	 * this method will fail since the audience wasn't sent in as a parameter.
	 * @return true if the time frame, signature and audience are valid, false otherwise
	 */
	public boolean isValidToken( String theSecret ) {
		// the token 'valid' should be the first item
		if( !isValidSignature( theSecret ) || !isValidTimeframe( ) || !isValidAudience( null ) ) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks that the token is valid by checking the time frame, signature and target audience.
	 * @param theAudience the audience value to check against the token's audience claim
	 * @return true if the time frame, signature and audience are valid, false otherwise
	 */
	public boolean isValidToken( String theSecret, String theAudience ) {
		if( !isValidSignature( theSecret ) || !isValidTimeframe( ) || !isValidAudience( theAudience ) ) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Override that simply returns the string representation of the token.
	 */
	@Override
	public String toString( ) {
		return getTokenString( );
	}
}
