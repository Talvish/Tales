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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.system.Conditions;

// TODO: it would be nice to have a warning if a block includes something the parent block has already included

/**
 * Represents the stored the version of the profile.
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.profile_descriptor")
public class ProfileDescriptor {
	public static final String NAME_VALIDATOR = "tales.configuration.hierarchical.profile_name";
	
	static {
		if( !NameManager.hasValidator( NAME_VALIDATOR ) ) {
			NameManager.setValidator( NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}
	
   	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	@DataMember( name="extends" )
	private String parentName; 
	@DataMember( name="blocks" )
	private BlockDescriptor[] declaredBlockArray;
	
	
	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private SourceDescriptor declaringSource = null;;
	private ProfileDescriptor parent = null;
	private final Map<String,BlockDescriptor> declaredBlockMap = new HashMap<>( );
	private final Map<String,BlockDescriptor> accessibleBlockMap = new HashMap<>( );
	
	/**
	 * The name given to the profile. 
	 * This is required.
	 * @return the profile name
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The optional description given to the profile.
	 * @return the description of the block
	 */
	public String getDescription( ) {
		return description;
	}

	/**
	 * Indicates if the block was name was declared directly in this profile.
	 * @param theName the name to check for 
	 * @return true if the block was declared in this profile, false otherwise
	 */
	public boolean hasDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a block from profile '%s'.", this.name );
		return declaredBlockMap.containsKey( theName );
	}
	
	/**
	 * Gets the named block from the profile, if declared directly on the profile.
	 * @param theName the name of the block to get
	 * @return the block if declared directly in this profile, null otherwise
	 */
	public BlockDescriptor getDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a block from profile '%s'.", this.name );
		return declaredBlockMap.get( theName );
	}
	
	/**
	 * Returns all of the blocks declared directly within this profile.
	 * @return the list of declared blocks or an empty list if none
	 */
	public Collection<BlockDescriptor> getDeclaredBlocks( ) {
		return declaredBlockMap.values();
	}

	/**
	 * Indicates if the named block is accessible to this profile, meaning
	 * either blocks declared here, or blocks declared in a parent profile.
	 * @param theName the name of the block to get
	 * @return true if accessible, false otherwise
	 */
	public boolean hasAccessibleBlock( String theName ) {
		return this.accessibleBlockMap.containsKey( theName );
	}

	/**
	 * Returns the named block based on the block accessible to this profile, meaning
	 * either blocks declared here, or blocks declared in a parent profile.
	 * @param theName the name of the block to get
	 * @return the block, if found, null otherwise
	 */
	public BlockDescriptor getAccessibleBlock( String theName ) {
		return this.accessibleBlockMap.get( theName );
	}
	
	/**
	 * These are the block descriptors that are available locally or from parents.
	 * These DO NOT take into account how sub-profiles may override these
	 * though does account for block overrides from this profile and any parents.
	 * @return The accessible blocks. 
	 */
	public Collection<BlockDescriptor> getAccessibleBlocks( ) {
		return accessibleBlockMap.values( );
	}

	/**
	 * Returns the parent profile of this profile, if a parent exists.
	 * @return the parent profile, or null if a parent doesn't exist
	 */
	public ProfileDescriptor getParent( ) {
		return parent;
	}

	/**
	 * Returns the source that the profile was declared within.
	 * @return he source
	 */
	public SourceDescriptor getDeclaringSource( ) {
		return declaringSource;
	}
	
	/**
	 * Helper method that helps prepare and validate the profile and blocks for getting settings.
	 * @param theManager the manager containing the other profiles that may be needed
	 * @param theProfileStack the stack used to manage cycles in the profile includes
	 */
	protected void initialize( SourceManager theManager, Deque<String> theProfileStack ) {
		// we check to see if we have already processed this, if so, then
		// we don't need to do it again, there may be a better way to do this
		// with state, but this certainly works too 
		if( this.accessibleBlockMap.size( ) == 0 ) {
		
			// first we make sure we haven't profile already, meaning we have a loop in the 
			Conditions.checkConfiguration( !theProfileStack.contains( name ), "Profile '%s' surfaced an includes cycle of '%s'.", name, String.join( " -> ", theProfileStack )  );
			
			// we put our name on the stack to prevent cycles
			theProfileStack.addLast( name ); 
			
			// we grab from the parents and process it, if available, since it will give us
			// all base blocks that can be overridden, these blocks are stored locally
			// and at the time of the call the accessible blocks will not yet be set
			if( parentName != null ) {
				this.parent = theManager.getProfile( parentName );
				Conditions.checkConfiguration( parent != null, "Could not find parent '%s' for profile '%s'.", parentName, name );
				parent.initialize( theManager, theProfileStack );
				// if we ever support multiple parents then we would need to look at the
				// parents blocks and decide how we are going to merge the block, 
				// conflict would likely be okay as long as declared block existed 
				this.accessibleBlockMap.putAll( parent.accessibleBlockMap );
			}
			
			// next we grab from our blocks, at this point we can check the overrides
			// situation at least since they should be in the list of blocks already
			for( Entry<String,BlockDescriptor> blockEntry : declaredBlockMap.entrySet() ) {
				boolean isOverride = blockEntry.getValue().isOverride();
				BlockDescriptor parentBlock = accessibleBlockMap.get( blockEntry.getKey( ) );
				String previousProfileName = parentBlock == null ? "<old>" : parentBlock.getDeclaringProfile().getName(); // this is for exceptional conditions (and makes sure exceptions aren't thrown in other cases)
				
				// if we have a block that says it doesn't override then a previous block must not exist but
				// if we have a block that says it does override then a previous block must exist 
				Conditions.checkConfiguration( !isOverride || parentBlock != null, "Declared block '%s.%s' is marked as an override but the block doesn't exist in parent profiles.", name, blockEntry.getKey( ) );  
				Conditions.checkConfiguration( isOverride || parentBlock == null, "Declared block '%s.%s' isn't marked as an override but the block exists in parent profile '%s'.", name, blockEntry.getKey(), previousProfileName );  

				// we then put this block into the block list and it will overwrite what is there 
				// to indicate it is the most relevant block of this name
				this.accessibleBlockMap.put( blockEntry.getKey(), blockEntry.getValue( ) );
			}
			
			// now that all the local blocks are in we can now look to validate the 
			// includes that were used on the local blocks since any block mentioned
			// should now exist in the block map ... 
			// this doesn't detect block cycles nor does it detect anything related to 
			// settings such as overrides, settings existing in more than one block
			// with or without an override, etc. .. that happens later

			// we do a bit of validation on the block entries
			for( Entry<String,BlockDescriptor> blockEntry : declaredBlockMap.entrySet() ) {
				for( String includedBlock : blockEntry.getValue( ).getIncludes( ) ) {
					Conditions.checkConfiguration( !blockEntry.getKey().equals( includedBlock ), "Declared block '%s.%s' is attempting to include itself.", name, blockEntry.getKey() );
					Conditions.checkConfiguration( accessibleBlockMap.containsKey( blockEntry.getKey() ), "Declared block '%s.%s' includes an unknown block '%s'.", name, blockEntry.getKey(), includedBlock );
				}
			}
			
			// remove ourselves from cycle handling 
			theProfileStack.removeLast( );
		}
	}

	/**
	 * Resolves all settings and any overrides using this profile as the root context for blocks.
	 * This will do additional validation to ensure there are no block cycles, proper setting  
	 * overrides, etc.
	 * @param theBlockName the name of the block acting as the entry point
	 * @return the available settings or empty list if not are available
	 */
	public Map<String,Setting> extractSettings( String theBlockName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBlockName ), "Block name not sent when looking to extract settings from profile '%s'.", this.name );
		Conditions.checkConfiguration( hasAccessibleBlock( theBlockName ),  "Block '%s' is not an accessible block on profile '%s'.", theBlockName, this.name );
		
		BlockDescriptor rootBlock = this.accessibleBlockMap.get( theBlockName );
		Conditions.checkConfiguration( !rootBlock.isDeferred(), "Block '%s.%s' is marked deferred and therfore cannot be directly used as a source of configuration settings.", this.name, theBlockName );
		
		Map<String, Setting> settings = extractSettings( this, rootBlock, rootBlock, new ArrayDeque<String>( ), false );
		
		// now we make sure things are fully validated
		for( Setting setting : settings.values( ) ) {
			setting.validate();
		}
		
		return settings;
	}
	
	/**
	 * A helper recursive method that retrieves the set of available settings for the given.
	 * It is recursive since it needs to use parent profiles to find all available blocks
	 * and their settings.
	 * @param theRootProfile the root profile context for getting settings, this is what is used to resolve blocks
	 * @param theBlock the block to be extracting settings for
	 * @param theBlockStack the block stack to look for cycles in the blocks
	 * @param bypassCheck indicates if cycle checks should be passed (which happens if dealing with a parent block)
	 * @return the extracted settings or an empty list of none are available
	 */
	private Map<String,Setting> extractSettings( ProfileDescriptor theRootProfile, BlockDescriptor theRootBlock, BlockDescriptor theBlock, Deque<String> theBlockStack, boolean bypassCheck ) {
		Map<String,Setting> blockSettings = new HashMap<>( );
		
		// first we make sure we don't have a cycle in the blocks meaning we have a loop in the blocks and processed this block already 
		// we by-pass this cycle check if we are currently processing a parent of a block (since it has the same name)
		if( !bypassCheck ) {
			Conditions.checkConfiguration( !theBlockStack.contains( theBlock.getName( ) ), "Block '%s.%s' surfaced an includes cycle of '%s'. Occurred while processing root block '%s.%s'.",
					theBlock.getDeclaringProfile().getName( ),
					theBlock.getName(), 
					String.join( " -> ", theBlockStack ), 
					theRootProfile.getName( ),
					theRootBlock.getName( ) );
			theBlockStack.addLast( theBlock.getName( ) );
		}
		
		// first we will look at the overridden block (if this current block is marked as an override)
		// to get the parent we need to find parent profile of the block and ask it to get 
		// the block, which means it may need to traverse its own parents to get the block
		if( theBlock.getDeclaringProfile().getParent( ) != null && theBlock.isOverride() ) {
			// now we need to get the block descriptor that we are overriding
			BlockDescriptor parentBlock = theBlock.getDeclaringProfile().getParent( ).getAccessibleBlock( theBlock.getName() );
			// now, since these should be empty, we can send the settings right to the extract
			Map<String,Setting> parentSettings  = extractSettings( theRootProfile, theRootBlock, parentBlock, theBlockStack, true );
			// for the purpose of the validation we put the parent settings into
			// the included settings to make sure that we compare against them, in
			// particular for any overrides and sometimes when people do a 're-include'
			// of a block
			blockSettings.putAll( parentSettings );
		}
		
		// right now we are looking to see if any settings from the
		// includes conflict with other settings from other includes
		// if so, then there must be a setting in the passed in 
		// block to disambiguate
		for( String includedBlockName : theBlock.getIncludes( ) ) {
			// get the block in question (which resolves via the root, since different profiles may have different blocks for the same block name)
			BlockDescriptor includedBlock =  theRootProfile.accessibleBlockMap.get( includedBlockName ); 
			// this should have been previously validated, but we do this to be safe
			Conditions.checkConfiguration( includedBlock != null, "Block '%s.%s' could not find included block '%s'. Occurred while processing root block '%s.%s'.", 
					theBlock.getDeclaringProfile().getName( ), 
					theBlock.getName(), 
					includedBlockName, 
					theRootProfile.getName( ),
					theRootBlock.getName( ) );
			
			// we now extract the settings for the included block
			Map<String,Setting> includedBlockSettings = extractSettings( theRootProfile, theRootBlock, includedBlock, theBlockStack, false );
			// but we make sure these settings make sense in context of settings from other included blocks
			for( Setting setting : includedBlockSettings.values( ) ) {
				// so either a) the setting isn't in our current list of included settings OR
				// b) it is the same setting we already included/from parent (which is fine) OR 
				// c) it is a setting that the current block has an override for (though we don't check 
				// for the override flag right now, just that exists ... override check is later)
				Setting existingSetting = blockSettings.get( setting.getName( ) );
				
				if( existingSetting == null ) {
					blockSettings.put( setting.getName(), setting );
				} else {
					existingSetting.addSibling( setting );
				} 
			}
		}
		
		// now that we have dealt with the included blocks, we now look at the settings
		// on the passed in block
		for( SettingDescriptor setting : theBlock.getDeclaredSettings( ) ) {
			// so now we look at the settings we have here and make sure that if a setting says it is
			// overriding that it is in fact overriding something and if it doesn't think it is
			// then there isn't an existing name ... this is all handled by the
			// Setting class constructor and addOverride methods
			
			Setting existingSetting = blockSettings.get( setting.getName( ) );
			if( existingSetting != null ) {
				existingSetting.addOverride( setting );
			} else {
				blockSettings.put( setting.getName( ), new Setting( setting, theRootProfile.getName( ), theRootBlock.getName( ) ) );
			}
		}
		
		if( !bypassCheck ) {
			theBlockStack.removeLast( );
		}
		return blockSettings;
	}
	
	/**
	 * Method called when this descriptor is deserialized. It occurs
	 * after all descriptors from a given source have been loaded
	 * but before all included sources are loaded.
	 * @param theDeclaringSource the source that profile was declared within
	 */
	protected void onDeserialized( SourceDescriptor theDeclaringSource ) {
		Preconditions.checkArgument( theDeclaringSource != null, "Profile '%s' is getting source set to null.", name ); // if name is blank, this will be null
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( name ), "A profile without a name was loaded from source '%s'.", theDeclaringSource.getSourcePath() );
		Preconditions.checkState( declaringSource == null, "Profile '%s' from source '%s' is getting source reset to '%s'.", name, declaringSource == null ? "<empty>" : declaringSource.getSourcePath( ), theDeclaringSource.getSourcePath() );
		NameValidator nameValidator = NameManager.getValidator( NAME_VALIDATOR );
		Conditions.checkConfiguration( nameValidator.isValid( name ), String.format( "Profile name '%s' from source '%s' does not conform to validator '%s'.", name, theDeclaringSource.getSourcePath( ), nameValidator.getClass().getSimpleName() ) );
		
		Conditions.checkConfiguration( !name.equals( this.parentName ), "Profile '%s' is attempting to use itself as a parent.", name );
		
		declaringSource = theDeclaringSource;
		
		if( declaredBlockArray != null ) {
			for( BlockDescriptor block : declaredBlockArray ) {
				Conditions.checkConfiguration( block != null, "Profile '%s' from config '%s' is attempting to include a missing block (check source for trailing commas, etc).", name, declaringSource.getSourcePath( ) );
				Conditions.checkConfiguration( !declaredBlockMap.containsKey( block.getName( ) ), "Profile '%s' from config '%s' is attempting to add block '%s' more than once.", name, declaringSource.getSourcePath(), block.getName( ) );
				declaredBlockMap.put( block.getName( ), block );
				block.onDeserialized( this );
			}
		}
	}
}
