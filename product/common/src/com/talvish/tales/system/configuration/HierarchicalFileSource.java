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
package com.talvish.tales.system.configuration;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContractTypeSource;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.system.Conditions;
import com.talvish.tales.system.configuration.hierarchical.JsonStringToListTranslator;
import com.talvish.tales.system.configuration.hierarchical.JsonStringToMapTranslator;
import com.talvish.tales.system.configuration.hierarchical.Setting;
import com.talvish.tales.system.configuration.hierarchical.SourceManager;

/**
 * This class represents a config source where data is sourced from 
 * a json-based, hierarchical config file. 
 * @author jmolnar
 *
 */
public class HierarchicalFileSource implements ConfigurationSource {
	private final String defaultSourceName;
	private final String profile;
	private final String block;
	
	private final SourceManager manager;
	
	private final Map<String,Setting> settings;
	
	/**
	 * Constructor taking the filename of the json-based file and the property and block to use.
	 * @param theFilename the path of the properties file
	 */
	public HierarchicalFileSource( String theProfile, String theBlock, String theFilename ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theProfile ), "Need a profile." );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBlock ), "Need a block." );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFilename ), "Need a filename." );

		profile = theProfile;
		block = theBlock;
		defaultSourceName = String.format( Setting.SOURCE_NAME_FORMAT, theFilename, theProfile, theBlock );
		
		manager = new SourceManager( theFilename, new JsonTranslationFacility( new DataContractTypeSource( ) ) );
		settings = manager.extractSettings( profile, block );
		Conditions.checkConfiguration( settings != null, "Could not find block '%s.%s' from source '%s'.", profile, block, theFilename );
	}
	
	/**
	 * Called to find out if the source contains the requested value.
	 * @param theName the name of the value to retrieve
	 * @return the value retrieved
	 */
	public boolean contains( String theName ) {
		return settings.containsKey( theName );
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
		if( settings.containsKey( theName ) ) {
			Setting hierarchicalSetting = settings.get( theName );
			setting = SettingValueHelper.generateValue( 
					theName, 
					hierarchicalSetting.getValue( ), 
					hierarchicalSetting.getDescription(), 
					hierarchicalSetting.isSensitive(),
					hierarchicalSetting.getSourceName(),
					theType );
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
		if( settings.containsKey( theName ) ) {
			Setting hierarchicalSetting = settings.get( theName );
			setting = generateList( 
					theName, 
					hierarchicalSetting.getValue( ), 
					hierarchicalSetting.getDescription(), 
					hierarchicalSetting.isSensitive(), 
					hierarchicalSetting.getSourceName(),
					theElementType );
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
		if( settings.containsKey( theName ) ) {
			Setting hierarchicalSetting = settings.get( theName );
			setting = generateMap( 
					theName, 
					hierarchicalSetting.getValue( ), 
					hierarchicalSetting.getDescription(), 
					hierarchicalSetting.isSensitive(), 
					hierarchicalSetting.getSourceName(),
					theKeyType, 
					theValueType );
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
		return defaultSourceName;
	}
	
	
	/**
	 * Generates a setting from a json string, as a list of a specific type. 
	 * It will throw exceptions if the string cannot be converted.
	 * @param theName the name of the value 
	 * @param theStringValue the string value to be translated
	 * @param theDescription the description for the setting
	 * @param isSensitive indicates if this is a private setting
	 * @param theSource the name given to the source of the value
	 * @param theElementType the type of the element 
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	private LoadedSetting generateList( String theName, String theStringValue, String theDescription, boolean isSensitive, String theSource, Class<?> theElementType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name cannot be null or empty.");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ), "Source cannot be null or empty.");
		Preconditions.checkNotNull( theElementType, "Need a type to be able to translate." );
		
		Translator elementTranslator = SettingValueHelper.getTranslator( theElementType );
		Preconditions.checkState( elementTranslator != null, "Element type '%s' did not have a translator.", theElementType.getName( ) );
		
		// NOTE: we generate this on the fly, we could store create items for lookup later 
		JsonStringToListTranslator collectionTranslator = new JsonStringToListTranslator( elementTranslator, true, null, null );
		
		try {
			return new LoadedSetting( theName, collectionTranslator.translate( theStringValue ), theStringValue, theDescription, isSensitive, theSource );
		} catch( TranslationException e ) {
			throw new ConfigurationException( String.format( "'%s' had a value of '%s' that could not be translated into a list of '%s'.", theName, theStringValue, theElementType.getName( ) ), e );
		}
	}

	
	/**
	 * Generates a setting from a json string, as a map of the specific key and value types. 
	 * It will throw exceptions if the string cannot be converted.
	 * @param theName the name of the value 
	 * @param theStringValue the string value to be translated
	 * @param theDescription the description for the setting
	 * @param isSensitive indicates if this is a private setting
	 * @param theSource the name given to the source of the value
	 * @param theKeyType the type of the key 
	 * @param theValueType the type of the value 
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	private LoadedSetting generateMap( String theName, String theStringValue, String theDescription, boolean isSensitive, String theSource, Class<?> theKeyType, Class<?> theValueType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name cannot be null or empty.");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ), "Source cannot be null or empty.");
		Preconditions.checkNotNull( theKeyType, "Need a key type to be able to translate." );
		Preconditions.checkNotNull( theValueType, "Need a value type to be able to translate." );
		
		Translator keyTranslator = SettingValueHelper.getTranslator( theKeyType );
		Translator valueTranslator = SettingValueHelper.getTranslator( theValueType );
		Preconditions.checkState( keyTranslator != null, "Key type '%s' did not have a translator.", theKeyType.getName( ) );
		Preconditions.checkState( valueTranslator != null, "Value type '%s' did not have a translator.", theValueType.getName( ) );
		
		// NOTE: we generate this on the fly, we could store create items for lookup later 
		JsonStringToMapTranslator collectionTranslator = new JsonStringToMapTranslator( keyTranslator, valueTranslator, true, null, null );
		
		try {
			return new LoadedSetting( theName, collectionTranslator.translate( theStringValue ), theStringValue, theDescription, isSensitive, theSource );
		} catch( TranslationException e ) {
			throw new ConfigurationException( String.format( "'%s' had a value of '%s' that could not be translated into a map of '[%s,%s]'.", theName, theStringValue, theKeyType.getName( ), theValueType.getName( ) ), e );
		}
	}
}
