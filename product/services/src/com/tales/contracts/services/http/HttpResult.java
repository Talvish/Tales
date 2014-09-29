// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.tales.contracts.services.http;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Preconditions;
import com.tales.communication.HeaderConstants;
import com.tales.communication.Status;

/**
 * This class represents the result of an execution of a HTTP request.
 * It covers both successful and unsuccessful cases.
 * @author jmolnar
 *
 */
public abstract class HttpResult<T> {
	protected T value;
	
	// share items 
	protected Map<String,String> headers = new HashMap<String,String>( );
	protected Map<String,Cookie> cookies = new HashMap<String,Cookie>( );
	protected Status code = Status.UNKNOWN;
	protected String subcode;
	protected String message;
	protected Throwable exception;
	
	private static final String[] defaultCachingEnabledOptions = new String[] { HeaderConstants.CACHE_CONTROL_PUBLIC_DIRECTIVE };
	
	private static final DateTimeFormatter RFC_1123_DATE_FORMATTER = DateTimeFormat
			.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
			//.withLocale( Locale.US )
			//.withZone(DateTimeZone.UTC);
	
	// ******* below are success items

	/**
	 * The return value, which may be null if a failure case.
	 * @return the result value
	 */
	public T getValue( ) {
		return value;
	}

	/**
	 * A convenience method that sets caching related headers to indicate we
	 * don't want to cache.
	 */
	public void setCachingDisabled( ) {
		headers.put( HeaderConstants.CACHE_CONTROL, HeaderConstants.CACHE_CONTROL_DEFAULT_DIRECTIVE );
		headers.put( HeaderConstants.PRAGMA, HeaderConstants.PRAGMA_DEFAULT_DIRECTIVE );
		headers.put( HeaderConstants.EXPIRES, HeaderConstants.EXPIRES_DEFAULT_VALUE );	
	}

	/**
	 * A convenience method that sets the to enable caching up to a particular date.
	 * This method will indicate the result can be cached publicly as well.
	 * @param theMaxAge the maximum age of the result in seconds
	 */
	public void setCachingEnabled( int theMaxAge ) {
		Preconditions.checkArgument( theMaxAge >= 0, "need a max-age greater than or equal to zero" );
		setCachingEnabled( theMaxAge, defaultCachingEnabledOptions );
	}

	/**
	 * A convenience method that sets the to enable caching up to a particular date 
	 * and a particular set of options.
	 * @param theMaxAge the maximum age of the result in seconds
	 * @param theOptions the options to use for the caching
	 */
	public void setCachingEnabled( int theMaxAge, String[] theOptions ) {
		Preconditions.checkArgument( theMaxAge >= 0, "need a max-age greater than or equal to zero" );
		
		DateTime dateTimeNow = new DateTime( DateTimeZone.UTC );
		DateTime dateTimeExpired = dateTimeNow.plusSeconds( theMaxAge );
		
		StringBuilder value = new  StringBuilder( HeaderConstants.CACHE_CONTROL_MAX_AGE_DIRECTIVE );
		value.append( "=" );
		value.append( theMaxAge );
		for( String option : theOptions ) {
			value.append( ", " );
			value.append( option );
		}
		
		// we put the date header so relative time can be calculated and as outlined
		// for origin-servers according to RFC2616
		headers.put( HeaderConstants.DATE_HEADER, RFC_1123_DATE_FORMATTER.print( dateTimeNow ) );
		// now we put in HTTP 1.1 headers
		headers.put( HeaderConstants.CACHE_CONTROL, value.toString( ) );
		// and then the older HTTP 1.0 header, just in case
		headers.put( HeaderConstants.EXPIRES, RFC_1123_DATE_FORMATTER.print( dateTimeExpired ) );
		// and remove pragma, to ensure no conflict
		headers.remove( HeaderConstants.PRAGMA ); 
	}

	// ******* below are shared success and failure items

	/**
	 * The HTTP headers to add to the result. 
	 * @return the headers to add
	 */
	public Map<String,String> getHeaders( ) {
		return this.headers;
	}
	
	public Map<String,Cookie> getCookies( ) {
		return this.cookies;
	}
	
	/**
	 * The overall status code.
	 * @return The status code.
	 */
	public Status getCode( ) {
		return code;
	}
	
	/**
	 * The  subcode, which is a simple free-form
	 * value that the handler of the request can return.
	 * @return the subcode
	 */
	public String getSubcode( ) {
		return subcode;
	}
	
	/**
	 * An exception, which is non-null if an exception occurred.
	 * @return the failure exception or null
	 */
	public Throwable getException( ) {
		return exception;
	}
	
	/**
	 * The message to display.
	 * @return the failure message, which may also be null.
	 */
	public String getMessage( ) {
		return message;
	}
}
