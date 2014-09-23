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
// ***************************************************************************

package com.tales.businessobjects;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This is a base identifier representing a globally unique entity.
 * This is derived from a C# object of the same name I created
 * for the same purpose in my Cello framework.
 * @author Joseph Molnar
 */
public class ObjectId {
	/**
	 * The largest value id possible.
	 */
	public static final long	MAX_VALUE_ID		= Long.MAX_VALUE;
	/**
	 * The smallest value id possible.
	 */
	public static final long	MIN_VALUE_ID		= 1;
	/**
	 * The largest type id possible.
	 */
	public static final int		MAX_TYPE_ID			= 0x000000000000FFFF;
	/**
	 * The smallest type id possible.
	 */
	public static final int		MIN_TYPE_ID			= 0x0000000000000001;
	/**
	 * The largest source id possible.
	 */
	public static final long	MAX_SOURCE_ID		= 0x0000FFFFFFFFFFFFL;
	/**
	 * The smallest source id possible.
	 */
	public static final long	MIN_SOURCE_ID		= 0x0000000000000001L;
	/**
	 * The bit length of an object id.
	 */
	public static final int		OID_LENGTH			= 32;

	
	// the actual id
	private final String		_stringForm;

	// the components
	private final long			_sourceId;
	private final long			_valueId;
	private final int 			_typeId;

	/**
	 * Creates an ObjectId based on the string representation.
	 * The string is checked for proper form but not if source
	 * or type are valid.
	 */
	public ObjectId( String theStringForm ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theStringForm ), "cannot have an empty, missing string form" );
		Preconditions.checkArgument( theStringForm.length( ) == ObjectId.OID_LENGTH, "string '%s' has a length of '%s', which doesn't match the required length of '%s'", theStringForm, theStringForm.length( ), ObjectId.OID_LENGTH );
		
		try {
			// the ToUpper form of the string
			_stringForm = theStringForm.toUpperCase();

			// set our values ....
			_valueId = Long.parseLong( theStringForm.substring( 0, 0 + 16 ), 16 );
			_typeId = Short.parseShort( theStringForm.substring( 16, 16 + 4 ), 16 );
			_sourceId = Long.parseLong( theStringForm.substring( 20, ObjectId.OID_LENGTH ), 16 );

			// now ensure they are good
			Validate( );
		} catch( Exception e ) {
			throw new IllegalArgumentException( String.format( "failed turning '%s' into an ObjectId due to exception '%s' with message '%s'", theStringForm, e.getClass().getName(), e.getMessage() ),e );
		}
	}

	/**
	 * Creates an ObjectId based on the components.
	 * The values are checked for proper ranges but not
	 * if source or type are valid.
	 * @param theValueId the value
	 * @param theTypeId the type of the object id
	 * @param theSourceId the source id, representing who generated it
	 */
	public ObjectId( long theValueId, int theTypeId, long theSourceId  )  {
		// set our values ....
		_valueId = theValueId;
		_typeId = theTypeId;
		_sourceId = theSourceId;

		// now ensure they are good
		Validate( );
		
		// the ToUpper form of the string
		_stringForm = String.format( "%016X%04X%012X", _valueId, _typeId, _sourceId );
	}


	/**
	 * Called by the constructor to validate the data given for creating the object id.
	 */
	private void Validate( ) {
		Preconditions.checkArgument( _valueId <= MAX_VALUE_ID && _valueId >= MIN_VALUE_ID, "value '%s' is out of the acceptable range", _valueId );
		Preconditions.checkArgument( _typeId <= MAX_TYPE_ID && MIN_TYPE_ID >= MIN_VALUE_ID, "type '%s' is out of the acceptable range", _typeId );
		Preconditions.checkArgument( _sourceId <= MAX_SOURCE_ID && _sourceId >= MIN_SOURCE_ID, "source '%s' is out of the acceptable range", _sourceId );
	}

	/**
	 * This is a 64-bit value representing the original unique number 
	 * assigned to the object when it was created in the ObjectIdService.
	 */
	public long getValueId() {
		return this._valueId;
	}

	/**
	 * This is a 16-bit value that represents the type of the object.
	 * The type id is assigned to the type by the ObjectIdService.
	 */
	public int getTypeId( ) {
		return this._typeId;
	}

	/**
	 * This is a value which represents the source that 
	 * generated the ObjectId. 
	 */
	public long getSourceId( ) {
		return this._sourceId;
	}

	/**
	 * Override of the equals method that ensures the
	 * object received is the correct type and that 
	 * individual components are all equal.
	 */
	@Override
	public boolean equals( Object theObject ) {
		boolean returnValue = false;
		
		if( theObject instanceof ObjectId ) {
			ObjectId theObjectId = ( ObjectId )theObject;
			if( _typeId == theObjectId._typeId &&
				_valueId == theObjectId._valueId &&
				_sourceId == theObjectId._sourceId ) {
				returnValue = true;
			}
		}

		return returnValue;
	}

	/**
	 * Override the hashCode method to return a hash code value
	 * based on the underlying string.
	 */
	@Override
	public int hashCode( ) {
		return _stringForm.hashCode( );
	}

	/**
	 * Override of the toString method to return the underlying string.
	 */
	@Override
	public String toString() {
		return _stringForm;
	}
}
