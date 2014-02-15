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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
	//       can and need to do work prior to others, think queue serivces where you want to
	//       absorbed 
	
	private List<Interface> interfaceList				= new ArrayList<Interface>( 0 );
	private Map<String, Interface> interfaceMap			= new HashMap<String, Interface>( 0 );
	private ExecutionLifecycleState lifecycleState		= ExecutionLifecycleState.CREATED;
	private final Object interfacesLock 				= new Object( );
	private final ExecutionLifecycleListeners listeners	= new ExecutionLifecycleListeners( ); 
	
	/**
	 * Returns the interface with the specified name.
	 * @param theName the name of the interface to get
	 * @return the interface with the specified name or {@code null} if not found
	 */
	public Interface getInterface( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name of the interface");
		return this.interfaceMap.get( theName );
	}
	
	/**
	 * Returns the interfaces exposed by the service.
	 * @return the list of interfaces
	 */
	public Collection<Interface> getInterfaces( ) {
		return interfaceList;
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
	 * Starts all of the interfaces in order they were added.
	 */
	public void start( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED, "Cannot start the interfaces when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STARTING;
		this.listeners.onStarting( this, this.lifecycleState );
		for( Interface serviceInterface: interfaceList ) {
			serviceInterface.start();
		}
		this.lifecycleState = ExecutionLifecycleState.STARTED;
		this.listeners.onStarted( this, this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Stops all the interfaces in the opposite order from how they were added.
	 */
	public void stop( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED || this.lifecycleState == ExecutionLifecycleState.RUNNING || this.lifecycleState == ExecutionLifecycleState.SUSPENDED, "Cannot stop the interfaces when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STOPPING;
		this.listeners.onStopping( this, this.lifecycleState );
		List<Interface> serviceInterfaces = this.interfaceList;
		for( int index = serviceInterfaces.size() - 1; index >= 0; index -= 1 ) {
			Interface serviceInterface = serviceInterfaces.get( index );
			serviceInterface.stop();
		}
		this.lifecycleState = ExecutionLifecycleState.STOPPED;
		this.listeners.onStopped( this, this.lifecycleState );
	}
	
	/**
	 * This is call to suspend all associated interfaces except the admin interface.
	 * They are called in the opposite order they were added.
	 */
	public void suspend( ) {
		Preconditions.checkState( canSuspend( ), "Cannot suspend when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.SUSPENDED;
		this.listeners.onSuspended( this, this.lifecycleState );
		List<Interface> serviceInterfaces = this.interfaceList;
		for( int index = serviceInterfaces.size() - 1; index >= 0; index -= 1 ) {
			Interface serviceInterface = serviceInterfaces.get( index );
			if( !serviceInterface.getName().equals( "admin" ) && serviceInterface.canSuspend( ) ) {
				serviceInterface.suspend();
			}
		}
	}
	
	/**
	 * This is called to resume the previously suspended interfaces.
	 * They are called in the order they were added.
	 */
	public void resume( ) {
		Preconditions.checkState( canResume( ), "Cannot resume when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
		for( Interface serviceInterface: interfaceList ) {
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
			Preconditions.checkArgument( !this.interfaceMap.containsKey( theInterface.getName( ) ), String.format( "An interface named '%s' already exists.", theInterface.getName( ) ) );

			HashMap<String, Interface> newInterfaceMap = new HashMap<>( this.interfaceMap );
			newInterfaceMap.put( theInterface.getName(), theInterface );
			interfaceMap = Collections.unmodifiableMap( newInterfaceMap );
			
			List<Interface> newInterfaceList = new ArrayList<>( this.interfaceList );
			newInterfaceList.add( theInterface );
			interfaceList = Collections.unmodifiableList( newInterfaceList );
		}
	}
}
