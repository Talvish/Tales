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

import com.google.common.base.Preconditions;
import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

@DataContract( name ="com.tales.transport.model_base")
public class TransportModelBase {
	@DataMember( name = "id" ) private UUID id;
	@DataMember( name = "creation_timestamp" ) private DateTime creationTimestamp;
	@DataMember( name = "modification_timestamp" ) private DateTime modificationTimestamp;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	protected TransportModelBase( ) {
	}
	
	/**
	 * A transforming copy constructor from the internal
	 * representation to the wire representation.
	 */
	protected TransportModelBase( EngineModelBase theBase ) {
		id = theBase.getId( );
		creationTimestamp = theBase.getCreationTimestamp();
		modificationTimestamp = theBase.getModificationTimestamp();
	}
	
	/**
	 * The constructor needed when creating a new instance. 
	 * @param theId The unique id for this object.
	 */
	public TransportModelBase( UUID theId ) {
		Preconditions.checkArgument( theId != null, "an id must be give" );
		
		id = theId;
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
}
