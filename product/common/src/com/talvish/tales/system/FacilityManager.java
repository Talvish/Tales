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

import java.util.Collection;

/**
 * A class that manages a set of facilities. This could be considered 
 * a mechanism similar to managing a set of singletons. 
 * @author jmolnar
 *
 */
public interface FacilityManager {
	/**
	 * Gets all the facilities supported by the manager.
	 * @return the collection of facilities
	 */
	Collection<Facility> getFacilities( );
	/**
	 * Gets a particular facility.
	 * @param theFacilityType the type of facility to get
	 * @return the facility or null if not available
	 */
	<F extends Facility> F getFacility( Class<F> theFacilityType );
	/**
	 * Adds a particular facility to the manager. Only one instance 
	 * of a facility is available per type.
	 * @param theFacilityType the type to reference the facility by
	 * @param theFacilityInstance the instance of the facility to add
	 */
	<F extends Facility> void addFacility( Class<F> theFacilityType, F theFacilityInstance );
	/**
	 * Removes a particular facility from the manager.
	 * @param theFacilityType the facility to remove, as referenced by the type.
	 * @return true if the facility was found and removed, false otherwise
	 */
	<F extends Facility> boolean removeFacility( Class<F> theFacilityType );
}
