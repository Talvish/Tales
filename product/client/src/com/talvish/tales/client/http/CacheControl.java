package com.talvish.tales.client.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.communication.HeaderConstants;

public class CacheControl {
	private static final Logger logger = LoggerFactory.getLogger( CacheControl.class );

	// tokens are based on spec: http://www.w3.org/Protocols/rfc2616/rfc2616-sec2.html 
	// all character changes are expressed in hex
	public static final String TOKEN_REGEX = "[\\x20-\\x7e&&[^()<>@,;:\\\\\"/\\[\\]?={} \\t]]+"; 

	public static final String QD_TEXT_REGEX = "[\\x20-\\x7e\\r\\n\\t \\x80-\\xff&&[^\"]]";
	public static final String QUOTED_PAIR_REGEX = "\\\\[\\x00-\\x7f]";
	public static final String QUOTED_STRING_REGEX ="\"(?:(?:" + QD_TEXT_REGEX + ")|(?:"+ QUOTED_PAIR_REGEX + "))*\"";
	
	
	public static final String QS_OR_T_REGEX = "(?:" + TOKEN_REGEX + ")|(?:" + QUOTED_STRING_REGEX + ")";
	
	public static final String CACHE_RESPONSE_DIRECTIVE_REGEX = "\\s*(" + QS_OR_T_REGEX + ")(?:\\s*=\\s*(" + QS_OR_T_REGEX + "))?\\s*";
	public static final Pattern CACHE_RESPONSE_DIRECTIVE_PATTERN = Pattern.compile( CACHE_RESPONSE_DIRECTIVE_REGEX );
	
	public static final String CACHE_RESPONSE_DIRECTIVES_REGEX = "^"+CACHE_RESPONSE_DIRECTIVE_REGEX + "(?:,\\s*" + CACHE_RESPONSE_DIRECTIVE_REGEX + ")*$";
	public static final Pattern CACHE_RESPONSE_DIRECTIVES_PATTERN = Pattern.compile( CACHE_RESPONSE_DIRECTIVES_REGEX );
	
	private static final int CACHE_DIRECTIVE_NAME_GROUP = 1;
	private static final int CACHE_DIRECTIVE_VALUE_GROUP = 2;
	
	// this is all based on RFC 2616 section 14.9

	private final boolean indicatedPublic;
	private final boolean indicatedPrivate;
	private final boolean indicatedNoCache;
	private final boolean indicatedNoStore;
	private final boolean indicatedNoTransform;
	private final boolean indicatedMustRevalidate;
	private final Integer maxAge;
	
	//private Map<String,String> cacheExtension;
	
	//TODO: Todd needs 416 support
	
	private CacheControl( 
			boolean indicatedPublic, 
			boolean indicatedPrivate, 
			boolean indicatedNoCache, 
			boolean indicatedNoStore, 
			boolean indicatedNoTransform, 
			boolean indicatedMustRevalidate, 
			Integer maxAge ) {
		this.indicatedPublic = indicatedPublic;
		this.indicatedPrivate = indicatedPrivate;
		this.indicatedNoCache = indicatedNoCache;
		this.indicatedNoStore = indicatedNoStore;
		this.indicatedNoTransform = indicatedNoTransform;
		this.indicatedMustRevalidate = indicatedMustRevalidate;
		this.maxAge = maxAge;
	}
	
	/**
	 * Indicates if the public directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedPublic( ) {
		return this.indicatedPublic;
	}
	
	/**
	 * Indicates if the private directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedPrivate( ) {
		return this.indicatedPrivate;
	}
	
	/**
	 * Indicates if the no-cache directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedNoCache( ) {
		return this.indicatedNoCache;
	}
	
	/**
	 * Indicates if the no-store directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedNoStore( ) {
		return this.indicatedNoStore;
	}
	
	/**
	 * Indicates if the no-transform directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedNoTransform( ) {
		return this.indicatedNoTransform;
	}
	
	/**
	 * Indicates if the must-revalidate directive was specified.
	 * @return indicates if the directive was specified or not
	 */
	public boolean indicatedMustRevalidated( ) {
		return this.indicatedMustRevalidate;
	}

	/**
	 * Gets the max age to use for caching.
	 * @return the max age to use for caching, or null if it was not specified
	 */
	public Integer getMaxAge( ) {
		return maxAge;
	}
	
	/**
	 * A method that process the value of the Cache-Control header
	 * to create a class with the result.
	 * @param theHeaderValue the value of the Cache-Control header
	 * @return the representation of the Cache-Control header
	 */
	public static CacheControl create( String theHeader ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theHeader ), "need a header to create the cache control information" );
		
		Matcher matcher = CACHE_RESPONSE_DIRECTIVE_PATTERN.matcher( theHeader );
		String name;
		String value;
		
		boolean indicatedPublic = false;
		boolean indicatedPrivate = false;
		boolean indicatedNoCache = false;		
		boolean indicatedNoStore = false;
		boolean indicatedNoTransform = false;
		boolean indicatedMustRevalidate = false;
		Integer maxAge = null;
		
		while( matcher.find( ) ) {
			// find the values we need
			name = matcher.group( CACHE_DIRECTIVE_NAME_GROUP );
			value = matcher.group( CACHE_DIRECTIVE_VALUE_GROUP );
			
			logger.info( "Found cache directive '{}' with value '{}'.", name, value );
			switch( name ) {
			case HeaderConstants.CACHE_CONTROL_PUBLIC_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by HTTP).", name, value );
				}
				indicatedPublic = true;
				break;
			case HeaderConstants.CACHE_CONTROL_PRIVATE_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by Tales).", name, value );
				}
				indicatedPrivate = true;
				break;
			case HeaderConstants.CACHE_CONTROL_NO_CACHE_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by Tales).", name, value );
				}
				indicatedNoCache = true;
				break;
			
			case HeaderConstants.CACHE_CONTROL_NO_STORE_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by HTTP).", name, value );
				}
				indicatedNoStore = true;
				break;
			case HeaderConstants.CACHE_CONTROL_NO_TRANSFORM_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by HTTP).", name, value );
				}
				indicatedNoTransform = true;
				break;
			case HeaderConstants.CACHE_CONTROL_MUST_REVALIDATE_DIRECTIVE:
				if( value != null ) {
					logger.warn( "Ignoring cache directive '{}' qualification '{}' (not supported by HTTP).", name, value );
				}
				indicatedMustRevalidate = true;
				break;
			case HeaderConstants.CACHE_CONTROL_MAX_AGE_DIRECTIVE:
				if( value != null ) {
					try {
						maxAge = Integer.parseInt( value );
					} catch( NumberFormatException e ) {
						// absorbing since we don't want to stop
						// the system from working, we will log 
						// the error
					}
				}
				if( maxAge < 0 ) {
					logger.warn( "Ignoring cache directive '{}' due to invalid delta-seconds '{}'.", name, value );
					maxAge = null;
				}
				break;
				
			default:
				logger.warn( "Ignoring unsupported Tales cache directive '{}' with value = '{}'.", name, value );
			}
		}
		
		return new CacheControl( indicatedPublic, indicatedPrivate, indicatedNoCache, indicatedNoStore, indicatedNoTransform, indicatedMustRevalidate, maxAge );
	}
}