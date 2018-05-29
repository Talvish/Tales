// ***************************************************************************
// *  Copyright 2012-2017 Joseph Molnar
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
package com.talvish.tales.services.http.servlets;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;

import com.google.common.base.Preconditions;

/**
 * A simple helper class that manages if the non-blocking
 * call was executed or not, in part because, at least in
 * Jetty, the AsyncContext/ServletResponse cannot bec
 * checked (without exceptions or re-set data) 
 * @author jmolnar
 *
 */
public class ResourceAsyncState {
	private final AsyncContext context;
	private AtomicBoolean completed = new AtomicBoolean( false );
	
	/**
	 * Constructor taking the AsyncContext 
	 * this state is associated with.
	 * @param theContext the associated context
	 */
	public ResourceAsyncState( AsyncContext theContext ) {
		Preconditions.checkNotNull( theContext, "need a context" );
		context = theContext;
	}

	/**
	 * Returns the associated context.
	 * @return the assocated context.
	 */
	public AsyncContext getContext(  ) {
		return context;
	}
	
	/**
	 * Indicates if the associated context/operation has completed.
	 * @return indicates operation has completed or not
	 */
	public final boolean hasCompleted( ) {
		return completed.get();
	}
	
	/**
	 * Sets the completed state to true indicating the 
	 * associating context/operation is done. It is not
	 * an indication of success.
	 * @return indicates if the state was set during this call or or not, indicating whether this was the call to update the state, or it previously been set
	 */
	public final boolean setCompleted( ) {
		// as a note, a race condition is possible in generally knowing which condition occurred
		// between successful finish, a time, or even a rejected queue insertion (meaning too busy)
		return !completed.getAndSet( true );
	}
}