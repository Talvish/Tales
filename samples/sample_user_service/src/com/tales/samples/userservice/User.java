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
package com.tales.samples.userservice;

import com.tales.businessobjects.BusinessObjectBase;
import com.tales.businessobjects.ObjectId;


/**
 * The representation of the User entity in the engine/persistence.
 * @author Joseph Molnar
 *
 */
public class User extends BusinessObjectBase {
	// TODO: this needs to be updated to make use of the storage system
	private String firstName;
	private String lastName;
	private boolean deleted = false;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	@SuppressWarnings("unused") 
	private User( ) {
	}
	
	/**
	 * Constructor taking the required id.
	 */
	public User( ObjectId theId ) {
		super( theId );
	}
	
	/**
	 * Returns the first name;
	 */
	public String getFirstName( ) {
		return firstName;
	}
	
	/**
	 * Sets the first name;
	 */
	public void setFirstName( String theFirstName ) {
		firstName = theFirstName;
		this.indicateModified();
	}
	
	/**
	 * Returns the last name.
	 */
	public String getLastName( ) {
		return lastName;
	}
	
	/**
	 * Sets the last name.
	 */
	public void setLastName( String theLastName ) {
		lastName = theLastName;
		this.indicateModified();
	}
	
	/**
	 * Indicates if an entity is soft-deleted.
	 * @return
	 */
	public boolean isDeleted( ) {
		return deleted;
	}
	
	/**
	 * A mechanism for soft-deletion. Pretty common
	 * to not actually delete entities in a system
	 * but instead to have them 'soft deleted'.
	 */
	public void indicateDeleted( ) {
		deleted = true;
		indicateModified();
	}
}
