// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.system.configuration.annotated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This is a special collection that is not intended to change dramatically overtime and largely be
 * filled at close to creation, but cannot be done at construction.
 * @author jmolnar
 *
 * @param <T> The type of element store in the collection.
 */
// TODO: look at concurrenthashmap more closely and see if would cover all our bases here (other than no dupes)
public class RegisteredCollection<T> {
	private List<T> list = Collections.unmodifiableList( new ArrayList<T>( 0 ) );
	private Map<String, T> map = Collections.unmodifiableMap( new HashMap<String, T>( 0 ) );
	private Object lock = new Object( );
	
	/**
	 * Registers the item into the collection. Since the expectation
	 * is that reads will far outweigh writes, the need for (with no need for read locks) 
	 * and quick lookup and easy iteration, this method recreates the
	 * underlying collections to at the new one. 
	 * @param theName the name of the item being added
	 * @param theItem the item to add
	 */
	public void register( String theName, T theItem ) {
		Preconditions.checkNotNull( theItem );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "The item being registered needs a name." );
		synchronized( this.lock ) {
			Preconditions.checkArgument( !this.map.containsKey( theName ), String.format( "An item named '%s' is already registered.", theName ) );

			HashMap<String, T> newMap = new HashMap<>( map );
			newMap.put( theName, theItem );
			map= Collections.unmodifiableMap( newMap );
			
			List<T> newList = new ArrayList<>( this.list );
			newList.add( theItem );
			list = Collections.unmodifiableList( newList );
		}
	}
	
	/**
	 * Checks to see if the given name was already registered.
	 * @param theName the name to see if it was registered already
	 * @return true if it was registered already, false otherwise
	 */
	public boolean contains( String theName ) {
		Preconditions.checkNotNull( !Strings.isNullOrEmpty( theName ), "Need a name before you can see if it exists." );
		return map.containsKey( theName );
	}
	
	/**
	 * Returns the named item, or null if it wasn't found.
	 * @param theName the registered item to return 
	 * @return the registered item or null if it could not be found
	 */
	public T get( String theName ) {
		Preconditions.checkNotNull( !Strings.isNullOrEmpty( theName ), "Need a name to retrieve a value." );
		return map.get( theName );
	}
	
	/**
	 * Returns the entire list of registered items.
	 * @return the collection item items or an empty list
	 */
	public Collection<T> getAll( ) {
		return list;
	}
	
	/**
	 * The number of items in the collection.
	 * @return number of registered items
	 */
	public int size( ) {
		return list.size();
	}
}
