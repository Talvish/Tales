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

import com.google.common.base.Preconditions;
import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * The base business object for objects that will used ObjectId's for ids.
 * @author jmolnar
 *
 */
@DataContract( name ="com.tales.business_objects.business_object_base")
public class BusinessObjectBase extends TimestampedBase {
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
	public BusinessObjectBase( ObjectId theId ) {
		Preconditions.checkNotNull( theId, "need an id" );
		
		id = theId;
	}
	
	/**
	 * Constructor primarily meant for non-reflection-based serialization.
	 * @param theId the id to give the object
	 * @param theCreationTimestamp the datetime the object was created
	 * @param theModificationTimestamp the datetime the object was last modified
	 */
	public BusinessObjectBase( ObjectId theId, DateTime theCreationTimestamp, DateTime theModificationTimestamp ) {
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
}
