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
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.system.Conditions;
import com.talvish.tales.system.configuration.ConfigurationException;

/**
 * This class manages json-based configuration settings centered around a root source file (and any of its includes).
 * @author jmolnar
 *
 */
public class SourceManager {
	private static final Logger logger = LoggerFactory.getLogger( SourceManager.class );

	private final String rootSource;
	
	private final JsonTranslationFacility jsonFacility;
	private final JavaType sourceType;
	
	private final Map<String,SourceDescriptor> sources = new HashMap<>( );
	private final Map<String,ProfileDescriptor> profiles = new HashMap<>( );
	
	/**
	 * Constructor for getting a source manager off the ground.
	 * @param theRootSource the source file acting as the root for the manager
	 * @param theJsonFacility a json facility to process the json in the source file
	 */
	public SourceManager( String theRootSource, JsonTranslationFacility theJsonFacility ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theRootSource ), "need a filename to create the manage " );
		Preconditions.checkNotNull( theJsonFacility, "need a json translation facility to create the manager" );
		
		jsonFacility = theJsonFacility;
		sourceType = new JavaType( SourceDescriptor.class );
		rootSource = theRootSource;
		
		// first thing we do is load the configuration which loads the main
		// file (and then include files) into memory, parses contents and
		// stores into our source and profile descriptor maps
		loadConfiguration( rootSource, new ArrayDeque<>( 2 ) );
		// now for each profile we want to make sure the profiles and
		// blocks are in a good spot
		initializeConfiguration( );
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
	private void loadConfiguration( String theSource, Deque<File> theSourceStack ) {
		logger.info( "Loading configuration from source '{}'.", theSource );

		// we convert to Files so we can ultimately understand how to process the relative paths 
		File parentSourceFile = theSourceStack.peekLast( );
		File sourceFile = new File( parentSourceFile == null ? null : parentSourceFile.getParent(), theSource );

		// first we make sure there isn't a cycle THOUGH this doesn't prevent a profile
		// having a parent that is defined by a config source that is not included by
		// provided by the source including this file 
		Conditions.checkConfiguration( !theSourceStack.contains( sourceFile ), "Configuration source '%s' surfaced an include cycle of '%s'.", theSource, toString( theSourceStack ) );

		// next we see if we have already processed this source in a different part of  
		// the inclusion tree, if so, that is okay we just don't re-process
		if( !sources.containsKey( theSource ) ) {
			// to understand how this works, essentially the order of loading is important
			// the first thing it does is load all of the files and their respective 
			// includes into memory, it then assumes that previously loaded/referenced
			// files are fully loaded and properly validated, it then processes any
			// existing profiles
			
			String contents;
			SourceDescriptor source;

			// we load the file's content
			contents = getSourceContents( sourceFile, theSourceStack );
			// once loaded, we are we add to the stack to detect cycles 
			theSourceStack.addLast( sourceFile );
			// then turn the file contents into an in memory representation
			source = jsonFacility.fromJsonString( contents, sourceType );
			// now we do a bit of cleanup (which traverses all descriptors)
			source.onDeserialized( theSource );

			// we store a map of the sources for debugging purposes
			sources.put( theSource, source );
	
			// after loading the file we pull out the includes and process them
			// since the includes will have blocks and profiles needed 
			for( String include : source.getIncludes( ) ) {
				Conditions.checkConfiguration( !Strings.isNullOrEmpty( include ), "Configuration source '%s' included an empty or missing source. Check for trailing commas.", theSource );
				logger.info( "Configuration file '{}' includes additional configuration source '{}'.", theSource, include );
				loadConfiguration( include, theSourceStack );
			}
			
			// we need to store all of the profile descriptors, since we may need them
			// this will also be used to verify that we don't get more than one profile
			// with the same name
			for( ProfileDescriptor profile : source.getDeclaredProfiles( ) ) {
				Conditions.checkConfiguration( profile != null, "Configuration source '%s' refers to an empty or missing profile source. Check for trailing commas.", theSource );
				
				ProfileDescriptor existingProfile = profiles.get( profile.getName( ) );
				String existingSource = existingProfile == null ? "<existing>" : existingProfile.getDeclaringSource().getSourcePath(); // this is done this way to aid debugging, exceptional conditions
				Conditions.checkConfiguration( !profiles.containsKey( profile.getName( ) ), "Profile '%s' from '%s' has the same name as an existing profile from '%s'.", profile.getName(), theSource, existingSource );
				
				profiles.put( profile.getName(), profile );
			}
			
			// we remove ourselves from cycle handling
			theSourceStack.removeLast( );
		}
	}

	
	/**
	 * This helper method loads into memory the contents of the specified file. 
	 * @param theSourceFile the file to load
	 * @param theSourceStack gives context for messages in exceptions
	 * @return the contents of the specified file
	 */
	private String getSourceContents( File theSourceFile, Deque<File> theSourceStack ) {
	    int readLength;
	    char[] buffer = new char[ 4096 ];
	    StringBuilder contents = new StringBuilder();
	    FileReader reader = null;
	    
		try {
			reader = new FileReader( theSourceFile );
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
			throw new ConfigurationException( String.format( "Could not find the source '%s' while traversing/loading the configuration source stack '%s'.", theSourceFile.getPath( ), toString( theSourceStack ) ), e );
		} catch( IOException e ) {
			throw new ConfigurationException( String.format( "Unknown I/O error reading configuration source '%s' while traversing/loading the configuration source stack '%s'.", theSourceFile.getPath( ), toString( theSourceStack ) ), e );
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
	 * Given the source stack, returns a string version of the stack.
	 * @param theSourceStack the source stack to stringify
	 * @return the source stack as a string
	 */
	private String toString( Deque<File> theSourceStack ) {
		StringBuilder list = new StringBuilder( );
		boolean first = true;
		
		for( File file : theSourceStack ) {
			if( !first ) {
				list.append( " -> " );
			}
			list.append( file.getPath( ) );
			first = false;
		}
		return list.toString( );
	}
	
	/**
	 * This is called by the constructor to make sure our profiles and blocks are setup correctly.
	 */
	private void initializeConfiguration( ) {
		Deque<String> profileStack = new ArrayDeque<String>( profiles.size( ) );
		for( ProfileDescriptor profileDescriptor : profiles.values() ) {
			Preconditions.checkState( profileStack.size() == 0, "The profile stack wasn't cleared." );
			profileDescriptor.initialize( this, profileStack );
		}
	}
	
	/**
	 * Gets the profile descriptor for a given profile.
	 * @param theName the name of the profile to get
	 * @return the profile, or null if not found
	 */
	protected ProfileDescriptor getProfile( String theName ) {
		return this.profiles.get( theName );
	}

	/**
	 * Gets the settings associated with the particular profile and block. This will  
	 * do additional validation to ensure there are no block cycles, proper setting  
	 * overrides, etc.
	 * An exception is thrown if the profile or block don't exist.
	 * @param theProfile the profile to base the configuration from
	 * @param theBlock the block the getting the settings from
	 * @return the settings or an empty map if there aren't any
	 */
	public Map<String,Setting> extractSettings( String theProfile, String theBlock ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theProfile ), "Cannot extract settings using a null/empty profile." );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBlock ), "Cannot extract settings on profile '%s' using a null/empty block.", theProfile );
		
		ProfileDescriptor profile = this.profiles.get( theProfile );
		Preconditions.checkArgument( profile != null, "Could not find profile '%s'.", theProfile );
		
		Map<String, Setting> settings = profile.extractSettings( theBlock );

		// now we make sure things are fully validated
		for( Setting setting : settings.values( ) ) {
			setting.validate();
		}
		
		return settings;
	}
	
	/**
	 * This is a debugging/logging method that logs the blocks that declared a setting.
	 * @param theSetting the setting to log
	 */
	@SuppressWarnings("unused")
	private void logSetting( Setting theSetting ) {
		logger.info( "Logging setting '{}' ...", theSetting.getName( ) );
		
		logSetting( theSetting.getHistory( ), 1 );
	}
	
	/**
	 * This is a recursive debugging/logging method that logs the blocks that declared a setting.
	 * @param theNode the node of the tree to log
	 * @param theDepth the depth in the tree so we can visually show where we are
	 */
	private void logSetting( SimpleTreeNode<SettingDescriptor> theNode, int theDepth ) {
		
		if( theNode.getValue() != null ) {
			StringBuilder builder = new StringBuilder( );
			for( int count = 0; count < theDepth; count += 1) {
				builder.append( "---- " );
			}
			builder.append( theNode.getValue().getDeclaringBlock().getDeclaringProfile().getName( ) );
			builder.append( "." );
			builder.append( theNode.getValue().getDeclaringBlock().getName( ) );
			builder.append( " (" );
			builder.append( theNode.getChildren().size( ) );
			builder.append( ")" );
			logger.info( builder.toString( ) );
			
			for( SimpleTreeNode<SettingDescriptor> child : theNode.getChildren() ) {
				logSetting( child, theDepth + 1 );
			}
		} else {
			for( SimpleTreeNode<SettingDescriptor> child : theNode.getChildren() ) {
				logSetting( child, theDepth ); // since is same depth
			}
		}
	}
}
