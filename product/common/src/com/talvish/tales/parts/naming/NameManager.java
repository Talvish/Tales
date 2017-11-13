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
package com.talvish.tales.parts.naming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.validation.Conditions;

/**
 * A utility class used to validate names read in from external sources or exposed externally.
 * @author jmolnar
 *
 */
public final class NameManager {
	private static Map<String, NameValidator> validators = Collections.unmodifiableMap( new HashMap<String,NameValidator>() );
	private static final Object lock = new Object();


	/**
	 * Returns the requested validator.
	 * @param theArea the area representing the validator to get
	 * @return the requested validator or null if not found
	 */
	public static NameValidator getValidator( String theArea ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theArea ), "theArea", "area was not given" );
		return validators.get( theArea );
	}

	/**
	 * Sets the validator to use for a particular area. 
	 * Setting the validator to null removes the validator.
	 * @param theArea the area representing the validator to set
	 * @param theValidator the validator to use for the area
	 */
	public static void setValidator( String theArea, NameValidator theValidator ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theArea ), "theArea", "area was not given" );

		synchronized( lock ) {
			if( theValidator == null ) {
				validators.remove( theArea );
			} else {
				HashMap<String, NameValidator> newvalidators = new HashMap<String, NameValidator>( validators );
				newvalidators.put( theArea, theValidator );
				validators = Collections.unmodifiableMap( newvalidators );
			}			
		}
	}
	
	/**
	 * Indiates whether a valiator exists for a particular area.
	 * @param theArea the area to check for
	 * @return returns true if a validator is set and false otherwise
	 */
	public static boolean hasValidator( String theArea ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theArea ), "theArea", "area was not given" );

		return validators.containsKey( theArea );
	}	
}
