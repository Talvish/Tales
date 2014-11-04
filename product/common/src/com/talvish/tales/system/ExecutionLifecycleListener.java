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
package com.talvish.tales.system;

/**
 * An interface that allows an object to listen to 
 * the changes in execution state of another object.
 * @author jmolnar
 *
 */
public interface ExecutionLifecycleListener {
	/**
	 * Method called when the item is created.
	 * @param theItem the item that was created.
	 */
	void onCreated( Object theItem );
	/**
	 * Method called when the item is starting up.
	 * @param theItem the item that is starting.
	 * @param thePreviousState the previous state the item was in
	 */
	void onStarting( Object theItem, ExecutionLifecycleState thePreviousState );
	/**
	 * Method called when the item has started.
	 * @param theItem the item that has started.
	 * @param thePreviousState the previous state the item was in
	 */
	void onStarted( Object theItem, ExecutionLifecycleState thePreviousState );
	/**
	 * Method called when the item is running.
	 * @param theItem the item that is now running.
	 * @param thePreviousState the previous state the item was in
	 */
	void onRunning( Object theItem, ExecutionLifecycleState thePreviousState );
	/**
	 * Method called when the item is stopping.
	 * @param theItem the item that is stopping.
	 * @param thePreviousState the previous state the item was in
	 */
	void onStopping( Object theItem, ExecutionLifecycleState thePreviousState );
	/**
	 * Method called when the item was stopped.
	 * @param theItem the item that was stopped.
	 * @param thePreviousState the previous state the item was in
	 */
	void onStopped( Object theItem, ExecutionLifecycleState thePreviousState );
	/**
	 * Method called when the item was suspended.
	 * @param theItem the item that was suspended.
	 * @param thePreviousState the previous state the item was in
	 */
	void onSuspended( Object theItem, ExecutionLifecycleState thePreviousState );
}
