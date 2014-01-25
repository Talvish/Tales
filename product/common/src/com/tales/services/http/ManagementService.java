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
package com.tales.services.http;

/**
 * This is a convenience class for someone looking to create a management http-based
 * service. Management refers to a set of contracts that are used to augment a public
 * or internally facing interface. Where public and internal are typically high
 * load/scale handling interfaces, typically eventually consistent, the management
 * side is typically meant for low volume immediately consistent calls such as
 * back-end administration/management sites. 
 * @author jmolnar
 *
 */
public class ManagementService extends HttpService {
	/**
	 * Constructor taking the name of the service.
	 * @param theCanonicalName the canonical name of the service
	 * @param theFriendlyName a visual name for the service
	 * @param theDescription a description of the service
	 */
	protected ManagementService( String theCanonicalName, String theFriendlyName, String theDescription ) {
		super( theCanonicalName, theFriendlyName, theDescription );
	}

	/**
	 * Overrides onStart to setup the http interface for doing work.
	 */
	@Override
	protected void onStart() {
		// setup the work interface
        this.interfaceManager.register( new HttpInterface( ServiceConstants.MANAGEMENT_INTERFACE_NAME, this ) );
	}

	/**
	 * Convenience method to get the management http interface.
	 * @return the management http interface
	 */
	public HttpInterface getManagmentInterface( ) {
		return ( HttpInterface )this.interfaceManager.getInterface( ServiceConstants.MANAGEMENT_INTERFACE_NAME );
	}
}
