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
package com.talvish.tales.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A utility class that manages thread pools/executors. 
 * It makes the assumptions that a lot of registration doesn't
 * occur since it makes copies of objects during the registration
 * process.
 * @author jmolnar
 *
 */
public class ExecutorManager implements Facility {
	// it maintains a map and a list so when the collection of elements
	// are returned, they are returned in registration order
	private List<Executor> list			= Collections.unmodifiableList( new ArrayList<Executor>( 0 ) );
	private Map<String, Executor> map	= Collections.unmodifiableMap( new HashMap<String, Executor>( 0 ) );
	private final Object lock 			= new Object( );
	
	/**
	 * Returns the executor with the specified name.
	 * @param theName the name of the executor to get
	 * @return the executor with the specified name or {@code null} if not found
	 */
	public Executor getExecutor( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name of the executor");
		return this.map.get( theName );
	}
	
	/**
	 * Returns the executor with the specified name.
	 * A class can be provided if the executor is known to be of a particular type.
	 * @param theName the name of the executor to get
	 * @param theType the type of the executor expected
	 * @return the executor with the specified name or {@code null} if not found
	 * @throws IllegalArgumentException thrown if the type provided is not the type of the executor
	 */
	@SuppressWarnings("unchecked")
	public <T extends Executor> T getExecutor( String theName, Class<T> theType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name of the executor");
		Preconditions.checkNotNull( theType, "need a type to get executor '%s'", theName );
		try {
			Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name of the executor");
			return ( T )this.map.get( theName );
		} catch( ClassCastException e ) {
			throw new IllegalArgumentException( String.format( "The executor '%s' is not of type '%s'.", theName, theType.getName( ) ), e );
		}
	}
	
	
	/**
	 * Returns the executors exposed.
	 * @return the list of executors
	 */
	public Collection<Executor> getExecutors( ) {
		return list;
	}
	
	/**
	 * Registers an executor.
	 * @param theExecutor the executor to register
	 */
	public void register( String theName, Executor theExecutor ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "the item needs a name" );
		Preconditions.checkNotNull( theExecutor );
		synchronized( this.lock ) {
			Preconditions.checkArgument( !this.map.containsKey( theName ), String.format( "An item named '%s' is already registered.", theName ) );

			HashMap<String, Executor> newMap = new HashMap<>( this.map );
			newMap.put( theName, theExecutor);
			map = Collections.unmodifiableMap( newMap );
			
			List<Executor> newList = new ArrayList<>( this.list );
			newList.add( theExecutor );
			list = Collections.unmodifiableList( newList );
		}
	}
}
