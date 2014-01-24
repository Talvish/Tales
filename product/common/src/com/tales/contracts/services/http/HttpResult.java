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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Preconditions;

import com.tales.services.Status;
import com.tales.services.http.HeaderConstants;

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
	protected Status statusCode = Status.UNKNOWN;
	
	// failure items
	protected String failureSubcode;
	protected String failureMessage;
	protected Throwable failureException;
	
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
		
		DateTime dateTime = new DateTime( DateTimeZone.UTC );
		dateTime = dateTime.plusSeconds( theMaxAge );
		
		StringBuilder value = new  StringBuilder( HeaderConstants.CACHE_CONTROL_MAX_AGE_DIRECTIVE );
		value.append( "=" );
		value.append( theMaxAge );
		for( String option : theOptions ) {
			value.append( ", " );
			value.append( option );
		}
		headers.put( HeaderConstants.CACHE_CONTROL, value.toString( ) );
		headers.put( HeaderConstants.EXPIRES, RFC_1123_DATE_FORMATTER.print( dateTime ) );
		headers.remove( HeaderConstants.PRAGMA ); // just to be sure there is no conflict
	}

	// ******* below are shared success and failure items

	/**
	 * The HTTP headers to add to the result. 
	 * @return the headers to add
	 */
	public Map<String,String> getHeaders( ) {
		return this.headers;
	}
	
	/**
	 * The overall status code.
	 * @return The status code.
	 */
	public Status getStatusCode( ) {
		return statusCode;
	}
	
	// ******* below are failure items
	
	/**
	 * Indicates if the result is a failure.
	 * @return true if a failure, false otherwise
	 */
	public boolean failed( ) {
		return statusCode.isFailure( );
	}
	
	/**
	 * The failure subcode, which is a simple free-form
	 * value that the handler of the request can return.
	 * @return the failure subcode
	 */
	public String getFailureSubcode( ) {
		return failureSubcode;
	}
	
	/**
	 * An exception, which is non-null if an exception occurred.
	 * @return the failure exception or null
	 */
	public Throwable getFailureException( ) {
		return failureException;
	}
	
	/**
	 * The failure message to display.
	 * @return the failure message, which may also be null.
	 */
	public String getFailureMessage( ) {
		return failureMessage;
	}
}
