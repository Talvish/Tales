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
package com.talvish.tales.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

/**
 * A utility class to manage a collection of listeners.
 * There is an assumption that a lot of add/removes 
 * won't be going on due to the implementation.
 * @author jmolnar
 *
 * @param <T> the type of listener
 */
public class ListenerManager< T >  {
	private Collection<T>	listeners = Collections.unmodifiableCollection( new ArrayList<T>( 0 ) );
	private Object			lock = new Object( );
	
	/**
	 * Adds a listener to the manager.
	 * @param theListener the listener to add
	 */
	public void addListener( T theListener ) {
		Preconditions.checkNotNull( theListener, "must have a listener" );
		synchronized( lock ) {
			ArrayList<T> newListeners = new ArrayList<T>( listeners );
			
			newListeners.add( theListener );
			listeners = Collections.unmodifiableCollection( newListeners );
		}
	}
	
	/**
	 * Removes a listener from the manager.
	 * @param theListener the listener to remove
	 */
	public boolean removeListener( T theListener ) {
		Preconditions.checkNotNull( theListener, "must have a listener" );
		boolean found = false;
		synchronized( lock ) {

			if( listeners.contains( theListener ) ) {
				ArrayList<T> newListeners = new ArrayList<T>( listeners );
				newListeners.remove( theListener );
				listeners = Collections.unmodifiableCollection( newListeners );
				found = true;
			}
		}
		return found;
	}
	
	/**
	 * Returns the listeners.
	 * @return the listeners
	 */
	public Collection<T> getListeners( ) {
		return listeners;
	}
}
