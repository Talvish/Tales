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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Conditions;

/**
 * This class represents the setting that is ultimately served by the
 * hierarchical source manager to the config manager. It is also one
 * of the main ways setting validation/conflict tracking occurs. 
 * The class stores a history of how a particular setting is referenced.
 * @author jmolnar
 *
 */
public class Setting {
	private static final Logger logger = LoggerFactory.getLogger( SourceManager.class );

	/**
	 * Format string to help create the format strings used by the configuration manager.
	 */
	public static final String SOURCE_NAME_FORMAT = "[%s].[%s].[%s]";

	private String name;
	private String description;
	private String value;
	private boolean sensitive;
	
	private boolean deferred;
	
	private String rootProfile;
	private String rootBlock;
	
	private String sourceName;
	
	private SimpleTreeNode<SettingDescriptor> history;

	/**
	 * Constructor taking the required data to get a setting off the ground.
	 * It provides the first piece of history for how this setting came to be.
	 * This is only called when creating a leaf in the history. 
	 * @param theSetting the initial descriptor for the setting 
	 */
	public Setting( SettingDescriptor theSetting, String theRootProfile, String theRootBlock ) {
		Preconditions.checkArgument( theSetting != null, "Attempting to create a setting without a setting descriptor." );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theRootProfile ), "Attempting to create setting '%s' without a root profile.", theSetting.getName() );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theRootBlock ), "Attempting to create setting '%s' without a root block.", theSetting.getName() );
		// given this is a leaf node in the history this cannot be an override
		Conditions.checkConfiguration( !theSetting.isOverride(), "Setting '%s' from block '%s.%s' indicates it is an override but hasn't overridden anything. Occurred while processing root block '%s.%s'.", 
				theSetting.getName( ), 
				theSetting.getDeclaringBlock().getDeclaringProfile().getName(), 
				theSetting.getDeclaringBlock().getName( ),
				theRootProfile,
				theRootBlock );
		
		name = theSetting.getName();
		description = theSetting.getDescription();
		value = theSetting.getValue();
		deferred = theSetting.isDeferred();
		sensitive = theSetting.isSensitive( ) == null ? false : theSetting.isSensitive().booleanValue();

		rootProfile = theRootProfile;
		rootBlock = theRootBlock;
		
		history = new SimpleTreeNode<SettingDescriptor>( theSetting );

	}

 	/**
	 * The name given to the setting.
	 * This is required.
	 * @return
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The description given to the setting.
	 * @return the description of the block
	 */
	public String getDescription( ) {
		return description;
	}
	
	/**
	 * The value given to the setting.
	 * @return the value of the setting
	 */
	public String getValue( ) {
		return value;
	}
	
	/**
	 * Indicates if this setting must be overridden.
	 * @return true if it must be overridden, false otherwise
	 */
	public boolean isDeferred( ) {
		return this.deferred;
	}	

	/**
	 * Indicates if this setting is consider sensitive. If sensitive the intention is for the setting value to
	 * not be dumped into log files or be made generally visible.
	 * @return true means  
	 */
	public boolean isSensitive( ) {
		return sensitive;
	}
	
	/**
	 * A string version of the specific source, profile and block that sourced the setting.
	 * This is only set once the setting has been validated.
	 * @return the source name
	 */
	public String getSourceName( ) {
		return sourceName;
	}
	
	/**
	 * Indicates that a block included another block that had this setting already.
	 * This method only takes the history information to carry forward to this setting.
	 * By adding as a sibling it will make sure proper conflict handling occurs. 
	 * @param theSetting the setting to add as a sibling
	 */
	public void addSibling( Setting theSetting ) {
		Preconditions.checkArgument( theSetting != null, "Attempting to add a sibling to setting '%s' without the setting.", this.name );

		Conditions.checkConfiguration( this.sensitive && theSetting.isSensitive() || !this.sensitive, "Setting '%s' (from one of the blocks in %s) indicates it is not sensitive but the setting it is overriding (from one of the blocks in %s) indicates it must be. Occurred while processing root block '%s.%s'.",
				theSetting.getName( ), 
				theSetting.getBlockHistory( ),
				this.getBlockHistory(),
				rootProfile,
				rootBlock );
		
		// since we are doing this for conflict handling, we only add a sibling (meaning 
		// potential conflict) if we are adding something that is not already a sibling ...
		// we use reference equals for checking if already a sibling because all instances 
		// of a setting (belonging to a profile/block) are created only once
		if( history.getValue( ) != null ) {
			// if the setting has a setting descriptor (the value on the 
			// tree node) then we need to create a new tree node and make 
			// them siblings (meaning they MAY conflict) ... BUT we only 
			// do this if the setting descriptors are different
			// if they are the same, there is no conflict
			if( theSetting.getHistory().getValue( ) != null && history.getValue() != theSetting.getHistory().getValue( ) ) {
				SimpleTreeNode<SettingDescriptor> newHistory = new SimpleTreeNode<>( );
				newHistory.addChild( history );
				newHistory.addChild( theSetting.getHistory( ) );
				history = newHistory;
			}
		} else {
			// if the setting doesn't have a setting descriptor
			// directly then we can make this a child directly
			// ... BUT we only do this one of the siblings aren't
			// the same setting descriptor (since the potential
			// conflict is already covered)
			boolean found = false;
			for( SimpleTreeNode<SettingDescriptor> node : history.getChildren( ) ) {
				if( node.getValue() != null && node.getValue() == theSetting.getHistory().getValue( ) ) {
					found = true;
					break;
				}
			}
			if( !found ) {
				history.addChild( theSetting.getHistory( ) );
			}
		}
	}
	
	/**
	 * Adds a setting descriptor as an override for any current setting descriptor in the history.
	 * This occurs when a block finds a declared setting BUT one of the included blocks also
	 * had this setting.
	 * @param theSetting the overriding setting descriptor to add to the history
	 */
	public void addOverride( SettingDescriptor theSetting ) {
		Preconditions.checkArgument( theSetting != null, "Attempting to place an override on setting '%s' without a setting descriptor.", this.name );
		// given this is an override it means that the setting descriptor passed in must indicate it is an override
		// we also know this setting has a history set because a setting cannot be created without a history
		String blockHistory = getBlockHistory( ); // putting here since Java will call these in the condition calls all the time and no point in generating more than once
		Conditions.checkConfiguration( theSetting.isOverride( ), "Setting '%s' from block '%s.%s' indicates it is not an override but the setting was found, so far, on %s. Occurred while processing root block '%s.%s'.", 
				theSetting.getName( ), 
				theSetting.getDeclaringBlock( ).getDeclaringProfile( ).getName( ), 
				theSetting.getDeclaringBlock( ).getName( ),
				blockHistory,
				rootProfile,
				rootBlock );
		Conditions.checkConfiguration( ( this.sensitive && ( theSetting.isSensitive() == null || theSetting.isSensitive().booleanValue( ) ) ) || !this.sensitive, "Setting '%s' from block '%s.%s' indicates it is not sensitive but the setting it is overriding (from one of the blocks in %s) indicates it must be. Occurred while processing for root block '%s.%s'.",
				theSetting.getName( ), 
				theSetting.getDeclaringBlock( ).getDeclaringProfile( ).getName( ), 
				theSetting.getDeclaringBlock( ).getName( ),
				blockHistory,
				rootProfile,
				rootBlock );
				
		if( history.getValue( ) != null ) {
			history = new SimpleTreeNode<>( theSetting, history );
		} else {
			history.setValue( theSetting );
		}
	
		// since this is an override we can update values on the setting
		
		// we only overwrite the description if the override has one
		if( !Strings.isNullOrEmpty( theSetting.getDescription( ) ) ) {
			this.description = theSetting.getDescription();
		}
		
		// we always overwrite the value
		this.value = theSetting.getValue();
		
		// whatever this block that is overriding says to do, we will do
		this.deferred = theSetting.isDeferred( );
		
		// set the sensitive setting
		if( !this.sensitive ) {
			this.sensitive = theSetting.isSensitive()== null ? false : theSetting.isSensitive();
		}
	}
	
	/**
	 * Returns the history that is associated with this setting.
	 * @return the history
	 */
	public SimpleTreeNode<SettingDescriptor> getHistory( ) {
		return history;
	}
	
	/**
	 * Recursive helper method that returns the set of blocks in the history.
	 * @return the blocks that are in the history of this setting
	 */
	private String getBlockHistory( ) {
		StringBuilder blocks = new StringBuilder();
		
		buildBlockHistory( blocks, history, new ArrayList<String>( ) );
		
		return blocks.toString();
	}
	
	/**
	 * Recursive helper method that returns the set of blocks in the history.
	 * @param theBuilder the builder to append the strings
	 * @param theHistoryNode the current node we are looking
	 * @param foundBlocks the list of found blocks to make sure we report only once
	 */
	private void buildBlockHistory( StringBuilder theBuilder, SimpleTreeNode<SettingDescriptor> theHistoryNode, List<String> foundBlocks ) {
		if( theHistoryNode.getValue() != null ) {
			String blockName = theHistoryNode.getValue( ).getDeclaringBlock().getDeclaringProfile().getName() + "." + theHistoryNode.getValue().getDeclaringBlock().getName();
			if( !foundBlocks.contains( blockName ) ) {
				foundBlocks.add( blockName );
				if( theBuilder.length() > 0 ) {
					theBuilder.append( ", " );
				}
				theBuilder.append( "'" );
				theBuilder.append( blockName );
				theBuilder.append( "'" );
			}
		}
		for( SimpleTreeNode<SettingDescriptor> childHistoryNode : theHistoryNode.getChildren( ) ) {
			buildBlockHistory( theBuilder, childHistoryNode, foundBlocks);
		}
	}
	
	/**
	 * Recursive helper method that returns the set of blocks in the history.
	 * @param theBuilder the builder to append the strings
	 * @param theHistoryNode the current node we are looking
	 * @param foundBlocks the list of found blocks to make sure we report only once
	 */
	private String getBlockAmbiguity( ) {
		StringBuilder blocks = new StringBuilder();
		
		for( SimpleTreeNode<SettingDescriptor> childHistoryNode : history.getChildren( ) ) {
			// we can assume that the child node's value is set given when this is called, but we are being safe
			if( childHistoryNode.getValue( ) != null ) { 
				String blockName = childHistoryNode.getValue( ).getDeclaringBlock().getDeclaringProfile().getName() + "." + childHistoryNode.getValue().getDeclaringBlock().getName();
				if( blocks.length() > 0 ) {
					blocks.append( ", " );
				}
				blocks.append( "'" );
				blocks.append( blockName );
				blocks.append( "'" );
			}
		}
		return blocks.toString();
	}
	
	/**
	 * This is the final method called before this setting is made available to the configuration system.
	 * It does some final checks like making sure there are overrides in place, etc.
	 */
	public void validate( ) {
		// if the history is null it means that we don't have a setting we can actually use
		// and it is because there is some form of ambiguity in this setting which we need to report
		Conditions.checkConfiguration( 
				history.getValue( ) != null, 
				"Could not resolve the ambiguity for setting '%s' given more than one block (%s) has the setting declared and an appropriate override is not available. Occurred while processing root block '%s.%s'.", 
				this.name,
				getBlockAmbiguity( ), 
				rootProfile,
				rootBlock ); 

		Conditions.checkConfiguration( 
				!this.deferred, 
				"Setting '%s' from block '%s.%s' is indicating it is deferred (an overridden value must be provided later), but that didn't happen. Occurred while processing root block '%s.%s'.", 
				this.name,
				history.getValue().getDeclaringBlock().getDeclaringProfile().getName( ), 
				history.getValue().getDeclaringBlock().getName( ), 
				rootProfile,
				rootBlock ); 

		if( history.hasChildren( ) ) {
			logger.info( "Setting '{}' was declared in {} and will therefore use an overridden value.", name, getBlockHistory( ) );
		}

		// now that we are 'done' with this setting, we can create the source name to report
		// to the configuration manager
		sourceName = String.format(
				SOURCE_NAME_FORMAT,
				history.getValue( ).getDeclaringBlock().getDeclaringProfile().getDeclaringSource().getSourcePath(), 
				history.getValue( ).getDeclaringBlock().getDeclaringProfile().getName(), 
				history.getValue( ).getDeclaringBlock().getName( ) );
	}
}
