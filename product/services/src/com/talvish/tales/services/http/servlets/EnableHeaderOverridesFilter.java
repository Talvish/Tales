// ***************************************************************************
// *  Copyright 2013 Joseph Molnar
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
package com.talvish.tales.services.http.servlets;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.talvish.tales.services.http.AttributeConstants;

/**
 * This is a filter, that if used by default, will enable header overrides.
 * This filter can be used as a base class to do dynamic checks to see if
 * overriding should be allowed. 
 * @author jmolnar
 *
 */
public class EnableHeaderOverridesFilter implements Filter {
	private static final Object enabled = new Object( );

	@Override
	public void destroy() {
		// this doesn't to do  need anything
	}

	@Override
	public void doFilter(
			ServletRequest theRequest, 
			ServletResponse theResponse, 
			FilterChain theFilterChain ) throws IOException, ServletException {
		if( shouldOverride( theRequest, theResponse ) ) {
			theRequest.setAttribute( AttributeConstants.ENABLE_HEADER_OVERRIDES, enabled );
		}
		theFilterChain.doFilter( theRequest, theResponse );
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// this doesn't to do  need anything
	}

	/**
	 * The method called to see if overrides should be enabled.
	 * A subclass can override this to use whatever logic it wants
	 * (e.g. configuration, or check the calling user, etc). 
	 * @param theRequest
	 * @return return true if overrides are supported, false otherwise
	 */
	protected boolean shouldOverride( ServletRequest theRequest, ServletResponse theResponse ) {
		return true;
	}
}
