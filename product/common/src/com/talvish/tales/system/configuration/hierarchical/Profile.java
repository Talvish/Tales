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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Conditions;

public class Profile {
	// TODO: we need some form of state to manage overall creation
	
	private final String name;
	private final String description;

	private final Map<String,Block> declaredBlocks = new HashMap<>( );
	private Profile parent;
	
	private final Map<String,Block> blocks = new HashMap<>( );
	
	protected Profile( String theName, String theDescription ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "The profile needs a name" );
		//Conditions.checkConfiguration( theParentProfile == null || theParentProfile.getName().equals( theName ), "The parent profile name '%s' is the same as this parent.", theName );
		
		name = theName;
		description = theDescription;

	}
	
	public Profile getParent( ) {
		return parent;
	}
	
	protected void setParent( Profile theParent ) {
		parent = theParent;

		if( parent != null && parent.blocks != null ) {
			for( Entry<String,Block> entry : parent.blocks.entrySet( ) ) {
				// this makes sure that if profiles have blocks with the same name it is a problem
				// this is really just a safety net since this should already have been checked
				// also if we move to multi-extends, then this is even more relevant
				Conditions.checkConfiguration( !blocks.containsKey( entry.getKey( ) ), "Cannot include the parent profile '%s' in profile '%s' because a block with name '%s' (from profile '%s') already exists (from profile '%s').", parent.getName(), this.name, entry.getKey( ), entry.getValue().getDeclaredProfile().getName( ), blocks.get( entry.getKey( )).getDeclaredProfile().getName( ) );
				blocks.put( entry.getKey(), entry.getValue( ) );
			}
		}
	}
	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	
	public boolean hasDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return declaredBlocks.containsKey( theName );
	}
	
	public Block getDeclaredBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return declaredBlocks.get( theName );
	}
	
	public void addDeclaredBlock( Block theBlock ) {
		Preconditions.checkNotNull( theBlock, "Sent an empty block to add to profile '%s'.", this.name );
		Preconditions.checkState( this == theBlock.getDeclaredProfile( ), "Cannot add block '%s' to profile '%s' since the block believes the parent is '%s'.", theBlock.getName(), this.name, theBlock.getDeclaredProfile().getName() );
		Conditions.checkConfiguration( !declaredBlocks.containsKey( theBlock.getName( ) ), "Cannot add block '%s' to profile '%s' since it has already been declared.", theBlock.getName( ), this.name );
		
		declaredBlocks.put( theBlock.getName( ), theBlock );
		
		// we do not validate things like overrides for the block since it is
		// already done in the block constructor

		// and now we put the block in our global list, which will overwrite
		// any existing block with the same name (since this is the most recent)
		blocks.put( theBlock.getName( ), theBlock );
	}

	public boolean hasBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return blocks.containsKey( theName );
	}
	
	public Block getBlock( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Sent an empty block name to retrieve from profile '%s'.", this.name );
		return blocks.get( theName );
	}
}
