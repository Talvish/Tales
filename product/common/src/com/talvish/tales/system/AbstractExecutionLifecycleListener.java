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
 * An implementation of {@link ExecutionLifecycleListener } that for all methods
 * does nothing. This makes it easier for those wishing to implement
 * {@link ExecutionLifecycleListener } since they only need to implement the
 * methods they care about.
 * @author jmolnar
 *
 */
public class AbstractExecutionLifecycleListener implements ExecutionLifecycleListener {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreated(Object theItem) {
		// do nothing
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStarting(Object theItem, ExecutionLifecycleState previousState) {
		// do nothing
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStarted( Object theItem, ExecutionLifecycleState thePreviousState ) {
		// do nothing
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRunning(Object theItem, ExecutionLifecycleState previousState) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStopping(Object theItem, ExecutionLifecycleState previousState) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStopped(Object theItem, ExecutionLifecycleState previousState) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSuspended(Object theItem, ExecutionLifecycleState previousState) {
		// do nothing
	}
}
