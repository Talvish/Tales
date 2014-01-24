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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * A simple implementation of the facility manager that stores facilities. 
 * @author jmolnar
 *
 */
public class SimpleFacilityManager implements FacilityManager {
	private Map<Class<?>, Facility> facilities = Collections.unmodifiableMap( new HashMap<Class<?>, Facility>() );
	private final Object lock = new Object();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Facility> getFacilities() {
		return facilities.values();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <F extends Facility> F getFacility( Class<F> theFacilityType ) {
		return ( F )facilities.get( theFacilityType );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <F extends Facility> void addFacility(Class<F> theFacilityType, F theFacilityInstance) {
		Preconditions.checkNotNull( theFacilityType );
		Preconditions.checkNotNull( theFacilityInstance );

		synchronized( lock ) {
			Preconditions.checkState( !facilities.containsKey( theFacilityType ) );
			
			HashMap<Class<?>, Facility> newFacilities = new HashMap<Class<?>, Facility>( facilities );
			newFacilities.put( theFacilityType, theFacilityInstance );
			facilities = Collections.unmodifiableMap( newFacilities );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <F extends Facility> boolean removeFacility(	Class<F> theFacilityType ) {
		Preconditions.checkNotNull( theFacilityType );

		boolean found = false;

		synchronized( lock ) {
			found = facilities.containsKey( theFacilityType );
			if( found ) {
				HashMap<Class<?>, Facility> newFacilities = new HashMap<Class<?>, Facility>( facilities );
				newFacilities.remove( theFacilityType );
				facilities = Collections.unmodifiableMap( newFacilities );
			}
		}
		return found;
	}
}
