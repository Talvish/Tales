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

/**
 * Configuration constants related to thread pools and threading. 
 * @author jmolnar
 *
 */
public final class ThreadingConstants {
	public static final String DEFAULT_THREAD_POOL = "default";
	public static final long DEFAULT_RESOURCE_EXECUTION_TIMEOUT = 10000;
	
	public static final int DEFAULT_CORE_THREADS_FACTOR = 10;
	public static final int DEFAULT_MAX_THREAD_FACTOR = 2;
	public static final long DEFAULT_KEEP_ALIVE_TIME = 60000l;
	public static final boolean DEFAULT_PRESTART_CORE = false;
	public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY;
	public static final boolean DEFAULT_IS_DAEMON = false;
}
