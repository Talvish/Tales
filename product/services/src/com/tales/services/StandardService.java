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
package com.tales.services;

import com.google.common.base.Strings;
import com.tales.contracts.services.http.ResourceFacility;
import com.tales.serialization.json.JsonTranslationFacility;
import com.tales.system.configuration.PropertySource;

/**
 * This is a convenience class for someone looking to create a default service.
 * service.
 * @author jmolnar
 *
 */
public abstract class StandardService extends Service {
	/**
	 * Constructor taking the name of the service.
	 * @param theCanonicalName the canonical name of the service
	 * @param theFriendlyName a visual name for the service
	 * @param theDescription a description of the service
	 */
	protected StandardService( String theCanonicalName, String theFriendlyName, String theDescription ) {
		super( theCanonicalName, theFriendlyName, theDescription );
	}

	/**
	 * Convenience method for getting the JSON translation facility.
	 * @return the JSON translation facility
	 */
	public JsonTranslationFacility getJsonTranslationFacility( ) {
		return this.facilityManager.getFacility( JsonTranslationFacility.class );
	}

	/**
	 * Convenience method for getting the resource facility.
	 * @return the resource facility
	 */
	public ResourceFacility getResourceFacility( ) {
		return this.facilityManager.getFacility( ResourceFacility.class );
	}

	@Override
	protected void onInitializeConfiguration() {
		String filename = this.getConfigurationManager( ).getStringValue( "settings.file", null ); // get a config filename	 from command-line, if available
		
		if( !Strings.isNullOrEmpty( filename ) ) {
			this.getConfigurationManager( ).addSource( new PropertySource( filename) );
		}
	};
}
