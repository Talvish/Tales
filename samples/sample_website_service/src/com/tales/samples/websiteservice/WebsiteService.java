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
package com.tales.samples.websiteservice;

import java.util.HashMap;

import com.google.common.base.Strings;
import com.tales.serialization.Readability;
import com.tales.services.OperationContext.Details;
import com.tales.services.http.HttpService;
import com.tales.services.http.ServiceConstants;
import com.tales.services.http.WebsiteInterface;
import com.tales.system.configuration.PropertySource;

/**
 * A simple website running as a service.
 * @author Joseph Molnar
 *
 */
public class WebsiteService extends HttpService {

	protected WebsiteService( ) {
		super( "website_service", "Website Service", "A public tales service show a very simple website calling a service." );
	}
	
	@Override
	protected void onInitializeConfiguration() {
		String filename = this.getConfigurationManager( ).getStringValue( "settings.file", null ); // get a config filename	 from command-line, if available
		
		if( !Strings.isNullOrEmpty( filename ) ) {
			this.getConfigurationManager( ).addSource( new PropertySource( filename) );
		}
	};
	
	@Override
	protected void onStart() {
		super.onStart();
		
		HashMap<String,String> jspInitParameters = new HashMap<String, String>( );
		jspInitParameters.put( "keepgenerated", "TRUE" );

		WebsiteInterface siteInterface = new WebsiteInterface( ServiceConstants.PUBLIC_INTERFACE_NAME, "website", jspInitParameters, this );
		
		this.interfaceManager.register( siteInterface );
		
		siteInterface.setDefaultResponseDetails( Details.ALL );
		siteInterface.setDefaultResponseReadability( Readability.HUMAN );
		siteInterface.bind( new SimpleResource( ), "/simple_contract" );
	}
	
    public static void main( String[ ] args ) throws Exception {
    	WebsiteService service = new WebsiteService( );
    	
    	service.start( args );
    	service.run( );
    	service.stop( );
	}
}
