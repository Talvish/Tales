// ***************************************************************************
// *  Copyright (C) 2014 Joseph Molnar. All rights reserved.
// *
// *
// * This source file and its contents are the intellectual property of
// * Joseph Molnar. Except as specifically permitted, no portion of this 
// * source code may be modified, reproduced, copied or distributed without
// * prior written permission.
// *
// * This confidential source file contains trade secrets.
// *
// * The file is a derivative of samples included in the Tales framework that
// * are developed by Joseph Molnar.
// ***************************************************************************
package com.tales.businessobjects;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Preconditions;
import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * Base class for business object's that don't require an id, or will have a different id.
 * @author jmolnar
 *
 */
@DataContract( name ="com.tales.business_objects.timestamped_base")
public class TimestampedBase {
	@DataMember( name = "creation_timestamp" ) private DateTime creationTimestamp;
	@DataMember( name = "modification_timestamp" ) private DateTime modificationTimestamp;
	
	/**
	 * A constructor used for serialization purposes.
	 */
	protected TimestampedBase( ) {
		creationTimestamp = DateTime.now( DateTimeZone.UTC );
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
	public TimestampedBase( DateTime theCreationTimestamp, DateTime theModificationTimestamp ) {
		Preconditions.checkNotNull( theCreationTimestamp, "need a creation timestamp" );
		Preconditions.checkNotNull( theModificationTimestamp, "need a creation timestamp" );
		// TODO: need to check that modification is equal or greater than creation
		
		creationTimestamp = theCreationTimestamp;
		modificationTimestamp = theModificationTimestamp;
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
	 * Method that the object has modified.
	 */
	protected void indicateModified( ) {
		modificationTimestamp = DateTime.now( DateTimeZone.UTC );
	}
}
