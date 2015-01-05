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

import com.google.common.base.Preconditions;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;

// TODO: Need to figure out how to handle overrides getting description, verifying sensitive flag etc
// TODO: consider if we want to add type to this

/**
 * This class represents the setting structure as found in the configuration. 
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.setting_descriptor")
public class SettingDescriptor {
	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	
	@DataMember( name="value" )
	private String value;
	
	@DataMember( name="override" )
	private Boolean override;
	@DataMember( name="sensitive" )
	private Boolean sensitive;
	
	// in-memory items
	private BlockDescriptor block;

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
	 * This may not be set in which case the overridden setting may have the value to use (assuming it is set). 
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
	 * Indicates if this setting is overriding another setting of the same name in a parent or included block.
	 * This value may not be set in the config source which means this method will return false.
	 * @return true means overriding, false means not overriding
	 */
	public boolean isOverride( ) {
		return override == null ? false : override.booleanValue();
	}
	
	/**
	 * Indicates if this setting is consider sensitive. If sensitive the intention is for the setting value to
	 * not be dumped into log files or be made generally visible.
	 * This value may not be set in the config source which means this method will return false.
	 * @return true means  
	 */
	public boolean isSensitive( ) {
		return sensitive == null ? false : sensitive.booleanValue();
	}

	/**
	 * The block that this setting is declared within.
	 * @return the block that the setting is declared within
	 */
	public BlockDescriptor getBlock( ) {
		return block;
	}
	
	/**
	 * Helper method called after the source loads. 
	 * This is also how the block is set on the setting. 
	 * It does some simple validation and sets up additional data to help 
	 * with runtime support.
	 * @param theBlock the block that this setting is declared within
	 */
	protected void cleanup( BlockDescriptor theBlock ) {
		Preconditions.checkArgument( theBlock != null, "Setting '%s' is getting the block set to null.", name );
		Preconditions.checkState( block == null, "Setting '%s' from '%s.%s' is having the block reset to '%s.%s'.", 
				name, 
				block == null ? "<empty>" : block.getProfile().getName( ),
				block == null ? "<empty>" : block.getName( ),
				theBlock.getProfile().getName(), 
				theBlock.getName( ) );

		block = theBlock;
	}
}
