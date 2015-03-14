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

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.talvish.tales.auth.capabilities.Capabilities;
import com.talvish.tales.auth.capabilities.StringToTokenCapabilityTranslator;
import com.talvish.tales.auth.capabilities.TokenCapabilityToStringTranslator;
import com.talvish.tales.contracts.data.DataContractTypeSource;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.serialization.json.JsonTypeReference;
import com.talvish.tales.serialization.json.translators.ArrayToJsonArrayTranslator;
import com.talvish.tales.serialization.json.translators.ChainToStringToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.JsonArrayToArrayTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToStringToChainTranslator;

/**
 * The manager is essentially a factory for creating json web tokens, but
 * allows registering json serialization handlers for particular claims.
 * The manager is able to handle string, number and boolean json values
 * automatically, but other types (e.g. arrays, and objects) are not 
 * handled UNLESS you use the registration mechanism. 
 * <p>
 * The general approach for the manager (and token) is that exceptions
 * are thrown when the data and format is unexpected. Things like
 * being signature failures or being expired do not throw exceptions,
 * instead there are methods for checking validity.
 * @author jmolnar
 *
 */
public class TokenManager {
	// members that are used to ultimately encode/decode the overall results
	private final static Gson gson = new GsonBuilder( ).serializeNulls( ).create();
	private final static JsonParser jsonParser = new JsonParser( );
	private final static Charset utf8 = Charset.forName( "UTF-8" );
	private final static Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
	private final static Decoder base64Decoder = Base64.getUrlDecoder();
	
	// the following regular expression is based on RFC 3986 (Appendix B) but modified to require the
	// scheme and colon since the jwt spec calls for StringOrUri to be a URI not a URI-reference
	// note: this has left the matching groups in and appendix B could be used to pull out the pieces
	private final static String uriRegex = "^(([^:/?#]+):)(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";
	private final static Pattern uriPattern = Pattern.compile( uriRegex );
	
	
	private final GenerationConfiguration defaultConfiguration;
	private final JsonTranslationFacility translationFacility;
	private final Map<String,ClaimDetails> claimHandlers = new HashMap<>( );

	
	/**
	 * Creates a manager with a default configuration of a) no timing/expiration
	 * and b) uses HS256 for the signing.
	 */
	public TokenManager( ) {
		this( null, null );
	}
	
	/**
	 * Creates a manager using the specified default configuration and translation facility.
	 * The default configuration is used when generating a token from
	 * a set of headers and claims and specific configuration is not
	 * provided at that time.
	 * @param theDefaultConfiguration the default configuration to use 
	 * @param theTranslationFacility the facility to aid the translation of types to/from json
	 */
	public TokenManager( GenerationConfiguration theDefaultConfiguration, JsonTranslationFacility theTranslationFacility ) {
		// TODO: consider other configuration for things like, how to handle the unknown json objects that come down the pipe
		//		 could make it so it leaves it as a string to deal, but we want that configurable
		
		if( theDefaultConfiguration == null ) {
			defaultConfiguration = new GenerationConfiguration( null, SigningAlgorithm.HS256 );
		} else {
			defaultConfiguration = theDefaultConfiguration;
		}
		if( theTranslationFacility == null ) {
			translationFacility = new JsonTranslationFacility( new DataContractTypeSource( ) );
		} else {
			translationFacility = theTranslationFacility;
		}

		// going to register handlers for specific claims
		
		// we have a bit of special handling for the string[] to handle the single items to/from the json array
		JavaType elementType = new JavaType( String.class );
		JsonTypeReference elementTypeReference = translationFacility.getTypeReference( elementType );

		_registerClaim( 
				"aud", 
				null,
	        	new JsonTypeReference( 
	        			new JavaType( String[].class ), 
	        			"list[string]",
	        			new JsonArrayToArrayTranslator( elementType.getUnderlyingClass(), elementTypeReference.getFromJsonTranslator(), true ),
	        			new ArrayToJsonArrayTranslator( elementTypeReference.getToJsonTranslator( ), true ) ) );
		// could consider doing the other's like exp or nbf
	}

