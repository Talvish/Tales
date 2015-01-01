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
package com.talvish.tales.system.configuration.hierarchical;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.serialization.Readability;
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.system.Conditions;
import com.talvish.tales.system.configuration.ConfigurationException;

public class Manager {
	private static final Logger logger = LoggerFactory.getLogger( Manager.class );

	private JsonTranslationFacility jsonFacility;
	private JavaType configType;
	
	private Map<String,Profile> profiles = new HashMap<>( );
	private Map<String,ProfileDescriptor> profileDescriptors = new HashMap<>( );
	
	public Manager( JsonTranslationFacility theJsonFacility ) {
		jsonFacility = theJsonFacility;
		configType = new JavaType( ConfigDescriptor.class );
	}
	

	private String loadFile( String theFilename ) {
		File file = new File( theFilename );

	    int len;
	    char[] chr = new char[ 4096 ];
	    StringBuffer buffer = new StringBuffer();
	    FileReader reader = null;
		try {
			reader = new FileReader( file );
	        while( ( len = reader.read( chr ) ) > 0 ) {
	            buffer.append(chr, 0, len);
	        }
		    return buffer.toString( );
		} catch (FileNotFoundException e) {
			throw new ConfigurationException( e );
		} catch( IOException e ) {
			throw new ConfigurationException( e );
	    } finally {
	    	if( reader != null ) {
	    		try {
					reader.close( );
				} catch (IOException e) {
					// we deliberately absorb this
				}
	    	}
	    }
	}

	public void dumpConfig( ConfigDescriptor theConfig ) {		
		String string = jsonFacility.toJsonString( theConfig, configType, Readability.HUMAN );
		
		System.out.print( string );
	}

	public ConfigDescriptor loadConfiguration( String theFilename ) {
		logger.info( "Loading configuration from file '{}'.", theFilename );
		// to understand how this works, essentially the order of loading is important
		// the first thing it does is load all of the files and their respective 
		// includes into memory, it then assumes that previously loaded/referenced
		// files are fully loaded and properly validated, it then processes any
		// existing profiles
		
		// TODO: we need to manage cycles and also we need to watch the ordering 
		// 		 of profile handling since we assume things are done in order 
		//		 while we can make that assumption when it references other
		//		 files but cannot when it is within the same file,
		// 		 so one option for that is to basically make the profiles descriptors
		//		 all loaded and we can reference them BUT if they are not processed
		//		 when requested then we can cause them to be referenced, meaning
		// 		 we try to load the profile first and if not available check for
		//		 a profile descriptor and if there process (and if not, we have bad config)
		//		 so then we just need to look for cycles
		//		 we might be able to do this on descriptors by using the ondeserialize methods
		//		 to make the data available in a map, or if we believe memory is an issue then
		//		 we can do a linear search (we could start that way I suppose too)

		// TODO: separate out the loading of the profiles and the preparing of the profiles
		//		 this would allow people to build an xml version or some other format if
		//		 so desired
		String file;
		ConfigDescriptor configDescriptor;
		
		file = loadFile( theFilename );
		configDescriptor = jsonFacility.fromJsonString( file, configType );

		// after loading the file we pull out the includes and 
		// process them to make the profiles and blocks are 
		// available for profiles and blocks in the file that
		// was passed in
		if( configDescriptor.getIncludes() != null ) { // TODO: must be a better way
			for( String include : configDescriptor.getIncludes( ) ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Configuration file '%s' refers to an empty or missing include file. Check for trailing commas.", theFilename );
				logger.info( "Configuration file '{}' includes additional configuration file '{}'.", theFilename, include );
				// TODO: take a look at the names like load configuration 
				//		 to make sure they are doing the right thing
				loadConfiguration( include );
			}
		}
		
		// now we go through and verify all of the pieces
//		for( ProfileDescriptor profileDescriptor : configDescriptor.getProfiles( ) ) {
//			prepareProfile( configDescriptor, profileDescriptor );
//		}
		
		
		return configDescriptor;
	}
	

	private void prepareProfile( ConfigDescriptor theConfigDescriptor, ProfileDescriptor theProfileDescriptor ) {
		
		Profile inheritedProfile;
		ProfileDescriptor inheritedProfileDescriptor;
		
		// first we make sure we have the profiles that are referenced
		for( String inheritedProfileName : theProfileDescriptor.getIncludes( ) ) {
			inheritedProfile = this.profiles.get( inheritedProfileName );
			//if( inheritedProfileDescriptor)
			Conditions.checkConfiguration( inheritedProfile != null, "Profile '%s' inherits from unknown profile '%s'.", theProfileDescriptor.getName(), inheritedProfileName );
		}

		Profile profile = new Profile( theProfileDescriptor.getName(), theProfileDescriptor.getDescription( ) );
		Conditions.checkConfiguration( !this.profiles.containsKey( theProfileDescriptor.getName( ) ), "A profile with name '%s' already exists", theProfileDescriptor.getName() );

		// now we look at the blocks
		for( BlockDescriptor blockDescriptor : theProfileDescriptor.getBlocks( ) ) {
			profile.addBlock( prepareBlock( profile, blockDescriptor ) );
		}
		
		// now we save the profile
		this.profiles.put( profile.getName( ), profile );
	}
	
	private Block prepareBlock( Profile theProfile, BlockDescriptor theBlockDescriptor ) {
		// we don't do validation here because the constructor does all the validation 
		// based on the values given and any previous blocks found on the profile
		Block block = new Block( 
				theBlockDescriptor.getName( ), 
				theBlockDescriptor.getDescription( ), 
				theProfile );
		// now we look at the settings
		for( SettingDescriptor settingDescriptor : theBlockDescriptor.getSettings( ) ) {
			block.addSetting( prepareSetting( theProfile, block, settingDescriptor ) );
		}
		return block;
	}
	
	private Setting prepareSetting( Profile theProfile, Block theBlock, SettingDescriptor theSettingDescriptor ) {
		// we don't do validation here because the constructor does all the validation 
		// based on the values given and any previous setting found on the profile
		return new Setting( 
				theSettingDescriptor.getName(),
				theSettingDescriptor.getDescription(),
				theSettingDescriptor.getValue(),
				theSettingDescriptor.getType(),
				theSettingDescriptor.isOverride(),
				theSettingDescriptor.isSensitive(),
				theBlock,
				theProfile );
		
	}
}
