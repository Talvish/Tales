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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
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

	private String rootSource;
	
	private JsonTranslationFacility jsonFacility;
	private JavaType configType;
	
	private Map<String,Profile> profiles = new HashMap<>( );
	
	private Map<String,ConfigDescriptor> configDescriptors = new HashMap<>( );
	private Map<String,ProfileDescriptor> profileDescriptors = new HashMap<>( );
	
	public Manager( String theFilename, JsonTranslationFacility theJsonFacility ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFilename ), "need a filename to create the manage " );
		Preconditions.checkNotNull( theJsonFacility, "need a json translation facility to create the manager" );
		
		jsonFacility = theJsonFacility;
		configType = new JavaType( ConfigDescriptor.class );
		rootSource = theFilename;
		
		// first thing we do is load the configuration which loads 
		// each file (and included file) into memory, parses the
		// the contents and stores into our config and profile
		// descriptor maps ... the descriptors are direct in
		// memory representations of the storage shape
		loadConfiguration( rootSource, new ArrayDeque<>( 2 ) );
		// now for each profile we want to make sure
		// that the blocks are in a good spot
		validateBlocks( );
	}

	/**
	 * Constructor helper method that loads the configuration from the specified file.
	 * This is called by the constructor but also it is recursive if the root source
	 * includes additional files.
	 * The only thing this method does is read the raw files and create the in-memory
	 * representation of the json. It doesn't attempt to validate the contents other 
	 * than missing include files or duplicate profiles.
	 * @param theFilename the file to be loaded
	 * @param theSourceStack ensures there isn't a loop in the include files
	 */
	private void loadConfiguration( String theSource, Deque<String> theSourceStack ) {
		logger.info( "Loading configuration from file '{}'.", theSource );

		// first we make sure there isn't a cycle THOUGH this doesn't prevent a profile
		// having a parent that is defined by a config source that is not included by
		// provided by the source including this file ... it allows for some interesting
		// abilities to have forward looking config files
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
			
			contents = getSourceContents( theSource );
			configDescriptor = jsonFacility.fromJsonString( contents, configType );

			// now we do a bit of cleanup (which traverses all descriptors)
			configDescriptor.cleanup( theSource );

			// we store a map of the config descriptors for debugging purposes
			configDescriptors.put( theSource, configDescriptor );
	
			// after loading the file we pull out the includes and process them
			// since the includes will have blocks and profiles needed 
			for( String include : configDescriptor.getIncludes( ) ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Configuration source '%s' refers to an empty or missing included source. Check for trailing commas.", theSource );
				logger.info( "Configuration file '{}' includes additional configuration source '{}'.", theSource, include );
				loadConfiguration( include, theSourceStack );
			}
			
			// we need to store all of the profile descriptors, since we may need them
			// this will also be used to verify that we don't get more than one profile
			// with the same name
			for( ProfileDescriptor profileDescriptor : configDescriptor.getProfileDescriptors( ) ) {
				Conditions.checkConfiguration( profileDescriptor != null, "Configuration source '%s' refers to an empty or missing profile source. Check for trailing commas.", theSource );
				ProfileDescriptor existingProfileDescriptor = profileDescriptors.get( profileDescriptor.getName( ) );
				String existingSource = existingProfileDescriptor == null ? "<existing>" : existingProfileDescriptor.getConfigDescriptor().getSource(); // this is done this way to aid debugging, exceptional conditions
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
	 * @return the contents of the specified file
	 */
	private String getSourceContents( String theFilename ) {
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
			throw new ConfigurationException( String.format( "Could not find the configuration file '%s'", theFilename ), e );
		} catch( IOException e ) {
			throw new ConfigurationException( String.format( "Unknown I/O error reading configuration file '%s'", theFilename ), e );
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
	
	private void validateBlocks( ) {
		Deque<String> profileStack = new ArrayDeque<String>( profileDescriptors.size( ) );
		for( ProfileDescriptor profileDescriptor : profileDescriptors.values() ) {
			Preconditions.checkState( profileStack.size() == 0, "The profile stack wasn't cleared." );
			profileDescriptor.validatePhaseOne( this, profileStack );
		}
	}

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
			logger.debug( "Found block '{}.{}'.", blockDescriptor.getProfileDescriptor().getName(), blockDescriptor.getName() );
		}
	}
	
	public void logSettings( String theProfile, String theBlock ) {
		Map<String, SettingDescriptor> settings = new HashMap<String, SettingDescriptor>( );

		ProfileDescriptor profile = this.profileDescriptors.get( theProfile );
		profile.extractSettings( profile, theBlock, settings );
		
		logger.debug( "Dumping settings for '{}.{}' ...", theProfile, theBlock );
		for( SettingDescriptor setting : settings.values( ) ) {
			logger.debug( "Found setting '{}' from block '{}.{}'.", setting.getName(), setting.getBlockDescriptor( ).getProfileDescriptor().getName(), setting.getBlockDescriptor( ).getName() );
		}
	}

	protected ProfileDescriptor getProfileDescriptor( String theName ) {
		return this.profileDescriptors.get( theName );
	}

//	// TODO: we need to manage cycles and also we need to watch the ordering 
//	// 		 of profile handling since we assume things are done in order 
//	//		 while we can make that assumption when it references other
//	//		 files but cannot when it is within the same file,
//	// 		 so one option for that is to basically make the profiles descriptors
//	//		 all loaded and we can reference them BUT if they are not processed
//	//		 when requested then we can cause them to be referenced, meaning
//	// 		 we try to load the profile first and if not available check for
//	//		 a profile descriptor and if there process (and if not, we have bad config)
//	//		 so then we just need to look for cycles
//	//		 we might be able to do this on descriptors by using the ondeserialize methods
//	//		 to make the data available in a map, or if we believe memory is an issue then
//	//		 we can do a linear search (we could start that way I suppose too)
//
//	// TODO: separate out the loading of the profiles and the preparing of the profiles
//	//		 this would allow people to build an xml version or some other format if
//	//		 so desired
//
//	
//
//	
//	public Profile generateProfile( String theProfile ) {
//		Preconditions.checkArgument( !Strings.isNullOrEmpty( theProfile ), "Attempting to generate a profile without a name." );
//		Conditions.checkConfiguration( this.profiles.containsKey( theProfile ), "Attempting to generate profile '%s', which has already been generated or there is a loop in the profile inheritance.", theProfile );
//		
//		ProfileDescriptor profileDescriptor = this.profileDescriptors.get( theProfile );
//		Conditions.checkConfiguration( profileDescriptor != null, "Attempting to generate profile '%s', when that profile descriptor cannot be found.", theProfile );
//		
//		Profile profile = new Profile( profileDescriptor.getName(), profileDescriptor.getDescription( ) );
//		// we put the profile in the profile list so we can detect looping in the inheritance structure
//		this.profiles.put( profile.getName(), profile ); 
//
//		// check if we have a parent, and if we so, we look to generate it as well
//		if( !Strings.isNullOrEmpty( profileDescriptor.getParent( ) ) ) {
//			Profile parentProfile = generateProfile( profileDescriptor.getParent( ) ); // TODO: consider an internal method for this instead (so we do less error checking on the public interface since they are just trying to get it)
//			profile.setParent( parentProfile ); 
//		}
//		
//		// now we look at the declared blocks
//		for( BlockDescriptor blockDescriptor : profileDescriptor.getDeclaredBlock( ) ) {
//			prepareBlock( blockDescriptor, profile, profileDescriptor );
//		}
//		
//		// from here we can see if the descriptor has a parent
//		// and if it does we go back BUT we want to prevent cycles SO
//		// we partially create the profile and 
//		
//		
//		// phase 1 - load the configuration and create the profile descriptors, this will ensure uniqueness of profile names
//		// phase 2 - for each profile, load the blocks, declared settings, this will ensure the 
//		// grab the profile descriptor
//		// now grab the parent profile descriptor
//		// now grab the blocks
//		// now grab the parent block
//		// now grab the referred to blocks
//		
//		// first pass grab the profiles and then manage the declared settings
//		
//		return profile;
//		
//	}
//	
//	public Block getConfigStartBlock( String theProfile, String theBlock ) {
//		return this.profiles.get( theProfile ).getBlock( theBlock );
//		
//	}
//
////	private void prepareProfile( ConfigDescriptor theConfigDescriptor, ProfileDescriptor theProfileDescriptor ) {
////		
////		Profile parentProfile;
////		Profile profile;
////		
////		parentProfile = this.profiles.get( theProfileDescriptor.getParent( ) );
////		if( parentProfile == null && theProfileDescriptor.getParent() != null ) {
////			// this means profile is in this config file or this is a bad file
////			ProfileDescriptor parentProfileDescriptor = theConfigDescriptor.getProfile( theProfileDescriptor.getParent( ) );
////			Conditions.checkConfiguration( parentProfileDescriptor != null, "Profile '%s' indicates it has extends profile '%s' but the profile could not be found.", theProfileDescriptor.getName( ), theProfileDescriptor.getParent( ) );
////			// so now we need to prepare this profile since it hasn't happened yet
////			// TODO: we need to make sure we don't do this twice so if we do a look-up, it has to be from ones just from this file
////			//       OR we make prepare profile return the profile
////			prepareProfile( theConfigDescriptor, parentProfileDescriptor );
////		}
////
////		profile = new Profile( theProfileDescriptor.getName(), theProfileDescriptor.getDescription( ), parentProfile );
////		Conditions.checkConfiguration( !this.profiles.containsKey( theProfileDescriptor.getName( ) ), "A profile with name '%s' already exists", theProfileDescriptor.getName() );
////
////		// now we look at the blocks
////		for( BlockDescriptor blockDescriptor : theProfileDescriptor.getBlocks( ) ) {
////			profile.addDeclaredBlock( prepareBlock( profile, blockDescriptor ) );
////		}
////		
////		// now we save the profile
////		this.profiles.put( profile.getName( ), profile );
////	}
//	
//	private Block prepareBlock( BlockDescriptor theBlockDescriptor, Profile theProfile, ProfileDescriptor theProfileDescriptor ) {
//		// the constructor will validate that the override 
//		// setting makes sense, so we don't here
//		Block block = new Block( 
//				theBlockDescriptor.getName( ), 
//				theBlockDescriptor.getDescription( ), 
//				theBlockDescriptor.isOverride( ),
//				theProfile );
//
//		// this makes sure we haven't previously added,
//		theProfile.addDeclaredBlock( block );
//		
//		// we now look at the includes of the block
//		// so these includes should either
//		// a) exist because they are from the parent profiles
//		// b) exist and loaded from current profile, since we saw it already
//		// c) not yet exist because we haven't loaded yet
//		
//		for( String includeBlockName : theBlockDescriptor.getIncludes() ) {
//			// so first we can see if the block name is found
//			// in the profile descriptor and if so and if not
//			// on the profile itself, it hasn't been processed
//			// so we can process, and if so, we can use it
//			// add to the block for inclusion
//			// if block name is not found on the profile descriptor
//			// then it has to come from the profile and since all 
//			// parent profiles are processed we can ask for it from there
//			
//			if( theProfileDescriptor.hasDeclaredBlock( includeBlockName ) ) {
//				// so the profile has it, but we haven't processed the block yet
//				// so we should process the block
//				if( !theProfile.hasBlock( includeBlockName ) ) {
//					// TODO: I know this will cause problems with the for-loop above that will do a prepare as well
//					//		 if we do settings later, DO NOT prepare here that may be okay that we do nothing here
//					prepareBlock( theProfileDescriptor.getDeclaredBlock( includeBlockName ), theProfile, theProfileDescriptor );
//				}
//			} else {
//				// if the profile descriptor doesn't have it, then we have to
//				// presume the parent process has it
//				Conditions.checkConfiguration( theProfile.getParent( ) != null && theProfile.getParent( ).hasBlock( includeBlockName ), "Block '%s.%s' includes block '%s', but the block could not be found in the local or parent profiles.", theProfile.getName( ), block.getName( ), includeBlockName );
//			}
//		}
//		
//		// we need to look at the 
//		// - includes, which may come from this profile OR parent profiles
//		
//		// now we look at the settings
//		for( SettingDescriptor settingDescriptor : theBlockDescriptor.getSettings( ) ) {
//			// TODO: update this so that it takes the config piece so we know the file this was happening within
//			Conditions.checkConfiguration( settingDescriptor != null, "Block '%s.%s' refers to an empty or missing setting. Check for trailing commas.", theProfile.getName( ), theBlockDescriptor.getName( ) );
//			block.addDeclaredSetting( prepareSetting( theProfile, block, settingDescriptor ) );
//		}
//		return block;
//	}
//	
//	private Setting prepareSetting( Profile theProfile, Block theBlock, SettingDescriptor theSettingDescriptor ) {
//		// we don't do validation here because the constructor does all the validation 
//		// based on the values given and any previous setting found on the profile
//		return new Setting( 
//				theSettingDescriptor.getName(),
//				theSettingDescriptor.getDescription(),
//				theSettingDescriptor.getValue(),
//				theSettingDescriptor.getType(),
//				theSettingDescriptor.isOverride(),
//				theSettingDescriptor.isSensitive(),
//				theBlock,
//				theProfile );
//		
//	}
}
