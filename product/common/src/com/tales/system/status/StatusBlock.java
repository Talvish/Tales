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
package com.tales.system.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.naming.NameManager;
import com.tales.parts.naming.NameValidator;


/**
 * This class represents a set of status values
 * typically associated with a contract.
 * @author jmolnar
 *
 */
public class StatusBlock {
	public static final String STATUS_BLOCK_NAME_VALIDATOR = "status_block_name";
	
	static {
		if( !NameManager.hasValidator( StatusBlock.STATUS_BLOCK_NAME_VALIDATOR ) ) {
			NameManager.setValidator( StatusBlock.STATUS_BLOCK_NAME_VALIDATOR, new LowerCaseEntityNameValidator() );
		}
	}
	
	private final String name;
	private final Collection<StatusValue> statusValues;

	/**
	 * Constructor taking the needed items for the status block.
	 * @param theName the name to give the status block
	 * @param theStatusValues the status values to store in the block
	 */
	public StatusBlock( String theName, Collection<StatusValue> theStatusValues ) {
		NameValidator nameValidator = NameManager.getValidator( StatusBlock.STATUS_BLOCK_NAME_VALIDATOR );
		
		Preconditions.checkArgument( !Strings.isNullOrEmpty(theName), "the status block needs a name" );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Status block name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theStatusValues, "the status block '%s' needs values", theName );
		
		name = theName;
		statusValues = Collections.unmodifiableCollection( new ArrayList<StatusValue>( theStatusValues ) );
	}
	
	/**
	 * Returns the status block name.
	 * @return the status block name
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * Returns the set of status value supported by the block.
	 * @return the status values
	 */
	public Collection<StatusValue> getStatusValues( ) {
		return statusValues;
	}
}
