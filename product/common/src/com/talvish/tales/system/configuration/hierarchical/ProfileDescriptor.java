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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.system.Conditions;

/**
 * Represents the stored the version of the profile.
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.profile_descriptor")
public class ProfileDescriptor {
	private static final Logger logger = LoggerFactory.getLogger( Manager.class );

	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	@DataMember( name="extends" )
	private String parent;
	@DataMember( name="blocks" )
	private BlockDescriptor[] declaredBlockArray;
	
	// TODO: maybe put a state on this regarding what has been done to it
	//		 so we don't have to do the work again later (as we process other profiles)
	
	// non-persisted data, mostly which is for aiding debugging, problem finding or easing data grabbing
	private ConfigDescriptor configDescriptor;
	private final Map<String,BlockDescriptor> declaredBlockMap = new HashMap<>( );
	private final Map<String,BlockDescriptor> accessibleBlockMap = new HashMap<>( );
	
	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}

	public boolean hasDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a block descriptor from profile descriptor '%s'.", this.name );
		return declaredBlockMap.containsKey( theName );
	}
	
	
	public BlockDescriptor getDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Need a name to get a block descriptor from profile descriptor '%s'.", this.name );
		return declaredBlockMap.get( theName );
	}
	
	public Collection<BlockDescriptor> getDeclaredBlock( ) {
		return declaredBlockMap.values();
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

	public String getParent( ) {
		return parent;
	}
	
	
	public void validatePhaseOne( Manager theManager, Deque<String> theProfileStack ) {
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
			if( parent != null ) {
				ProfileDescriptor parentDescriptor = theManager.getProfileDescriptor( parent );
				Conditions.checkConfiguration( parentDescriptor != null, "Could not find parent '%s' for profile '%s'.", parent, name );
				parentDescriptor.validatePhaseOne( theManager, theProfileStack );
				// if we ever support multiple parents then we would need to look at the
				// parents blocks and decide how we are going to merge the block, 
				// conflict would likely be okay as long as declared block existed 
				this.accessibleBlockMap.putAll( parentDescriptor.accessibleBlockMap );
			}
			
			// next we grab from our blocks, at this point we can check the overrides
			// situation at least since they should be in the list of blocks already
			for( Entry<String,BlockDescriptor> blockEntry : declaredBlockMap.entrySet() ) {
				boolean isOverride = blockEntry.getValue().isOverride() == null ? false : blockEntry.getValue( ).isOverride();
				BlockDescriptor previousBlock = accessibleBlockMap.get( blockEntry.getKey( ) );
				String previousProfileName = previousBlock == null ? "<old>" : previousBlock.getProfileDescriptor().getName(); // this is for exceptional conditions (and makes sure exceptions aren't thrown in other cases)
				
				// if we have a block that says it doesn't override then a previous block must not exist but
				// if we have a block that says it does override then a previous block must exist 
				Conditions.checkConfiguration( !isOverride || previousBlock != null, "Declared block '%s.%s' is marked as an override but the block doesn't exist in parent profiles.", name, blockEntry.getKey( ) );  
				Conditions.checkConfiguration( isOverride || previousBlock == null, "Declared block '%s.%s' isn't marked as an override but the block exists in profile '%s'.", name, blockEntry.getKey(), previousProfileName );  

				logger.info( "Making block '{}' accessible to profile '{}'.", blockEntry.getKey(), this.name );
				
				// we then put this block into the block list and it will overwrite what is there 
				// to indicate it is the most relevant block of this name
				this.accessibleBlockMap.put( blockEntry.getKey(), blockEntry.getValue( ) );
			}
			
			// now that all the local blocks are in we can now look to validate the 
			// includes that were used on the local blocks since any block mentioned
			// should now exist in the block map ... 
			// this doesn't detect block cycles nor does it detect anything related to 
			// settings such as overrides, settings existing in more than one block
			// with or without an override

			// we will need to do that step afterward
			// and probably the way we need to do that is to have each included block
			// return it's combined settings and then the block that combined
			// takes a look at them, sees duplicates with no way to decide if no override, etc
			// something else we need to look at is that there can be more than one includes
			// ... a super block can include and then a sub block can include different stuff
			// and that is okay though we may want to detect his to indicate it was already
			// included
			// this may mean we process the same blocks over and over again but they will be 
			// under different contexts
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

	public void extractSettings( ProfileDescriptor theRoot ) {
		for( BlockDescriptor blockDescriptor : accessibleBlockMap.values( ) ) {
			Map<String, SettingDescriptor> settings = new HashMap<String, SettingDescriptor>( );
			extractSettings( theRoot, blockDescriptor, settings, new ArrayDeque<String>( ) );
		}
	}

	public void extractSettings( ProfileDescriptor theRoot, String theBlockName, Map<String, SettingDescriptor> theSettings ) {
		extractSettings( theRoot, theRoot.accessibleBlockMap.get( theBlockName ), theSettings, new ArrayDeque<String>( ) );
	}

	
	private void extractSettings( ProfileDescriptor theRoot, BlockDescriptor theBlockDescriptor, Map<String,SettingDescriptor> theSettings, Deque<String> theBlockStack ) {
		// first we make sure we haven't profile already, meaning we have a loop in the 
		Conditions.checkConfiguration( !theBlockStack.contains( theBlockDescriptor.getName( ) ), "Block '%s.%s' surfaced an includes cycle of '%s' while processing profile '%s' for root profile '%s'.", theBlockDescriptor.getProfileDescriptor().getName( ), theBlockDescriptor.getName(), String.join( " -> ", theBlockStack ), name, theRoot.getName( ) );
		
		theBlockStack.addLast( theBlockDescriptor.getName( ) );
		
		// we now need to look at the includes
		for( String includedBlockName : theBlockDescriptor.getIncludes( ) ) {
			Map<String,SettingDescriptor> includedSettings = new HashMap<>( );
			BlockDescriptor includedBlock =  theRoot.accessibleBlockMap.get( includedBlockName ); 
			// this should have been previously validated, but we do this to be safe
			Conditions.checkConfiguration( includedBlock != null, "Block '%s.%s' could not load included block '%s' while processing profile '%s' for root profile '%s'.", theBlockDescriptor.getProfileDescriptor().getName( ), theBlockDescriptor.getName(), includedBlock, name, theRoot.getName( ) );
		
			// so we now extract the settings for the included block
			extractSettings( theRoot, includedBlock, includedSettings, theBlockStack );
			// but we make sure these settings make sense in context of previously extract settings
			for( SettingDescriptor settingDescriptor : includedSettings.values( ) ) {
				// so either the setting isn't in our current list of settings OR if it is
				// then the current block MUST have it ... we dont' check other information
				// like type consistency or if it has an override, that will happen later
				SettingDescriptor existingSettingDescriptor = theSettings.get( settingDescriptor.getName( ) );
				// TODO: consider how the parent block will work here
				Conditions.checkConfiguration( 
						existingSettingDescriptor == null || ( theBlockDescriptor.hasDeclaredSettingDescriptor( settingDescriptor.getName() ) ), 
						"Block '%s.%s' found setting '%s' on included block '%s.%s' but other include block '%s.%s' already has the setting but an override is not available to resolve the ambiguity. This was found while processing profile '%s' for root profile '%s'.", 
						theBlockDescriptor.getProfileDescriptor().getName( ), 
						theBlockDescriptor.getName(), 
						settingDescriptor.getName(),
						includedBlock.getProfileDescriptor().getName(),
						includedBlock.getName(),
						existingSettingDescriptor == null ? "<empty>" : existingSettingDescriptor.getBlockDescriptor().getProfileDescriptor().getName(),
						existingSettingDescriptor == null ? "<empty>" : existingSettingDescriptor.getBlockDescriptor().getName(),
						name, 
						theRoot.getName( ) );
				
				// even though it is valid for there to be an existing setting, 
				// we only added the setting from the included block to make 
				// sure we take the first one, which, assuming we process the 
				// parent first, will be the parent 
				// TODO: for consideration here is that there can be different types, so it definitely makes it more interesting (or we decide to ignore type)
				if( existingSettingDescriptor == null ) {
					theSettings.put( settingDescriptor.getName( ), settingDescriptor );
				}
			}
		}
		
		// now that we have dealt with the included blocks, we now look at the settings
		// on the passed in block
		for( SettingDescriptor settingDescriptor : theBlockDescriptor.getDeclaredSettingDescriptors( ) ) {
			logger.info( "Looking at setting '{}' for block '{}.{}' while processing profile '{}' for root profile '{}' ... '{}.{}'.", 
					settingDescriptor == null ? "<null>" : settingDescriptor.getName(), 
					theBlockDescriptor.getProfileDescriptor().getName( ), 
					theBlockDescriptor.getName(),
					name,
					theRoot == null ? "<root>" : theRoot.getName( ),
					settingDescriptor.getBlockDescriptor() == null ? "<null>" : settingDescriptor.getBlockDescriptor().getProfileDescriptor().getName(),
					settingDescriptor.getBlockDescriptor() == null ? "<null>" : settingDescriptor.getBlockDescriptor().getName() );
			// so now we look at the settings we have here and make sure that if a setting says it is
			// overriding that it is in fact overriding something and if it doesn't think it is
			// then there isn't an existing name
			
			SettingDescriptor existingSettingDescriptor = theSettings.get( settingDescriptor.getName( ) );
			boolean isOverride = settingDescriptor.isOverride() == null ? false : settingDescriptor.isOverride();

			Conditions.checkConfiguration( !isOverride || existingSettingDescriptor != null, "Setting '%s' from block '%s.%s' is marked as an override but the setting doesn't exist in parent or included blocks. This was found while processing profile '%s' for root profile '%s'.",
					settingDescriptor.getName(),
					settingDescriptor.getBlockDescriptor().getProfileDescriptor().getName(),
					settingDescriptor.getBlockDescriptor().getName(),
					name,
					theRoot.getName( ) );
			Conditions.checkConfiguration( isOverride || existingSettingDescriptor == null, "Setting '%s' from block '%s.%s' isn't marked as an override but the setting was found on block '%s.%s'. This was found while processing profile '%s' for root profile '%s'.",
					settingDescriptor.getName(),
					settingDescriptor.getBlockDescriptor().getProfileDescriptor().getName(),
					settingDescriptor.getBlockDescriptor().getName(),
					existingSettingDescriptor == null ? "<empty>" : existingSettingDescriptor.getBlockDescriptor().getProfileDescriptor().getName(),
					existingSettingDescriptor == null ? "<empty>" : existingSettingDescriptor.getBlockDescriptor().getName(),
					name,
					theRoot.getName( ) );
		
			theSettings.put( settingDescriptor.getName( ), settingDescriptor );
		}
		
		theBlockStack.removeLast( );
	}
	
	public ConfigDescriptor getConfigDescriptor( ) {
		return configDescriptor;
	}
	
	protected void cleanup( ConfigDescriptor theConfigDescriptor ) {
		Preconditions.checkArgument( theConfigDescriptor != null, "Profile descriptor '%s' is getting config descriptor set to null.", name );
		Preconditions.checkState( configDescriptor == null, "Profile descriptor '%s' from config descriptor '%s' is getting config descriptor reset to '%s'.", name, configDescriptor == null ? "<empty>" : configDescriptor.getSource( ), theConfigDescriptor.getSource() );

		Conditions.checkConfiguration( !name.equals( this.parent ), "Profile descriptor '%s' is attempting to use itself as a parent.", name );
		
		configDescriptor = theConfigDescriptor;
		
		if( declaredBlockArray != null ) {
			for( BlockDescriptor blockDescriptor : declaredBlockArray ) {
				Conditions.checkConfiguration( blockDescriptor != null, "Profile descriptor '%s' from config descriptor '%s' is attempting to include a missing block descriptor (check source for trailing commas, etc).", name, configDescriptor.getSource( ) );
				Conditions.checkConfiguration( !declaredBlockMap.containsKey( blockDescriptor.getName( ) ), "Profile descriptor '%s' from config descriptor '%s' is attempting to add block descriptor '%s' more than once.", name, configDescriptor.getSource(), blockDescriptor.getName( ) );
				declaredBlockMap.put( blockDescriptor.getName( ), blockDescriptor );
				blockDescriptor.cleanup( this );
			}
		}
	}
}
