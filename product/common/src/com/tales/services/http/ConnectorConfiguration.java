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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.services.ConfigurationConstants;
import com.tales.system.configuration.ConfigurationManager;

public class ConnectorConfiguration {
	private final String name;
	
	private final Integer acceptors; // ServerConnector( acceptors, selectors )
	private final Integer acceptQueueSize; // ServerConnector.setAcceptQueueSize
	
	private final Integer selectors;
	
	private final Integer idleTimeout; // ServerConnector.setIdleTimeout
	
	private final Integer headerCacheSize; 
	private final Integer requestHeaderSize; 
	private final Integer responseHeaderSize; 
	private final Integer outputBufferSize; 

	private final Integer maxFormContentSize; // TODO: ?
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
		name = "default";
		acceptors = null;
		acceptQueueSize = null;
		selectors = null;
		idleTimeout = null;
		headerCacheSize = null;
		requestHeaderSize = null;
		responseHeaderSize = null;
		outputBufferSize = null;
		maxFormContentSize = null;
	}
	
	public ConnectorConfiguration( String theName, ConfigurationManager theConfigurationManager ) {
		// http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java
			
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name" );
		Preconditions.checkNotNull( theConfigurationManager, "need a configuration manager" );
		
		name = theName;
		
		acceptors = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_ACCEPTORS, theName ), null );
		acceptQueueSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_ACCEPT_QUEUE_SIZE, theName ), null );
		
		selectors = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_SELECTORS, theName ), null );

		idleTimeout = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_IDLE_TIMEOUT, theName ), null );

		headerCacheSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_HEADER_CACHE_SIZE, theName ), null );
		requestHeaderSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_REQUEST_HEADER_SIZE, theName ), null );
		responseHeaderSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_RESPONSE_HEADER_SIZE, theName ), null );
		outputBufferSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_OUTPUT_BUFFER_SIZE, theName ), null );

		maxFormContentSize = theConfigurationManager.getIntegerValue( String.format( ConfigurationConstants.HTTP_CONNECTORS_MAX_FORM_CONTENT_SIZE, theName ), null );
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
}
