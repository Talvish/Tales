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
package com.talvish.tales.services.http;

import com.google.common.base.Preconditions;

/**
 * Configuration values for resources.
 * @author jmolnar
 *
 */
// TODO: consider updating to use new config
public class ResourceConfiguration {
	// this is the default configuration
	final static ResourceConfiguration DEFAULT_CONFIGURATION = new ResourceConfiguration( )
		.setThreadPoolName( ThreadingConstants.DEFAULT_THREAD_POOL )
		.setExecutionTimeout( ThreadingConstants.DEFAULT_RESOURCE_EXECUTION_TIMEOUT );
	
	private String threadPoolName;
	private Long executionTimeout;
	
	/**
	 * Standard default constructor.
	 */
	public ResourceConfiguration( ) {
	}
	
	/**
	 * The thread pool name for non-blocking operations.
	 * @return the thread pool name, or null, if the default will be used
	 */
	public String getThreadPoolName( ) {
		return threadPoolName;
	}

	/**
	 * Set the thread pool name for non-blocking operations.
	 * @param theThreadPoolName the thread pool name, or null, if the default will be used
	 * @return returns this configuration object so things can be chained together
	 */
	public ResourceConfiguration setThreadPoolName( String theThreadPoolName ) {
		Preconditions.checkArgument( theThreadPoolName == null || !theThreadPoolName.equals( "" ), "the resource configuration requires a thread pool name or null (to use the default)" );
		threadPoolName = theThreadPoolName;
		return this;
	}

	/**
	 * The execution time, in milliseconds, to allow non-blocking operations to run for before timing out.
	 * @return the allowed execution time, or null, if default will be used
	 */
	public Long getExecutionTimeout( ) {
		return executionTimeout;
	}

	/**
	 * The execution time, in milliseconds, to allow non-blocking operations to run for before timing out.
	 * @return the allowed execution time, or null, if default will be used
	 */
	public ResourceConfiguration setExecutionTimeout( Long theExecutionTimeout ) {
		Preconditions.checkArgument( theExecutionTimeout == null || theExecutionTimeout >= 0, "the resource configuration requires an execution time greater that or equal to 0 or null (to use default)" );
		executionTimeout = theExecutionTimeout;
		return this;
	}	
}
