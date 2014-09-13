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
package com.tales.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.tales.communication.Status;
import com.tales.contracts.services.http.HttpContract;

/**
 * This is a servlet holder that has a contract
 * associated with a servlet and ensures that
 * callers use a version supported by the contract.
 * @author jmolnar
 *
 */
public class StrictContractServletHolder extends ContractServletHolder {
	private static final Logger logger = LoggerFactory.getLogger( ContractServletHolder.class );

	/**
	 * The constructor taking the contract and the servlet it is associated with.
	 * @param theContract the contract associated with the servlet
	 */
	public StrictContractServletHolder( HttpContract theContract, HttpInterfaceBase theInterface ) {
		super( theContract, theInterface );
	}

	/**
	 * This method will filter out requests that do not have proper contract versions.
	 * @param theHttpRequest the request being responded to
	 * @param theHttpResponse the response we can write out to
	 * @param theVersion the version of the contract being requested, which may be null/empty
	 * @return return true if filtered/handled, false otherwise
	 */
	protected boolean filterContract( HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse, String theVersion ) {
		boolean filtered = false;
		// NOTE: I would like to make the contract validation piece more sophisticated at some point
		//       where it could be managed by source location, something from the caller, certain 
		//       contracts are more lax, etc  ... even then it could be that a front-end could see 
		//       the missing version and at that time bind to the latest known good, so the servlets
		//       still require it, but front-end picks latest
		String version = theHttpRequest.getParameter( ParameterConstants.VERSION_PARAMETER );
		boolean missingVersion = Strings.isNullOrEmpty( version );
		
		// here we want to check if we have a service request
		// if we do, we want to grab the version parameter and validate
		// that based on the contract we have the right version here 
		// NOTE: in the future we may actually need to locally route based on the version of the contract
		if( missingVersion ) { // see if we have a version, but not the contract contract
			ResponseHelper.writeFailure( 
					theHttpRequest, 
					theHttpResponse, 
					Status.CALLER_BAD_INPUT,
					FailureSubcodes.VERSION_MISSING,
					"version must be specified",
					null );
			logger.warn( "A service request was made on contract '{}' but a version wasn't specified.", getContract( ).getName() );
			filtered = true;
		} else if( !getContract( ).supports( version ) ) { // see if the version is supported
			ResponseHelper.writeFailure( 
					theHttpRequest, 
					theHttpResponse, 
					Status.CALLER_BAD_INPUT, 
					FailureSubcodes.VERSION_NOT_SUPPORTED,
					String.format( "version '%s' is not supported", version ), 
					null );
			logger.warn( "A service request was made on contract '{}' asking for version '{}' which is not supported.", getContract( ).getName(), version );
			filtered = true;
		}
		return filtered;
	} 
}
