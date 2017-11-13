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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.configuration.ConfigurationException;
import com.talvish.tales.system.configuration.annotated.Setting;
import com.talvish.tales.system.configuration.annotated.Settings;
import com.talvish.tales.system.configuration.annotated.SettingsName;
import com.talvish.tales.validation.OnValidation;


/**
 * The connector configuration class. This is based on configuration for Jetty that
 * can be largely found here:
 * http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java
 * @author jmolnar
 *
 */
@Settings( prefix="service.http_connectors" )
public class ConnectorConfiguration {
	private static List<String> SupportedProtocols;
	private static List<String> DefaultProtocols;

	static {
		// setups the the default protocols and list of support protocols
		SupportedProtocols = new ArrayList<String>( 2 );
		SupportedProtocols.add( "http1.1" );
		SupportedProtocols.add( "http2" );
		SupportedProtocols = Collections.unmodifiableList( SupportedProtocols );
		DefaultProtocols = SupportedProtocols;
	}

	@SettingsName
	private String name = "default";
	
	@Setting( name="{prefix}.{name}.acceptors" )
	private Integer acceptors; // ServerConnector( acceptors, selectors )
	
	@Setting( name="{prefix}.{name}.accept_queue_size" )
	private Integer acceptQueueSize; // ServerConnector.setAcceptQueueSize
	
	@Setting( name="{prefix}.{name}.selectors" )
	private Integer selectors;
	
	@Setting( name="{prefix}.{name}.idle_timeout" )
	private Integer idleTimeout; // ServerConnector.setIdleTimeout
	
	@Setting( name="{prefix}.{name}.header_cache_size" )
	private Integer headerCacheSize; 
	
	@Setting( name="{prefix}.{name}.request_header_size" )
	private Integer requestHeaderSize; 
	
	@Setting( name="{prefix}.{name}.response_header_size" )	
	private Integer responseHeaderSize; 
	
	@Setting( name="{prefix}.{name}.output_buffer_size" )
	private Integer outputBufferSize; 

	@Setting( name="{prefix}.{name}.max_form_content_size" )
	private Integer maxFormContentSize;

	@Setting( name="{prefix}.{name}.protocols" )
	private List<String> protocols = DefaultProtocols;

	// TODO: the HttpConnectionFactory in Jetty has an input buffer size, not sure if we need to set that somehow?
	// TODO: there is a reference to low resources here: http://www.eclipse.org/jetty/documentation/9.0.1.v20130408/limit-load.html
	//       more class details here: http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/LowResourceMonitor.html
	//       this is a class associated with the server object itself
	
	
	// TODO: additional items
	// - solingertime (ServerConnector)
	// - reuseaddress (ServerConnector)
	// - setinheritchannel (ServerConnector)

	/**
	 * Creates a default connector configuration.
	 */
	public ConnectorConfiguration( ) {
	}
	
	public ConnectorConfiguration( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name" );
		name = theName;
		// TODO: need to add the manual setters if we are going to allow these to be create manually (instead of just from the config manager)
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the acceptors
	 */
	public Integer getAcceptors() {
		return acceptors;
	}

	/**
	 * @return the acceptQueueSize
	 */
	public Integer getAcceptQueueSize() {
		return acceptQueueSize;
	}

	/**
	 * @return the acceptors
	 */
	public Integer getSelectors() {
		return selectors;
	}

	/**
	 * @return the maxIdleTime
	 */
	public Integer getIdleTimeout() {
		return idleTimeout;
	}

	/**
	 * @return the headerCacheSize
	 */
	public Integer getHeaderCacheSize() {
		return headerCacheSize;
	}

	/**
	 * @return the requestHeaderSize
	 */
	public Integer getRequestHeaderSize() {
		return requestHeaderSize;
	}

	/**
	 * @return the responseHeaderSize
	 */
	public Integer getResponseHeaderSize() {
		return responseHeaderSize;
	}

	/**
	 * @return the outputBufferSize
	 */
	public Integer getOutputBufferSize() {
		return outputBufferSize;
	}

	/**
	 * @return the maxContentFormSize
	 */
	public Integer getMaxFormContentSize() {
		return maxFormContentSize;
	}

	/**
	 * @return the protocols
	 */
	public List<String> getProtocols() {
		return protocols;
	}
	
	/**
	 * This is called to verify that the protocols.
	 */
	@OnValidation
	private void onValidation( ) {
		if( protocols == null || protocols.size() <=0 ) {
			throw new ConfigurationException( String.format( "Unable load connector settings '%s' since there no protocols are specified.", name ) );
		} else {
			for( String protocol : protocols ) {
				if( !SupportedProtocols.contains( protocol ) ) {
					throw new ConfigurationException( String.format( "Unable load connector settings '%s' due to unknown protocol '%s'.", name, protocol ) );
				}
			}
		}
	}
}
