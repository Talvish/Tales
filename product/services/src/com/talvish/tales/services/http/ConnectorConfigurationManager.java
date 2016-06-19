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
package com.talvish.tales.services.http;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Facility;

/**
 * A simple manager that manages connector configurations stores used in the system.
 * @author jmolnar
 *
 */
public class ConnectorConfigurationManager implements Facility {
	private Map<String,ConnectorConfiguration> configurations = Collections.unmodifiableMap( new HashMap< String,ConnectorConfiguration >( ) );
	private Object lock = new Object();
	
	/**
	 * Returns all of the configurations
	 * @return the collection of configurations
	 */
	public Collection<ConnectorConfiguration> getConfigurations( ) {
		return configurations.values();
	}

	/**
	 * Retrieves a particular configuration
	 * @param theName the name of the configuration to retrieve
	 * @return the configuration if found, null otherwise
	 */
	public ConnectorConfiguration getConfiguration( String theName) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ) );
		return configurations.get( theName );
	}
	
	/**
	 * Registers a configuration with the manager. 
	 * If a configuration with this manager already exist then 
	 * an exception is thrown.
	 * @param theName the name to give the key store
	 * @param theConfiguration the connector configuration to register
	 */
	public void register( ConnectorConfiguration theConfiguration ) {
		Preconditions.checkNotNull( theConfiguration, "need a configuration to register" );

		synchronized( lock ) {
			if( configurations.containsKey( theConfiguration.getName( ) ) ) {
				throw new IllegalStateException( String.format( "Connector configuration with name '%s' is already registered.", theConfiguration.getName( ) ) );
			} else {
				Map<String,ConnectorConfiguration> newConfigurations = new HashMap<String, ConnectorConfiguration>( configurations );
				
				newConfigurations.put( theConfiguration.getName( ), theConfiguration );
				configurations = newConfigurations;
			}
		}
	}
}
