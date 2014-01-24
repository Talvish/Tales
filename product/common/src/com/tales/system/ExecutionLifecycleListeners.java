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
package com.tales.system;

import com.tales.parts.ListenerManager;

/**
 * A utility class that is a {@link ListenerManager} that implements the {@link ExecutionLifecycleListener} interface.
 * @author jmolnar
 *
 */
public class ExecutionLifecycleListeners extends ListenerManager<ExecutionLifecycleListener> implements ExecutionLifecycleListener {

	/**
	 * Method called when the item is created.
	 * This will send to all registered listeners.
	 * @param theItem the item that was created.
	 */
	@Override
	public void onCreated(Object theItem) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onCreated( theItem );
		}
	}

	/**
	 * Method called when the item is starting up.
	 * This will send to all registered listeners.
	 * @param theItem the item that is starting.
	 * @param thePreviousState the previous state the item was in
	 */
	@Override
	public void onStarting(Object theItem, ExecutionLifecycleState thePreviousState) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onStarting( theItem, thePreviousState );
		}
	}
	/**
	 * Method called when the item has started.
	 * @param theItem the item that has started.
	 * @param thePreviousState the previous state the item was in
	 */
	public void onStarted( Object theItem, ExecutionLifecycleState thePreviousState ) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onStarted( theItem, thePreviousState );
		}
	}
	/**
	 * Method called when the item is running.
	 * This will send to all registered listeners.
	 * @param theItem the item that is now running.
	 * @param thePreviousState the previous state the item was in
	 */
	@Override
	public void onRunning(Object theItem, ExecutionLifecycleState thePreviousState) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onRunning( theItem, thePreviousState );
		}
	}
	/**
	 * Method called when the item is stopping.
	 * This will send to all registered listeners.
	 * @param theItem the item that is stopping.
	 * @param thePreviousState the previous state the item was in
	 */
	@Override
	public void onStopping(Object theItem, ExecutionLifecycleState thePreviousState) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onStopping( theItem, thePreviousState );
		}
	}

	/**
	 * Method called when the item was stopped. 
	 * This will send to all registered listeners.
	 * @param theItem the item that was stopped.
	 * @param thePreviousState the previous state the item was in
	 */
	@Override
	public void onStopped(Object theItem, ExecutionLifecycleState thePreviousState) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onStopped( theItem, thePreviousState );
		}
	}

	/**
	 * Method called when the item was suspended.
	 * This will send to all registered listeners.
	 * @param theItem the item that was suspended.
	 * @param thePreviousState the previous state the item was in
	 */
	@Override
	public void onSuspended(Object theItem, ExecutionLifecycleState thePreviousState) {
		for( ExecutionLifecycleListener listener : this.getListeners() ) {
			listener.onSuspended( theItem, thePreviousState );
		}
	}
}
