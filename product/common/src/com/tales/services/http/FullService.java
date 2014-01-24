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

import java.util.List;

import com.tales.services.ConfigurationConstants;

/**
 * This is a convenience class for someone looking to create an http-based
 * service with public, internal and management service.
 * @author jmolnar
 *
 */
public class FullService extends HttpService {
	/**
	 * Constructor taking the name of the service.
	 * @param theCanonicalName the canonical name of the service
	 * @param theFriendlyName a visual name for the service
	 * @param theDescription a description of the service
	 */
	protected FullService( String theCanonicalName, String theFriendlyName, String theDescription ) {
		super( theCanonicalName, theFriendlyName, theDescription );
	}

	/**
	 * Overrides onStart to setup the http interface for doing work.
	 */
	@Override
	protected void onStart() {
		// setup the work interfaces
		List<String> endPoints;
		HttpInterfaceBase httpInterface;
		
		endPoints = getConfigurationManager( ).getListValue( String.format( ConfigurationConstants.HTTP_INTERFACE_ENDPOINTS, ServiceConstants.PUBLIC_INTERFACE_NAME ), String.class );
        httpInterface = new HttpInterface( ServiceConstants.PUBLIC_INTERFACE_NAME, endPoints, this );
        this.interfaceManager.register( httpInterface );

		endPoints = getConfigurationManager( ).getListValue( String.format( ConfigurationConstants.HTTP_INTERFACE_ENDPOINTS, ServiceConstants.INTERNAL_INTERFACE_NAME ), String.class );
        httpInterface = new HttpInterface( ServiceConstants.INTERNAL_INTERFACE_NAME, endPoints, this );
        this.interfaceManager.register( httpInterface );

		endPoints = getConfigurationManager( ).getListValue( String.format( ConfigurationConstants.HTTP_INTERFACE_ENDPOINTS, ServiceConstants.MANAGEMENT_INTERFACE_NAME ), String.class );
        httpInterface = new HttpInterface( ServiceConstants.MANAGEMENT_INTERFACE_NAME, endPoints, this );
        this.interfaceManager.register( httpInterface );
    }

	/**
	 * Convenience method to get the public facing http interface.
	 * @return the public http interface
	 */
	public HttpInterface getPublicInterface( ) {
		return ( HttpInterface )this.interfaceManager.getInterface( ServiceConstants.PUBLIC_INTERFACE_NAME );
	}
	
	/**
	 * Convenience method to get the internal http interface.
	 * @return the internal http interface
	 */
	public HttpInterface getInternalInterface( ) {
		return ( HttpInterface )this.interfaceManager.getInterface( ServiceConstants.INTERNAL_INTERFACE_NAME );
	}
	
	/**
	 * Convenience method to get the management http interface.
	 * @return the management http interface
	 */
	public HttpInterface getManagmentInterface( ) {
		return ( HttpInterface )this.interfaceManager.getInterface( ServiceConstants.MANAGEMENT_INTERFACE_NAME );
	}

}
