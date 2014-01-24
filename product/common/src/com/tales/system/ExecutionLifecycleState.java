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

/**
 * Represents the current running state of an item.
 * @author jmolnar
 *
 */
public enum ExecutionLifecycleState {
	/**
	 * Item was created/freshly constructed, but not yet started.
	 */
	CREATED,
	/**
	 * Item is in the processing of being ready to use.
	 */
	STARTING,
	/**
	 * Item has completed starting.
	 */
	STARTED,
	/**
	 * Item is now up and running and fully available.
	 */
	RUNNING,
	/**
	 * Item is in the process of being shutdown.
	 */
	STOPPING,
	/**
	 * Item is now shutdown and no longer able to properly execute.
	 */
	STOPPED,
	/**
	 * Item is suspended, though not shutdown, but will not execute requests.
	 */
	SUSPENDED,
}