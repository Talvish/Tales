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
package com.talvish.tales.auth.capabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Facility;

/**
 * A utility class that manages capability definitions. 
 * It makes the assumptions that a lot of registration doesn't
 * occur since it makes copies of objects during the registration
 * process.
 * @author jmolnar
 *
 */
public class CapabilityDefinitionManager implements Facility {
	
	// the auth token can have a hash on it for quick validation of changes in permissions 
	// which can cause some form of re-up  ... this can potentially be done by 
	// have an in memory cache of the token that is verified by the auth service and it will
	// do a quick check of the hash (not recalculate)
	
	
	// we are going to add apps/clients/integrators and that will be storing a secret for the apps ... wondering if we shoudl be
	// we storing that hashed, though we could never show it in teh UI then...
	
	//TODO: curious about how to do impersonation


	
	// it maintains a map and a list so when the collection of elements
	// are returned, they are returned in registration order
	private List<CapabilityFamilyDefinition> list	= Collections.unmodifiableList( new ArrayList<CapabilityFamilyDefinition>( 0 ) );
	private Map<String, CapabilityFamilyDefinition> map	= Collections.unmodifiableMap( new HashMap<String, CapabilityFamilyDefinition>( 0 ) );
	private final Object lock = new Object( );
	
	/**
	 * Returns the set with the specified name.
	 * @param theName the name of the set to get
	 * @return the set with the specified name or {@code null} if not found
	 */
	public CapabilityFamilyDefinition getFamily( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need the name");
		return this.map.get( theName );
	}
	
	/**
	 * Returns the set exposed.
	 * @return the list of set
	 */
	public Collection<CapabilityFamilyDefinition> getFamilies( ) {
		return list;
	}
	
	/**
	 * Registers a family of capabilities.
	 * @param theFamily the set to register
	 */
	public void register( CapabilityFamilyDefinition theFamily ) {
		Preconditions.checkNotNull( theFamily );

		synchronized( this.lock ) {
			Preconditions.checkArgument( !this.map.containsKey( theFamily.getName( ) ), String.format( "An item named '%s' is already registered.", theFamily.getName( ) ) );

			HashMap<String, CapabilityFamilyDefinition> newMap = new HashMap<>( this.map );
			newMap.put( theFamily.getName( ), theFamily);
			map = Collections.unmodifiableMap( newMap );
			
			List<CapabilityFamilyDefinition> newList = new ArrayList<>( this.list );
			newList.add( theFamily );
			list = Collections.unmodifiableList( newList );
		}
	}
}

