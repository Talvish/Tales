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
package com.tales.services.http;

import com.google.common.base.Preconditions;

/**
 * Configuration values for resources.
 * @author jmolnar
 *
 */
public class ResourceConfiguration {
	final static ResourceConfiguration DEFAULT_CONFIGURATION = new ResourceConfiguration( ThreadingConstants.DEFAULT_THREAD_POOL, ThreadingConstants.DEFAULT_RESOURCE_EXECUTION_TIMEOUT );
	
	private final String threadPoolName;
	private final Long executionTimeout;
	
	/**
	 * Constructor taking the thread pool name and execution time.
	 * @param theThreadPoolName the thread pool name for non-blocking operations, or null for default thread pool
	 * @param theExecutionTimeout the execution time, in milliseconds, to allow non-blocking to run for before timing out, or null for default time-out
	 */
	public ResourceConfiguration( String theThreadPoolName, Long theExecutionTimeout ) {
		Preconditions.checkArgument( theThreadPoolName == null || !theThreadPoolName.equals( "" ), "the resource configuration requires a thread pool name or null (to use the default)" );
		Preconditions.checkArgument( theExecutionTimeout == null || theExecutionTimeout >= 0, "the resource configuration requires an execution time greater that or equal to 0 or null (to use default)" );
		
		threadPoolName = theThreadPoolName;
		executionTimeout = theExecutionTimeout;
	}
	
	/**
	 * The thread pool name for non-blocking operations.
	 * @return the thread pool name, or null, if the default will be used
	 */
	public String getThreadPoolName( ) {
		return threadPoolName;
	}
	
	/**
	 * The execution time, in milliseconds, to allow non-blocking operations to run for before timing out.
	 * @return the allowed execution time, or null, if default will be used
	 */
	public Long getExecutionTimeout( ) {
		return executionTimeout;
	}
}
