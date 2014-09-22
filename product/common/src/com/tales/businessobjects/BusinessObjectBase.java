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
	 * The constructor needed when creating a new instance. 
	 * @param theId The unique id for this object.
	 */
	public BusinessObjectBase( ObjectId theId ) {
		Preconditions.checkArgument( theId != null, "an id must be give" );
		
		id = theId;
	}
	
	/**
	 * The unique id given to this object.
	 */
	public ObjectId getId( ) {
		return id;
	}
}
