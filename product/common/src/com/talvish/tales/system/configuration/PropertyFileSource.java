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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class represents a config source where config data 
 * is sourced from a properties file.
 * @author jmolnar
 *
 */
public class PropertyFileSource implements ConfigurationSource {
	private Properties properties;
	private String sourceName;
	
	/**
	 * Constructor taking the filename of a properties file to use.
	 * @param theFilename the path of the properties file
	 */
	public PropertyFileSource( String theFilename ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFilename ) );

		File file = new File( theFilename );
		constructorHelper( file.getAbsolutePath(), theFilename );
	}
	
	/**
	 * Constructor taking the source name and filename of a properties file to use.
	 * @param theSourceName the name to give the source
	 * @param theFilename the path of the properties file
	 */
	public PropertyFileSource( String theSourceName, String theFilename ) {
		constructorHelper( theSourceName, theFilename );
	}
	
	/**
	 * Constructor helper taking the source name and filename of a properties file to use.
	 * @param theSourceName the name to give the source
	 * @param theFilename the path of the properties file
	 */
	private void constructorHelper( String theSourceName, String theFilename ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSourceName ) );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFilename ) );
		
		this.sourceName = theSourceName;
		this.properties = new Properties();
		try {
			this.properties.load( new FileInputStream( theFilename ) );
		} catch (FileNotFoundException e) {
			throw new ConfigurationException( String.format( "Could not find file '%s'.", theFilename), e);
		} catch (IOException e) {
			throw new ConfigurationException( String.format( "Could not load configuraton from file '%s'.", theFilename), e);
		}
	}
	
	/**
	 * Called to find out if the source contains the requested value.
	 * @param theName the name of the value to retrieve
	 * @return the value retrieved
	 */
	public boolean contains( String theName ) {
		return properties.containsKey( theName );
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
		if( properties.containsKey( theName ) ) {
			setting = SettingValueHelper.generateValue( theName, properties.getProperty( theName ), null, false, sourceName, theType );
		}
		return setting;
	}
	
	/**
	 * Gets the configuration value, as a list, for the name given. If the value
	 * doesn't exist a null is returned.
	 * @param theName the name of the value to retrieve
	 * @param theElementType the type of the element of the list retrieve
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public LoadedSetting getList( String theName, Class<?> theElementType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name value is null or empty.");
		Preconditions.checkNotNull( theElementType, "Need an element type to be able to translate." );

		LoadedSetting setting = null;
		if( properties.containsKey( theName ) ) {
			setting = SettingValueHelper.generateList( theName, properties.getProperty( theName ), null, false, sourceName, theElementType );
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
		if( properties.containsKey( theName ) ) {
			setting = SettingValueHelper.generateMap( theName, properties.getProperty( theName ), null, false, sourceName, theKeyType, theValueType );
		}
		return setting;
	}
	
	/**
	 * Returns the name given to this source. 
	 * If a name wasn't given on construction
	 * then the source path is the name.
	 */
	@Override
	public String getName() {
		return sourceName;
	}

}
