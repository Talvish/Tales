// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.system.ExecutionLifecycleListener;
import com.tales.system.ExecutionLifecycleListeners;
import com.tales.system.ExecutionLifecycleState;

/**
 * A utility class that manages the interfaces used by a service. 
 * @author jmolnar
 *
 */
public class InterfaceManager {
	private Map<String, Interface> interfaces 	= new HashMap<String, Interface>( 0 );
	private ExecutionLifecycleState lifecycleState		= ExecutionLifecycleState.CREATED;
	private final Object interfacesLock 		= new Object( );
	private final ExecutionLifecycleListeners listeners	= new ExecutionLifecycleListeners( ); 
	
	/**
	 * Returns the interface with the specified name.
	 * @param theName the name of the interface to get
	 * @return the interface with the specified name or {@code null} if not found
	 */
	public Interface getInterface( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name of the interface");
		return this.interfaces.get( theName );
	}
	
	/**
	 * Returns the interfaces exposed by the service.
	 * @return the list of interfaces
	 */
	public Collection<Interface> getInterfaces( ) {
		return this.interfaces.values();
	}
	
	/**
	 * Adds an object interested in getting lifecycle state updates.
	 * @param theListener the listener to add
	 */
	public void addListener( ExecutionLifecycleListener theListener ) {
		listeners.addListener( theListener );
	}
	
	/**
	 * Removes an object that was once interested in getting lifecycle state updates.
	 * @param theListener the listener to remove
	 */
	public void removeListener( ExecutionLifecycleListener theListener ) {
		listeners.removeListener( theListener );
	}
	
	/**
	 * Starts all of the interfaces.
	 */
	public void start( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED, "Cannot start the interfaces when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STARTING;
		this.listeners.onStarting( this, this.lifecycleState );
		for( Interface serviceInterface: interfaces.values() ) {
			serviceInterface.start();
		}
		this.lifecycleState = ExecutionLifecycleState.STARTED;
		this.listeners.onStarted( this, this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Stops all the interfaces.
	 */
	public void stop( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED || this.lifecycleState == ExecutionLifecycleState.RUNNING || this.lifecycleState == ExecutionLifecycleState.SUSPENDED, "Cannot stop the interfaces when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STOPPING;
		this.listeners.onStopping( this, this.lifecycleState );
		for( Interface serviceInterface: interfaces.values() ) {
			serviceInterface.stop();
		}
		this.lifecycleState = ExecutionLifecycleState.STOPPED;
		this.listeners.onStopped( this, this.lifecycleState );
	}
	
	/**
	 * This is call to suspend all associated interfaces except the admin interface.
	 */
	public void suspend( ) {
		Preconditions.checkState( canSuspend( ), "Cannot suspend when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.SUSPENDED;
		this.listeners.onSuspended( this, this.lifecycleState );
		for( Interface serviceInterface: interfaces.values() ) {
			if( !serviceInterface.getName().equals( "admin" ) && serviceInterface.canSuspend( ) ) {
				serviceInterface.suspend();
			}
		}
	}
	
	/**
	 * This is called to resume the previously suspended interfaces.
	 */
	public void resume( ) {
		Preconditions.checkState( canResume( ), "Cannot resume when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
		for( Interface serviceInterface: interfaces.values() ) {
			if( serviceInterface.canResume( ) ) {
				serviceInterface.resume();
			}
		}
	}
	
	/**
	 * Helper method that indicates if the manager is in a state that will
	 * allow suspending.
	 * @return return true if suspendable, false otherwise
	 */
	public boolean canSuspend( ) {
		return this.lifecycleState == ExecutionLifecycleState.RUNNING;
	}
	
	/**
	 * Helper method that indicates if the manager is suspended so that
	 * it can be resumed.
	 * @return return true if resumable, false otherwise
	 */
	public boolean canResume( ) {
		return this.lifecycleState == ExecutionLifecycleState.SUSPENDED;
	}
	
	/**
	 * Registers an interface.
	 * @param theInterface the interface to register
	 */
	public void register( Interface theInterface ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED || this.lifecycleState == ExecutionLifecycleState.STARTING, "Cannot register an interface when the status is '%s'.", this.lifecycleState );
		Preconditions.checkNotNull( theInterface );
		synchronized( this.interfacesLock ) {
			Preconditions.checkArgument( !this.interfaces.containsKey( theInterface.getName( ) ), String.format( "An interface named '%s' already exists.", theInterface.getName( ) ) );

			HashMap<String, Interface> newInterfaces = new HashMap<String,Interface>( this.interfaces );
			newInterfaces.put( theInterface.getName(), theInterface );
			interfaces = Collections.unmodifiableMap( newInterfaces );
		}
	}
}
