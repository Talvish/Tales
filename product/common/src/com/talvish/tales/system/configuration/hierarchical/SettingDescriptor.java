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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.naming.SegmentedLowercaseValidator;
import com.talvish.tales.serialization.json.UnmappedName;
import com.talvish.tales.serialization.json.UnmappedValue;
import com.talvish.tales.validation.Conditions;

/**
 * This class represents the setting structure as found in the configuration. 
 * @author jmolnar
 *
 */
@DataContract( name="talvish.tales.configuration.hierarchical.setting_descriptor")
public class SettingDescriptor {
	public static final String NAME_VALIDATOR = "tales.configuration.hierarchical.setting_name";
	
	static {
		if( !NameManager.hasValidator( NAME_VALIDATOR ) ) {
			NameManager.setValidator( NAME_VALIDATOR, new SegmentedLowercaseValidator( ) );
		}
	}

	@UnmappedName
	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	
	@UnmappedValue
	@DataMember( name="value" )
	private JsonElement value;
	
	@DataMember( name="override" )
	private Boolean override;
	@DataMember( name="deferred" )
	private Boolean deferred;

	@DataMember( name="sensitive" )
	private Boolean sensitive;
	
	// in-memory items
	private BlockDescriptor declaringBlock;

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
	public JsonElement getValue( ) {
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
	 * Indicates if this setting must be overridden. If it is not
	 * overridden a configuration failure will occur.
	 * @return true if it must be overridden, false otherwise
	 */
	public boolean isDeferred( ) {
		return deferred == null ? false : deferred.booleanValue();
	}
	
	/**
	 * Indicates if this setting is consider sensitive. If sensitive the intention is for the setting value to
	 * not be dumped into log files or be made generally visible.
	 * If this is not set, then it should take the value on of something it is overriding or false.
	 * @return true means  
	 */
	public Boolean isSensitive( ) {
		return sensitive;
	}

	/**
	 * The block that this setting is declared within.
	 * @return the block that the setting is declared within
	 */
	public BlockDescriptor getDeclaringBlock( ) {
		return declaringBlock;
	}
	
	/**
	 * Helper method called after the source loads. 
	 * This is also how the block is set on the setting. 
	 * It does some simple validation and sets up additional data to help 
	 * with runtime support.
	 * @param theDeclaringBlock the block that this setting is declared within
	 */
	protected void onDeserialized( BlockDescriptor theDeclaringBlock ) {
		Preconditions.checkArgument( theDeclaringBlock != null, "Setting '%s' is getting the block set to null.", name );
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( name ), "A setting without a name was loaded from block '%s.%s'.", theDeclaringBlock.getDeclaringProfile().getName(), theDeclaringBlock.getName() );
		Preconditions.checkState( declaringBlock == null, "Setting '%s' from '%s.%s' is having the block reset to '%s.%s'.", 
				name, 
				declaringBlock == null ? "<empty>" : declaringBlock.getDeclaringProfile().getName( ),
				declaringBlock == null ? "<empty>" : declaringBlock.getName( ),
				theDeclaringBlock.getDeclaringProfile().getName(), 
				theDeclaringBlock.getName( ) );
		NameValidator nameValidator = NameManager.getValidator( NAME_VALIDATOR );
		Conditions.checkConfiguration( nameValidator.isValid( name ), String.format( "Setting name '%s' on block '%s.%s' does not conform to validator '%s'.", 
				name, 
				theDeclaringBlock.getDeclaringProfile().getName(), 
				theDeclaringBlock.getName( ),
				nameValidator.getClass().getSimpleName() ) );
		Conditions.checkConfiguration( !isDeferred( ) || value == null || value.isJsonNull(), String.format( "Setting name '%s' on block '%s.%s' indicates it is deferred (an overridden value must be provided later) however a value was provided now.", 
				name, 
				theDeclaringBlock.getDeclaringProfile().getName(), 
				theDeclaringBlock.getName( ) ) );
		Conditions.checkConfiguration( !isDeferred( ) || theDeclaringBlock.isDeferred(), String.format( "Setting name '%s' on block '%s.%s' indicates it is deferred however the block is not marked deferred.", 
				name, 
				theDeclaringBlock.getDeclaringProfile().getName(), 
				theDeclaringBlock.getName( ) ) );

		declaringBlock = theDeclaringBlock;
	}
}