	/**
	 * Registers a type for a particular claim (or header). This means that when this particular 
	 * claim comes up it will use the translators associated with that type to convert to and from the json.
	 * @param theClaimName the name of the claim to associate with a type
	 * @param theType the type of the claim, which means the system will attempt to translate to and from that type
	 */
	public void registerClaim( String theClaimName, Type theType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theClaimName ), "need a claim name" );
		Preconditions.checkNotNull( theType, "need type for claim '%s'", theClaimName );
		Preconditions.checkArgument( !claimHandlers.containsKey( theClaimName ), "a type/capability was already registered for claim '%s'", theClaimName );

		_registerClaim(
				theClaimName, 
				null,
				translationFacility.getTypeReference( new JavaType( theType ) ) );
	}

	/**
	 * Registers a type for a particular claim (or header). This means that when this particular 
	 * claim comes up it will use the translators associated with that type to convert to and from the json.
	 * @param theClaimName the name of the claim to associate with a type
	 * @param theTypeReference the type reference of the claim, which means the system will attempt to translate to and from 
	 */
	public void registerClaim( String theClaimName, JsonTypeReference theTypeReference ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theClaimName ), "need a claim name" );
		Preconditions.checkNotNull( theTypeReference, "need type reference for claim '%s'", theClaimName );
		Preconditions.checkArgument( !claimHandlers.containsKey( theClaimName ), "a type/capability was already registered for claim '%s'", theClaimName );

		_registerClaim( 
				theClaimName, 
				null, 
				theTypeReference );
	}

	/**
	 * Registers a particular claim as a set of capability bits.
	 * @param theClaimName the claim name to use
	 * @param theCapabilityFamily the family of capability bits in the claim
	 */
	public void registerCapability( String theClaimName, String theCapabilityFamily ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theClaimName ), "need a claim name" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theCapabilityFamily ), "need a capability family" );
		Preconditions.checkArgument( !claimHandlers.containsKey( theClaimName ), "a type/capability was already registered for claim '%s'", theClaimName );

		_registerClaim( 
				theClaimName,
				theCapabilityFamily,
				new JsonTypeReference( 
						new JavaType( Capabilities.class ),
						"capabilities : string",
						new JsonElementToStringToChainTranslator( new StringToTokenCapabilityTranslator( theCapabilityFamily ) ),
						new ChainToStringToJsonPrimitiveTranslator( new TokenCapabilityToStringTranslator( ) ) ) );						
	}
	
	/**
	 * Private registration mechanism used by the public version and the constructor. 
	 * @param theClaimName the name of the claim to associate with a type
	 * @param theCapabilityFamily the family for the capability, if the claim represents capabilities
	 * @param theTypeReference the type reference of the claim, which means the system will attempt to translate to and from 
	 */
	private final void _registerClaim( String theClaimName, String theCapabilityFamily, JsonTypeReference theTypeReference ) {
		// we register the handler and not type since it speeds up the runtime slightly 
		// and does give us more options for if we want more custom handling of claims
		claimHandlers.put( 
				theClaimName, 
				new ClaimDetails( theClaimName, theCapabilityFamily, theTypeReference ) ); 
	}
	
	/**
	 * Returns the details around a particular claim. 
	 * This only returns details for claims that have been registered.
	 * @param theName the name of claim to get details for.
	 * @return the details for the claim or null if the claim wasn't registered
	 */
	public ClaimDetails getRegisteredClaim( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name to get claim details" );
		return claimHandlers.get( theName );
	}

	/**
	 * Creates a json web token from a set of claims and a secret, if signing. This call
	 * uses the default configuration.
	 * <p>
	 * This call is made when a new, never having existed, token is to be created and sent out into the world.
	 * @param theClaims the claims to be placed into the token
	 * @param theSecret the secret to use when signing the token, it can be null if signing is not enabled
	 * @return returns a json web token 
	 */
	public JsonWebToken generateToken( Map<String,Object> theClaims, String theSecret ) {
		return this.generateToken( null, theClaims, theSecret, defaultConfiguration );
	}

	// TODO: need to figure out how to handle the secret side better
	// 		 need to options, a secret may not be sufficient and
	//		 a secret doesn't help with you need to do some form
	//		 'key/secret' rotation, options include specify some
	//		 form of key/secret id which would be used to lookup
	// 		 what should be used

	/**
	 * Creates a json web token from a set of claims and a secret and a set of configuration. In addition
	 * it allows you to specify additional headers. This really meant to support the JWT spec where it
	 * indicates that encrypted tokens can have claims in the header, since the header would be in the 
	 * clear. Encryption, however, is not yet supported.
	 * <p>
	 * This call is made when a new, never having existed, token is to be created and sent out into the world.
	 * @param theHeaders the headers to used for the token
	 * @param theClaims the claims to be placed into the token
	 * @param theSecret the secret to use when signing the token, it can be null if signing is not enabled
	 * @param theConfiguration the configuration to use when creating the tken
	 * @return returns a json web token 
	 */
	public JsonWebToken generateToken( Map<String,Object> theHeaders, Map<String,Object> theClaims, String theSecret, GenerationConfiguration theConfiguration ) {
		// make sure we have defaults if not provided
		if( theConfiguration == null ){
			theConfiguration = defaultConfiguration;
		}
		
		// first we look at the headers
		
		if( theHeaders == null ) {
			theHeaders = new HashMap<>( );
		} else {
			theHeaders = new HashMap<>( theHeaders ); // copying for no side-effects
		}
		
		SigningAlgorithm signingAlgorithm = theConfiguration.getSigningAlgorithm( );	
		
		// need to setup the configuration based headers
		// first we have the signing algorithm
		if( signingAlgorithm != null ) {
			Preconditions.checkArgument( !Strings.isNullOrEmpty( theSecret ), "signing of type '%s' is configured but the secret is missing", signingAlgorithm.name( ) );
			theHeaders.put( "alg", signingAlgorithm.name( ) );
		} else {
			theHeaders.put( "alg", "none" );
		}
		// not putting in the following because it is only needed when doing encryption (and value would be 'JWE')
		// theHeaders.put( "typ",  "JWT" );
		// we now process the map and produce the header segment 
		String headersSegment = processMap( theHeaders );

		// second we look at the claims
		
		if( theClaims == null ) {
			theClaims = new HashMap<>( );
		} else {
			theClaims = new HashMap<>( theClaims ); // copying for no side-effects
		}

		// need to setup the configuration based claims
		// if the configuration is not null/false then 
		// this code over writes the values in the claims
		// but if null/false then the developer using 
		// this method can use their own values by 
		// setting the values in the map parameters
		
		// the indication of who issues the token
		if( theConfiguration.getIssuer( ) != null ) {
			theClaims.put( "iss",  theConfiguration.getIssuer( ) );
		}
		// unique id (if configured)
		if( theConfiguration.shouldGenerateId() ) {
			theClaims.put( "jti", UUID.randomUUID().toString( ) );
		}
		// some timing based configuration
		long now = System.currentTimeMillis( ) / 1000l;
		if( theConfiguration.shouldIncludeIssuedTime( ) ) {
			theClaims.put( "iat", now );
		}
		Long validDelay = theConfiguration.getValidDelayDuration( );
		if( validDelay != null ) {
			theClaims.put( "nbf",  now + validDelay );
		} else {
			validDelay = 0l; // we set this for the expiration below
		}
		Long expiresIn = theConfiguration.getValidDuration( );
		if( expiresIn != null ) {
			theClaims.put( "exp", now + validDelay + expiresIn );
		}
		// we generate the json from the passed-in/configured claims 
		String claimsSegment = processMap( theClaims );		
		
		// we create the combined segments that will be signed
		String combinedSegments = String.join( ".", headersSegment, claimsSegment );
		
		
		// just need to create that final segments, the signature
		if( signingAlgorithm != null ) {
			// and now we need sign (using the configuration algorithm)
			Mac mac;
			
			try {
				mac = Mac.getInstance( signingAlgorithm.getJavaName( ) );
				mac.init( new SecretKeySpec( theSecret.getBytes( utf8 ), signingAlgorithm.getJavaName( ) ) );
	
				byte[] signatureBytes = mac.doFinal( combinedSegments.getBytes( ) );		
				String signatureSegment = base64Encoder.encodeToString( signatureBytes );
				combinedSegments = String.join( ".", combinedSegments, signatureSegment );				
	
			} catch( NoSuchAlgorithmException e ) {
				throw new IllegalArgumentException( String.format( "Could not find the algorithm to used for the token." ), e );
			} catch( InvalidKeyException e ) {
				throw new IllegalStateException( String.format( "Key issues attempting to generate token." ), e );
			}
		} else {
			// no signing, so slap a dot on the end
			combinedSegments += ".";
		}
		// and now we have our token
		return new JsonWebToken( theHeaders, theClaims, combinedSegments  );
	}
	
	
	/**
	 * Creates a json web token from an previous token, in addition to the new claims and a secret, if signing. This call
	 * uses the default configuration. The values in the header and claims of the previous token are placed into the new
	 * token and then the then new claims are added. The new claims will overwrite any existing claims.  Any well known
	 * claims (e.g. iss, jti, etc) will also not transfer but will be based on the configuration.
	 * @param theOriginalToken the token to base the new token on
	 * @param theClaims the claims to be placed into the token
	 * @param theSecret the secret to use when signing the token, it can be null if signing is not enabled
	 * @return returns a json web token 
	 */
	public JsonWebToken generateToken( JsonWebToken theOriginalToken, Map<String,Object> theClaims, String theSecret ) {
		return this.generateToken( theOriginalToken, null, theClaims, theSecret, defaultConfiguration );
	}

	/**
	 * Creates a json web token from an previous token, in addition to the new claims and a secret, if signing. This call
	 * uses the default configuration. The values in the header and claims of the previous token are placed into the new
	 * token and then the then new claims are added. The new claims will overwrite any existing claims.  Any well known
	 * claims (e.g. iss, jti, etc) will also not transfer but will be based on the configuration.
	 * In addition it allows you to specify additional headers. This really meant to support the JWT spec where it
	 * indicates that encrypted tokens can have claims in the header, since the header would be in the clear. Encryption, 
	 * however, is not yet supported.
	 * @param theOriginalToken the token to base the new token on
	 * @param theHeaders the headers to used for the token
	 * @param theClaims the claims to be placed into the token
	 * @param theSecret the secret to use when signing the token, it can be null if signing is not enabled
	 * @param theConfiguration the configuration to use when creating the tken
	 * @return returns a json web token 
	 */
	public JsonWebToken generateToken( JsonWebToken theOriginalToken, Map<String,Object> theHeaders, Map<String,Object> theClaims, String theSecret, GenerationConfiguration theConfiguration ) {
		// make sure we have defaults if not provided
		if( theConfiguration == null ){
			theConfiguration = defaultConfiguration;
		}

		// first we look at the headers
		
		// for headers, we simply take the original (since the alg algorithm will overwrite any existing value)
		if( theHeaders == null ) {
			theHeaders = new HashMap<>( theOriginalToken.getHeaders( ) );
		} else {
			theHeaders = new HashMap<>( theHeaders );
			theHeaders.putAll( theOriginalToken.getHeaders( ) );
		}
		
		SigningAlgorithm signingAlgorithm = theConfiguration.getSigningAlgorithm( );	
		
		// need to setup the configuration based headers

		// first we have the signing algorithm
		if( signingAlgorithm != null ) {
			Preconditions.checkArgument( !Strings.isNullOrEmpty( theSecret ), "signing of type '%s' is configured but the secret is missing", signingAlgorithm.name( ) );
			theHeaders.put( "alg", signingAlgorithm.name( ) );
		} else {
			theHeaders.put( "alg", "none" );
		}
		// not putting in the following because it is only needed when doing encryption (and value would be 'JWE')
		// theHeaders.put( "typ",  "JWT" );
		// we now process the map and produce the header segment 
		String headersSegment = processMap( theHeaders );

		// second we look at the claims

		// for claims we copy the original and then reset any important well known claims below
		if( theClaims == null ) {
			theClaims = new HashMap<>( theOriginalToken.getClaims( ) );
		} else {
			theClaims = new HashMap<>( theClaims );
			theClaims.putAll( theOriginalToken.getClaims( ) );
		}

		// need to setup the configuration based claims
		// if the configuration is not null/false then 
		// this code over writes the values in the claims
		// but if null/false then the developer using 
		// this method can use their own values by 
		// setting the values in the map parameters
		
		// the indication of who issues the token
		if( theConfiguration.getIssuer( ) != null ) {
			theClaims.put( "iss",  theConfiguration.getIssuer( ) );
		} else {
			theClaims.remove( "iss" );
		}
		// unique id (if configured)
		if( theConfiguration.shouldGenerateId() ) {
			theClaims.put( "jti", UUID.randomUUID().toString( ) );
		} else {
			theClaims.remove( "jti" );
		}
		// some timing based configuration
		long now = System.currentTimeMillis( ) / 1000l;
		if( theConfiguration.shouldIncludeIssuedTime( ) ) {
			theClaims.put( "iat", now );
		} else {
			theClaims.remove( "iat" );
		}
		Long validDelay = theConfiguration.getValidDelayDuration( );
		if( validDelay != null ) {
			theClaims.put( "nbf",  now + validDelay );
		} else {
			theClaims.remove( "nbf" );
			validDelay = 0l; // we set this for the expiration below
		}
		Long expiresIn = theConfiguration.getValidDuration( );
		if( expiresIn != null ) {
			theClaims.put( "exp", now + validDelay + expiresIn );
		} else {
			theClaims.remove( "exp" );
		}
		// we generate the json from the passed-in/configured claims 
		String claimsSegment = processMap( theClaims );		
		
		// we create the combined segments that will be signed
		String combinedSegments = String.join( ".", headersSegment, claimsSegment );
		
		
		// just need to create that final segments, the signature
		if( signingAlgorithm != null ) {
			// and now we need sign (using the configuration algorithm)
			Mac mac;
			
			try {
				mac = Mac.getInstance( signingAlgorithm.getJavaName( ) );
				mac.init( new SecretKeySpec( theSecret.getBytes( utf8 ), signingAlgorithm.getJavaName( ) ) );
	
				byte[] signatureBytes = mac.doFinal( combinedSegments.getBytes( ) );		
				String signatureSegment = base64Encoder.encodeToString( signatureBytes );
				combinedSegments = String.join( ".", combinedSegments, signatureSegment );				
	
			} catch( NoSuchAlgorithmException e ) {
				throw new IllegalArgumentException( String.format( "Could not find the algorithm to used for the token." ), e );
			} catch( InvalidKeyException e ) {
				throw new IllegalStateException( String.format( "Key issues attempting to generate token." ), e );
			}
		} else {
			// no signing, so slap a dot on the end
			combinedSegments += ".";
		}
		// and now we have our token
		return new JsonWebToken( theHeaders, theClaims, combinedSegments  );
	}
	
	/**
	 * Helper method that takes the map of claims/headers and then converts them
	 * into utf-8, json, base64 encoded string. This will used any registered
	 * claims handlers to create the correct json string.
	 * @param theMap the map of claims
	 * @return the utf-8, json, based64 encoded string of the claims
	 */
	private String processMap( Map<String,Object> theMap  ) {
		ClaimDetails claimDetails;
		JsonObject outputJson = new JsonObject( );

		for( Entry<String,Object> entry : theMap.entrySet() ) {
			// quick note, the JWT spec says headers and claims shouldn't have
			// duplicates but apps allow it so there is no attempt to enforce
			claimDetails = this.claimHandlers.get( entry.getKey( ) );
			if( claimDetails != null ) {
				try {
					outputJson.add( entry.getKey( ), ( JsonElement )claimDetails.getTypeReference( ).getToJsonTranslator( ).translate( entry.getValue( ) ) );
				} catch( TranslationException e ) {
					// this will help with understanding problems ...
					throw new IllegalArgumentException( String.format( "Claim '%s' is using a custom translation that failed to translate the associated value.", entry.getKey( ) ), e );
				}
			} else if( entry.getValue( ) instanceof String ) {
				outputJson.addProperty( entry.getKey( ), validateString( entry.getKey( ), ( String )entry.getValue( ) ) );
			} else if( entry.getValue( ) instanceof Number ) {
				outputJson.addProperty( entry.getKey( ), ( Number )entry.getValue( ) );
			} else if( entry.getValue( ) != null && Boolean.class.isAssignableFrom( entry.getValue().getClass( ) ) ) {
				// no need to check for boolean primitive type since the map cannot hold onto primitive types, booleans will be boxed into Boolean
				outputJson.addProperty( entry.getKey( ), ( Boolean )entry.getValue( ) );
			} else {
				throw new IllegalArgumentException( String.format( "Claim '%s' is using type '%s', which has no mechanism for translation.", entry.getKey(), entry.getValue( ).getClass().getSimpleName() ) );
			}
		}
		return base64Encoder.encodeToString( gson.toJson( outputJson ).getBytes( utf8 ) );
	}
	
	/**
	 * Helper method that ensures that the string passed in is 
	 * of the correct format. According to the JWT spec any 
	 * string with a colon must be a URI. 
	 * @param theName the name of the claim
	 * @param theValue the value of the claim
	 * @throws IllegalArgumentException thrown if the value is null or if the value contains a colon but doesn't confirm to the URI spec
	 * @return return the string the string that was passed in
	 */
	private String validateString( String theName, String theValue ) {
		if( theValue == null ) {
			throw new IllegalArgumentException( String.format( "Claim '%s' was set with a null value, which is not permitted.", theName ) );
		} else if( theValue.indexOf( ':' ) < 0 ) {
			return theValue;
		} else if( uriPattern.matcher( theValue ).matches( ) ) {
			return theValue;
		} else {
			throw new IllegalArgumentException( String.format( "Claim '%s' is using a value '%s', which contains a ':' but does not match the URI spec '%s'.", theName, theValue, uriRegex ) );
		}
	}
	
	/**
	 * Creates a json web token from a string. This call does not validate the token
	 * but instead makes the header and claims available for evaluation. A separate
	 * call should be made to validate.
	 * <p>
	 * This call is made when an existing token has been received and the claims are to be used and the
	 * token needs validation.
	 * @param theTokenString the string representation of the token to generate into the full tken
	 * @return returns an unvalidated json web token 
	 */
	public JsonWebToken generateToken( String theTokenString ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theTokenString ), "need a token string to generate a token" );

		String[] segments = theTokenString.split( "\\." );
		Preconditions.checkArgument( segments.length >= 2, "token contains wrong number of segments" ); 

		Map<String,Object> claimItems = null;
		Map<String,Object> headerItems = null;
		
		// need to process the items in the header and claims
		claimItems = processSegment( segments[ 1 ], 1 );
		headerItems = processSegment( segments[ 0 ], 0 );
		
		return new JsonWebToken( headerItems, claimItems, theTokenString, segments );
	}

	/**
	 * Helper method that takes a string segment (e.g. headers, claims) and 
	 * base64 decodes, parses out the json and generates a map of the values. 
	 * @param theSegment the segment to process
	 * @return the map of values generated from the segment
	 */
	private Map<String,Object> processSegment( String theSegment, int theSegmentIndex ) {
		Map<String,Object> outputItems = new HashMap<>( );
		ClaimDetails claimDetails;
		String claimName = null;
		JsonElement claimValue = null;
		
		try {
			JsonObject inputJson = ( JsonObject )jsonParser.parse( new String( base64Decoder.decode( theSegment ), utf8 ) );
			for( Entry<String,JsonElement> entry : inputJson.entrySet( ) ) {
				claimName = entry.getKey();
				claimValue = entry.getValue();
				claimDetails = this.claimHandlers.get( claimName );
				if( claimDetails != null ) {
					outputItems.put( claimName, claimDetails.getTypeReference().getFromJsonTranslator().translate( claimValue ) );
				} else if( claimValue.isJsonPrimitive( ) ) {
					JsonPrimitive primitiveJson = ( JsonPrimitive )claimValue;
					if( primitiveJson.isString( ) ) {
						outputItems.put( claimName, primitiveJson.getAsString( ) );
					} else if( primitiveJson.isNumber( ) ) {
						outputItems.put( claimName, primitiveJson.getAsNumber( ) );
					} else if( primitiveJson.isBoolean( ) ) {
						outputItems.put( claimName, primitiveJson.getAsBoolean( ) );
					} else {
						throw new IllegalArgumentException( String.format( "Claim '%s' is a primitive json type with value '%s', which has no mechanism for translation.", claimName, claimValue.getAsString() ) );	
					}
				} else {
					throw new IllegalArgumentException( String.format( "Claim '%s' is not a primitive json type with value '%s', which has no mechanism for translation.", claimName, claimValue.getAsString() ) );
				}
			}
		} catch( JsonParseException e ) {
			throw new IllegalArgumentException( String.format( "Segment '%d' contains invalid json.", theSegmentIndex ), e );
		} catch( TranslationException e ) {
			// claim name will be set if we have this exception, if not it will be null and will not cause a problem
			// but to be safe for the value, which should also be not null, we check so no exceptions are thrown
			if( claimValue != null ) {
				throw new IllegalArgumentException( String.format( "Claim '%s' in segment '%d' contains invalid data '%s'.", claimName, theSegmentIndex, claimValue.getAsString( ) ), e );
			} else {
				throw new IllegalArgumentException( String.format( "Claim '%s' in segment '%d' contains invalid data.", claimName, theSegmentIndex ), e );
			}
		}
		return outputItems;
	}
}
