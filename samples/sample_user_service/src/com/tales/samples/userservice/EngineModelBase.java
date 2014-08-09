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

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Preconditions;

/**
 * A helper base class for objects that are used in an engine
 * and typically persisted into storage.
 * @author Joseph Molnar
 */
public class EngineModelBase {
	// TODO: this needs to be updated to make use of the storage system
	private UUID id;
	private DateTime creationTimestamp;
	private DateTime modificationTimestamp;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	protected EngineModelBase( ) {
	}
	
	/**
	 * The constructor needed when creating a new instance. 
	 * @param theId The unique id for this object.
	 */
	public EngineModelBase( UUID theId ) {
		Preconditions.checkArgument( theId != null, "an id must be give" );
		
		id = theId;
		creationTimestamp = DateTime.now( DateTimeZone.UTC );
		modificationTimestamp = creationTimestamp;
	}
	
	/**
	 * The unique id given to this object.
	 */
	public UUID getId( ) {
		return id;
	}
	
	/**
	 * The date, in UTC, when the object was created.
	 */
	public DateTime getCreationTimestamp( ) {
		return creationTimestamp;
	}	
	
	/**
	 * The date, in UTC when the object was last modified.
	 */
	public DateTime getModificationTimestamp( ) {
		return modificationTimestamp;
	}
	
	/**
	 * A public helper class called to indicate the class was modified.
	 */
	public void indicateModified( ) {
		modificationTimestamp = DateTime.now( DateTimeZone.UTC);
	}
}
