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
package com.tales.system;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A factory, similar to the DefaultThreadFactory, but allowing to set parameters
 * to use for the creation of threads.
 * @author jmolnar
 *
 */
public class ConfigurableThreadFactory implements ThreadFactory {
	private final ThreadGroup threadGroup;
	private final static AtomicInteger lastPoolNumber = new AtomicInteger( 1 );
	private final AtomicInteger lastThreadNumber = new AtomicInteger( 1 );
	private final String threadNamePrefix;
	
	
	private final int defaultPriority;
	private final boolean defaultDaemon;

	/**
	 * Constructor for the thread factory that will use a "tp" name prefix
	 * normal default priority and is not a default daemon. 
	 */
	public ConfigurableThreadFactory( ) {
		this( "default", Thread.NORM_PRIORITY, false );
	}
	
	/**
	 * Constructor for the thread factory that will use 
	 * normal default priority and is not a default daemon. 
	 * @param theNamePrefix the base prefix to use
	 */
	public ConfigurableThreadFactory( String theNamePrefix ) {
		this( theNamePrefix, Thread.NORM_PRIORITY, false );
	}
	
	/**
	 * Constructor for the thread factory taking all parameters that can be set.
	 * @param theName the base prefix to use
	 * @param defaultPriority the priority to use for creating threads
	 * @param producesDaemon indicates if the threads created are daemon threads
	 */
	public ConfigurableThreadFactory( String theNamePrefix, int theDefaultPriority, boolean isDefaultDaemon ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theNamePrefix ), "need a name prefix to use for thread creation" );
		Preconditions.checkArgument( theDefaultPriority >= Thread.MIN_PRIORITY && theDefaultPriority <= Thread.MAX_PRIORITY, "the priority '%s' is not within range for thread factory with prefix '%s'", theDefaultPriority, theNamePrefix );
		
		SecurityManager securityManager = System.getSecurityManager( );
		if( securityManager != null ) {
			threadGroup = securityManager.getThreadGroup( );
		} else {
			threadGroup = Thread.currentThread( ).getThreadGroup();
		}
		threadNamePrefix = String.format( "%s_p%s_t", theNamePrefix, lastPoolNumber.getAndIncrement( ) );
		defaultPriority = theDefaultPriority;
		defaultDaemon = isDefaultDaemon;
	}

	/**
	 * Creates and returns a thread to run the Runnable.
	 */
	@Override
	public Thread newThread( Runnable theRunnable ) {
		Thread thread = new Thread( 
				this.threadGroup, 
				theRunnable, 
				this.threadNamePrefix + lastThreadNumber.getAndIncrement(), 
				0 );

		// reset daemon to expectations of the factor
		if( thread.isDaemon() != defaultDaemon ) {
			thread.setDaemon( defaultDaemon );
		}

		// reset priority to expectations of the factory
		if( thread.getPriority() != defaultPriority ) {
			thread.setPriority( defaultPriority );
		}
		
		return thread;
	}
}
