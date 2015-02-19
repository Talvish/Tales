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
package com.talvish.tales.businessobjects;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.google.common.base.Preconditions;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;

/**
 * Base class for business object's that don't require an id, or will have a different id.
 * @author jmolnar
 *
 */
@DataContract( name ="com.tales.business_objects.timestamped_base")
public abstract class TimestampedBase {
	@DataMember( name = "creation_timestamp" ) private OffsetDateTime creationTimestamp;
	@DataMember( name = "modification_timestamp" ) private OffsetDateTime modificationTimestamp;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	protected TimestampedBase( ) {
		creationTimestamp = OffsetDateTime.now( ZoneOffset.UTC );
		modificationTimestamp = creationTimestamp;
	}

	/**
	 * A constructor used, internally, for doing copy constructor
	 * or copying between different projects of the same object.
	 * @param theReference the object to copy
	 */
	protected TimestampedBase( TimestampedBase theReference ) {
		Preconditions.checkNotNull( theReference, "reference must not be null" );

		creationTimestamp = theReference.creationTimestamp;
		modificationTimestamp = theReference.modificationTimestamp;
	}

	/**
	 * Constructor primarily meant for non-reflection-based serialization.
	 * @param theCreationTimestamp the datetime the object was created
	 * @param theModificationTimestamp the datetime the object was last modified
	 */
	protected TimestampedBase( OffsetDateTime theCreationTimestamp, OffsetDateTime theModificationTimestamp ) {
		Preconditions.checkNotNull( theCreationTimestamp, "need a creation timestamp" );
		Preconditions.checkNotNull( theModificationTimestamp, "need a creation timestamp" );
		Preconditions.checkArgument( !theCreationTimestamp.isAfter( theModificationTimestamp ), "the creation timestamp should be less than or equal to the modification timestamp");
		
		creationTimestamp = theCreationTimestamp;
		modificationTimestamp = theModificationTimestamp;
	}
	
	/**
	 * The date, in UTC, when the object was created.
	 */
	public OffsetDateTime getCreationTimestamp( ) {
		return creationTimestamp;
	}	
	
	/**
	 * The date, in UTC when the object was last modified.
	 */
	public OffsetDateTime getModificationTimestamp( ) {
		return modificationTimestamp;
	}
	
	/**
	 * Method that the object has modified.
	 */
	protected void indicateModified( ) {
		modificationTimestamp = OffsetDateTime.now( ZoneOffset.UTC );
	}
}
