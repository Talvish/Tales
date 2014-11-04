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
package com.talvish.tales.communication;

/**
 * HTTP header names and values commonly used. 
 * Current set concentrate on caching headers based on RFC 2616 
 * (see: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html)
 * @author jmolnar
 *
 */
public class HeaderConstants {
	public static final String USER_AGENT_HEADER = "User-Agent";
	public static final String ROOT_REQUEST_ID_HEADER = "Root-Request-Id";
	public static final String PARENT_REQUEST_ID_HEADER = "Parent-Request-Id";
	
	// TODO: if we set the cache-control headers we shoudl also be sending the date field
	//		 see: http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
	public static final String DATE_HEADER = "DATE";

	// the following below are cache related headers
	public static final String CACHE_CONTROL = "Cache-Control";
	// client/server shared names/values
	public static final String CACHE_CONTROL_NO_CACHE_DIRECTIVE = "no-cache";
	public static final String CACHE_CONTROL_NO_STORE_DIRECTIVE = "no-store";
	public static final String CACHE_CONTROL_NO_TRANSFORM_DIRECTIVE = "no-transform";
	public static final String CACHE_CONTROL_MAX_AGE_DIRECTIVE = "max-age";
	
	// server only names/values 
	public static final String CACHE_CONTROL_PUBLIC_DIRECTIVE = "public";
	public static final String CACHE_CONTROL_PRIVATE_DIRECTIVE = "private";
	public static final String CACHE_CONTROL_MUST_REVALIDATE_DIRECTIVE = "must-revalidate";
	public static final String CACHE_CONTROL_PROXY_REVALIDATE_DIRECTIVE = "proxy-revalidate";
	public static final String CACHE_CONTROL_S_MAXAGE_DIRECTIVE = "s-maxage";

	// client only names/value
	public static final String CACHE_CONTROL_MAX_STALE_DIRECTIVE = "max-stale";
	public static final String CACHE_CONTROL_MIN_FRESH_DIRECTIVE = "min-fresh";
	public static final String CACHE_CONTROL_ONLY_IF_CACHED_DIRECTIVE = "only-if-cached";

	public static final String CACHE_CONTROL_DEFAULT_DIRECTIVE = 
			CACHE_CONTROL_PRIVATE_DIRECTIVE + ", " +
			CACHE_CONTROL_NO_CACHE_DIRECTIVE + ", " +
			CACHE_CONTROL_NO_STORE_DIRECTIVE + ", " +
			CACHE_CONTROL_NO_TRANSFORM_DIRECTIVE + ", " +
			CACHE_CONTROL_MUST_REVALIDATE_DIRECTIVE;			
	
	public static final String EXPIRES = "Expires";
	public static final String EXPIRES_DEFAULT_VALUE = "Sat, 01 Jan 2000 00:00:00 GMT";
	
	public static final String PRAGMA = "Pragma";
	public static final String PRAGMA_NO_CACHE_DIRECTIVE = "no-cache";
	public static final String PRAGMA_DEFAULT_DIRECTIVE = PRAGMA_NO_CACHE_DIRECTIVE;
}
