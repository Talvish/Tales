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
package com.tales.system.configuration;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class represents a config source were config data 
 * is sourced from a map.
 * @author jmolnar
 *
 */public class MapSource implements ConfigurationSource {
	private Map<String,String> values;
	private String sourceName;
	
	/**
	 * Constructor taking the source name and the map containing the values.
	 * @param theSourceName the name to give the source
	 * @param theValues the map containing the values
	 */
	public MapSource( String theSourceName, Map<String,String> theValues ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSourceName ) );
		Preconditions.checkNotNull( theValues, "No values given" );
		
		this.sourceName = theSourceName;
		values = theValues;
	}
	
	/**
	 * Called to find out if the source contains the requested value.
	 * @param theName the name of the value to check for
	 * @return true if found, false otherwise
	 */
	public boolean contains( String theName ) {
		return values.containsKey( theName );
	}
	
	/**
	 * Gets the configuration value for name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theType the type of the value to retrieve
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public LoadedSetting getValue( String theName, Class<?> theType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name value is null or empty.");
		Preconditions.checkNotNull( theType, "Need a type to be able to translate." );

		LoadedSetting setting = null;
		if( values.containsKey( theName ) ) {
			setting = SettingValueHelper.generateValue( theName, values.get( theName ), null, false, sourceName, theType );
		}
		return setting;
	}
	
	/**
	 * Gets the configuration value, as a list, for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theElementType the type of the element of the list to retrieve
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public LoadedSetting getList( String theName, Class<?> theElementType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name value is null or empty.");
		Preconditions.checkNotNull( theElementType, "Need an element type to be able to translate." );

		LoadedSetting setting = null;
		if( values.containsKey( theName ) ) {
			setting = SettingValueHelper.generateList( theName, values.get( theName ), null, false, sourceName, theElementType );
		}
		return setting;
	}
	
	/**
	 * Gets the configuration value, as a list, for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public LoadedSetting getMap( String theName, Class<?> theKeyType, Class<?> theValueType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name value is null or empty.");
		Preconditions.checkNotNull( theKeyType, "Need a key type to be able to translate." );
		Preconditions.checkNotNull( theValueType, "Need a value type to be able to translate." );

		LoadedSetting setting = null;
		if( values.containsKey( theName ) ) {
			setting = SettingValueHelper.generateMap( theName, values.get( theName ), null, false, sourceName, theKeyType, theValueType );
		}
		return setting;
	}

	/**
	 * Returns the name given to this source. 
	 */
	@Override
	public String getName() {
		return sourceName;
	}
}
