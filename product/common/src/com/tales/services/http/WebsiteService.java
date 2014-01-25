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

/**
 * A convenience class for creating an http service that is just a website.
 * @author Joseph Molnar
 *
 */
public class WebsiteService extends HttpService {
	// TODO: this shouldnt' inherit off of httpservice (well, it doesn't have a resource associated,so has methods not needed)
	/**
	 * Constructor taking the name of the service.
	 * @param theCanonicalName the canonical name of the service
	 * @param theFriendlyName a visual name for the service
	 * @param theDescription a description of the service
	 */
	protected WebsiteService( String theCanonicalName, String theFriendlyName, String theDescription ) {
		super( theCanonicalName, theFriendlyName, theDescription );
	}

	/**
	 * Overrides onStart to setup the http interface for doing work.
	 */
	@Override
	protected void onStart() {
		// setup the work interface
        this.interfaceManager.register( new WebsiteInterface( ServiceConstants.WEBSITE_INTERFACE_NAME, "webapp", this ) );
	}

	/**
	 * Convenience method to get the public facing http interface.
	 * @return the public http interface
	 */
	public WebsiteInterface getWebsiteInterface( ) {
		return ( WebsiteInterface )this.interfaceManager.getInterface( ServiceConstants.WEBSITE_INTERFACE_NAME );
	}
}
