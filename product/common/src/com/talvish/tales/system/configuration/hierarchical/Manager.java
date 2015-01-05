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
package com.talvish.tales.system.configuration.hierarchical;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
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

/**
 * This class manages json-based configuration settings centered around a root file (and any of its includes).
 * @author jmolnar
 *
 */
public class Manager {
	private static final Logger logger = LoggerFactory.getLogger( Manager.class );

	private final String rootSource;
	
	private final JsonTranslationFacility jsonFacility;
	private final JavaType configType;
	
	private final Map<String,ConfigDescriptor> configDescriptors = new HashMap<>( );
	private final Map<String,ProfileDescriptor> profileDescriptors = new HashMap<>( );
	
	public Manager( String theFilename, JsonTranslationFacility theJsonFacility ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFilename ), "need a filename to create the manage " );
		Preconditions.checkNotNull( theJsonFacility, "need a json translation facility to create the manager" );
		
		jsonFacility = theJsonFacility;
		configType = new JavaType( ConfigDescriptor.class );
		rootSource = theFilename;
		
		// first thing we do is load the configuration which loads the main
		// file (and then include files) into memory, parses contents and
		// stores into our config and profile descriptor maps
		loadConfiguration( rootSource, new ArrayDeque<>( 2 ) );
		// now for each profile we want to make sure the profiles and
		// blocks are in a good spot
		validateBlocks( );
	}

	/**
	 * Constructor helper method that loads the configuration from the specified file.
	 * This is called by the constructor but also it is recursive if the root source
	 * includes additional files.
	 * The only thing this method does is read the raw files and create the in-memory
	 * representation of the json. It doesn't attempt to validate the contents other 
	 * than missing include files, cycles in file includes, and duplicate profiles.
	 * @param theFilename the file to be loaded
	 * @param theSourceStack ensures there isn't a loop in the include files
	 */
	private void loadConfiguration( String theSource, Deque<String> theSourceStack ) {
		logger.info( "Loading configuration from file '{}'.", theSource );

		// first we make sure there isn't a cycle THOUGH this doesn't prevent a profile
		// having a parent that is defined by a config source that is not included by
		// provided by the source including this file 
		Conditions.checkConfiguration( !theSourceStack.contains( theSource ), "Configuration source '%s' surfaced an include file cycle of '%s'.", theSource, String.join( " -> ", theSourceStack ) );

		// next we see if we have already processed this source in a different part of  
		// the inclusion tree, if so, that is okay we just don't re-process
		if( !configDescriptors.containsKey( theSource ) ) {
			// we add to the stack to prevent cycles
			theSourceStack.addLast( theSource );
		
			// to understand how this works, essentially the order of loading is important
			// the first thing it does is load all of the files and their respective 
			// includes into memory, it then assumes that previously loaded/referenced
			// files are fully loaded and properly validated, it then processes any
			// existing profiles
			
			String contents;
			ConfigDescriptor configDescriptor;
			
			contents = getSourceContents( theSource, theSourceStack );
			configDescriptor = jsonFacility.fromJsonString( contents, configType );

			// now we do a bit of cleanup (which traverses all descriptors)
			configDescriptor.cleanup( theSource );

			// we store a map of the config descriptors for debugging purposes
			configDescriptors.put( theSource, configDescriptor );
	
			// after loading the file we pull out the includes and process them
			// since the includes will have blocks and profiles needed 
			for( String include : configDescriptor.getIncludes( ) ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Configuration source '%s' included an empty or missing source. Check for trailing commas.", theSource );
				logger.info( "Configuration file '{}' includes additional configuration source '{}'.", theSource, include );
				loadConfiguration( include, theSourceStack );
			}
			
			// we need to store all of the profile descriptors, since we may need them
			// this will also be used to verify that we don't get more than one profile
			// with the same name
			for( ProfileDescriptor profileDescriptor : configDescriptor.getDeclaredProfiles( ) ) {
				Conditions.checkConfiguration( profileDescriptor != null, "Configuration source '%s' refers to an empty or missing profile source. Check for trailing commas.", theSource );
				ProfileDescriptor existingProfileDescriptor = profileDescriptors.get( profileDescriptor.getName( ) );
				String existingSource = existingProfileDescriptor == null ? "<existing>" : existingProfileDescriptor.getConfig().getSourcePath(); // this is done this way to aid debugging, exceptional conditions
				Conditions.checkConfiguration( !profileDescriptors.containsKey( profileDescriptor.getName( ) ), "Profile '%s' from '%s' has the same name as an existing profile from '%s'.", profileDescriptor.getName(), theSource, existingSource );
				profileDescriptors.put( profileDescriptor.getName(), profileDescriptor );
			}
			
			// we remove ourselves from cycle handling
			theSourceStack.removeLast( );
		}
	}

	
	/**
	 * This helper method loads into memory the contents of the specified file. 
	 * @param theFilename the file to load
	 * @param theSourceStack gives context for messages in exceptions
	 * @return the contents of the specified file
	 */
	private String getSourceContents( String theFilename, Deque<String> theSourceStack ) {
		File file = new File( theFilename );

	    int readLength;
	    char[] buffer = new char[ 4096 ];
	    StringBuffer contents = new StringBuffer();
	    FileReader reader = null;
	    
		try {
			reader = new FileReader( file );
			for( ; ; ) { 
				readLength = reader.read( buffer );
				if( readLength > 0 ) {
					contents.append( buffer, 0, readLength );
				} else {
					break;
				}
	        }
		    return contents.toString( );
		    
		} catch( FileNotFoundException e ) {
			throw new ConfigurationException( String.format( "Could not find the configuration file '%s' while traversing/loading the configuration source stack '%s'.", theFilename, String.join( " -> ", theSourceStack ) ), e );
		} catch( IOException e ) {
			throw new ConfigurationException( String.format( "Unknown I/O error reading configuration file '%s' while traversing/loading the configuration source stack '%s'.", theFilename, String.join( " -> ", theSourceStack ) ), e );
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
	
	/**
	 * This is called by the constructor to make sure our profiles and blocks are setup correctly.
	 */
	// TODO: reconsider this name
	private void validateBlocks( ) {
		Deque<String> profileStack = new ArrayDeque<String>( profileDescriptors.size( ) );
		for( ProfileDescriptor profileDescriptor : profileDescriptors.values() ) {
			Preconditions.checkState( profileStack.size() == 0, "The profile stack wasn't cleared." );
			profileDescriptor.validatePhaseOne( this, profileStack );
		}
	}
	
	/**
	 * Gets the profile descriptor for a given profile.
	 * @param theName the name of the profile to get
	 * @return the profile, or null if not found
	 */
	protected ProfileDescriptor getProfileDescriptor( String theName ) {
		return this.profileDescriptors.get( theName );
	}

	/**
	 * Gets the settings associated with the particular profile and block. This will  
	 * do additional validation to ensure there are no block cycles, proper setting  
	 * overrides, etc.
	 * @param theProfile the profile to base the configuration from
	 * @param theBlock the block the getting the settings from
	 * @return the settings or an empty map if they cannot be found
	 */
	public Map<String,SettingDescriptor> getSettings( String theProfile, String theBlock ) {
		Map<String, SettingDescriptor> settings = new HashMap<String, SettingDescriptor>( );

		ProfileDescriptor profile = this.profileDescriptors.get( theProfile );
		profile.extractSettings( profile, theBlock, settings );
		
		return settings;
	}
	
	// TODO: consider removing the log methods below
	
	/**
	 * Debugging method that will dump the configuration into the logger.
	 * This will traverse from the root file through the includes.
	 */
	public void logConfiguration( ) {
		logConfiguration( this.rootSource );
	}
	
	/**
	 * Main recursive method for dumping the configuration into the logger.
	 * @param theSource the root of the source to dump.
	 */
	private void logConfiguration( String theSource ) {
		ConfigDescriptor configDescriptor = configDescriptors.get( theSource );
		String configuration = jsonFacility.toJsonString( configDescriptor, configType, Readability.HUMAN );
		
		logger.debug( "Dumping configuration for '{}' ...", theSource );
		logger.debug( configuration );
		
		for( String includedSource : configDescriptor.getIncludes( ) ) {
			logConfiguration( includedSource );
		}
	}
	
	
	public void logAccessibleBlocks( ) {
		for( ProfileDescriptor profileDescriptor : profileDescriptors.values() ) {
			logAccessibleBlocks( profileDescriptor );
		}
	}

	private void logAccessibleBlocks( ProfileDescriptor theProfileDescriptor ) {
		logger.debug( "Dumping accessible blocks for '{}' ...", theProfileDescriptor.getName() );
		
		for( BlockDescriptor blockDescriptor : theProfileDescriptor.getAccessibleBlocks( ) ) {
			logger.debug( "Found block '{}.{}'.", blockDescriptor.getProfile().getName(), blockDescriptor.getName() );
		}
	}
}
