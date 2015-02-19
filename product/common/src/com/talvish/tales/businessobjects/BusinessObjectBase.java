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

import com.google.common.base.Preconditions;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;

/**
 * The base business object for objects that will used ObjectId's for ids.
 * @author jmolnar
 *
 */
@DataContract( name ="com.tales.business_objects.business_object_base")
public abstract class BusinessObjectBase extends TimestampedBase {
	@DataMember( name = "id") private ObjectId id;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	protected BusinessObjectBase( ) {
	}
	
	/**
	 * A constructor used, internally, for doing copy constructor
	 * or copying between different projects of the same object.
	 * @param theReference the object to copy
	 */
	protected BusinessObjectBase( BusinessObjectBase theReference ) {
		super( theReference ); // this will check it isn't null
		id = theReference.id;
	}

	/**
	 * The constructor needed when creating a new instance. 
	 * @param theId The unique id for this object.
	 */
	protected BusinessObjectBase( ObjectId theId ) {
		Preconditions.checkNotNull( theId, "need an id" );
		
		id = theId;
	}
	
	/**
	 * Constructor primarily meant for non-reflection-based serialization.
	 * @param theId the id to give the object
	 * @param theCreationTimestamp the datetime the object was created
	 * @param theModificationTimestamp the datetime the object was last modified
	 */
	protected BusinessObjectBase( ObjectId theId, OffsetDateTime theCreationTimestamp, OffsetDateTime theModificationTimestamp ) {
		super( theCreationTimestamp, theModificationTimestamp );
		Preconditions.checkNotNull( theId, "need an id" );
		id = theId;
	}
	
	/**
	 * The unique id given to this object.
	 */
	public ObjectId getId( ) {
		return id;
	}
	
	/**
	 * Sets the id, which is only possible if the id isn't set.
	 * @param theId the id to set
	 */
	public void setId( ObjectId theId ) {
		Preconditions.checkNotNull( theId, "need an id" );
		Preconditions.checkState( this.id == null, "cannot set id to '%s' since id is already set to '%s'", theId, this.id );
		id = theId;
	}
}
