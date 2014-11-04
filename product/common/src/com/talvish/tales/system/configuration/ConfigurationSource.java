// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.system.configuration;

/**
 * An interface representing a source of data for config values.
 * @author jmolnar
 *
 */
public interface ConfigurationSource {
	/**
	 * Called to find out if the source contains the requested value.
	 * @param theName the name of the value to check for
	 * @return true if found, false otherwise
	 */
	boolean contains( String theName );		
	
	
	/**
	 * Gets the configuration value for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theType the type of the value to retrieve
	 * @return a configuration usage generated for value, if found, null otherwise
	 */
	LoadedSetting getValue( String theName, Class<?> theType );

	/**
	 * Gets the configuration value, as a list, for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theElementType the type of the element of the list to retrieve
	 * @return a configuration usage generated for value, if found, null otherwise
	 */
	LoadedSetting getList( String theName, Class<?> theElementType );

	/**
	 * Gets the configuration value, as a map, for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @return a configuration usage generated for value, if found, null otherwise
	 */
	LoadedSetting getMap( String theName, Class<?> theKeyType, Class<?> theValueType );
	
	/**
	 * The name of the source.
	 * @return the name of the source.
	 */
	String getName( );
}
