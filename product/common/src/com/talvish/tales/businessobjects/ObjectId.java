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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static final String	OID_REGEX			= "([0-9a-fA-F]{16})([0-9a-fA-F]{4})([0-9a-fA-F]{12})";
	public static final Pattern OID_PATTERN			= Pattern.compile( OID_REGEX );

	
	// the actual id
	private final String		_stringForm;

	// the components
	private final long			_sourceId;
	private final long			_valueId;
	private final int 			_typeId;
	

	/**
	 * Creates an ObjectId based on the components.
	 * The values are checked for proper ranges but not
	 * if source or type are valid.
	 * @param theValueId the value
	 * @param theTypeId the type of the object id
	 * @param theSourceId the source id, representing who generated it
	 */
	public ObjectId( long theValueId, int theTypeId, long theSourceId  ) {
		this( theValueId, theTypeId, theSourceId, true);
	}

	/**
	 * Private constructor for sharing code.
	 * @param theValueId the value 
	 * @param theTypeId the type of the object id
	 * @param theSourceId the source id, representing who generated it
	 * @param validate if true, the numeric values are validated, if false, they are not
	 */
	private ObjectId( long theValueId, int theTypeId, long theSourceId, boolean validate  )  {
		if( validate ) {
			// ensure parameters are good
			isValid( theValueId, theTypeId, theSourceId, true );
		}

		// set our values ....
		_valueId = theValueId;
		_typeId = theTypeId;
		_sourceId = theSourceId;
		
		// the ToUpper form of the string
		_stringForm = String.format( "%016X%04X%012X", _valueId, _typeId, _sourceId );
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

	/**
	 * A method that will parse a string into an ObjectId and throw an exception if it cannot.
	 * @param theStringForm the string to parse
	 * @return an ObjectId 
	 * @throws IllegalArgumentException if the string cannot be parse
	 */
	public static ObjectId parse( String theStringForm ) {
		return tryParse( theStringForm, true );
	}
	
	/**
	 * A method that will attempt to parse a string into an ObjectId.
	 * @param theStringForm the string to parse
	 * @return null if the string could not be parsed, and ObjectId if it could
	 */
	public static ObjectId tryParse( String theStringForm ) {
		return tryParse( theStringForm, false );
	}

	/**
	 * Helper method that will parse a string into an ObjectId and it 
	 * may or may not except depending on the shoudlExcept parameter
	 * @param theStringForm the string to parse
	 * @param shouldExcept true if the method should except if the values are wrong, false if it should not
	 * @return null if the string could not be parsed (and shouldExcept is false, since an exception will be raised otherwise), and ObjectId if it could
	 */
	private static ObjectId tryParse( String theStringForm, boolean shouldExcept ) {
		ObjectId result = null;
		
		if( theStringForm != null ) {
			Matcher matcher = OID_PATTERN.matcher( theStringForm );
			if( matcher.matches( ) ) {
				// set our values, none of which should fail given the regex
				long valueId = Long.parseLong( matcher.group( 1 ), 16 );
				int typeId = Integer.parseInt( matcher.group( 2 ), 16 );
				long sourceId = Long.parseLong( matcher.group( 3 ), 16 );
	
				if( isValid( valueId, typeId, sourceId, shouldExcept ) ) {					
					result = new ObjectId( valueId, typeId, sourceId, false ); // false means don't validate (since I just did)
				} // no need to throw exceptions if bad, isValid will
	
			} else if( shouldExcept ) {
				throw new IllegalArgumentException( String.format( "string, '%s', is either not %s characters long or does not have required shape", OID_LENGTH, theStringForm ) );
			}
		} else if( shouldExcept ) {
			throw new IllegalArgumentException( "string was not given to parse " );
		}

		return result;
	}

	/**
	 * Called by methods looking to confirm the numeric values for the object id components are in range.
	 */
	private static boolean isValid( long theValueId, int theTypeId, long theSourceId, boolean shouldExcept )  {
		boolean valid = true;
		String failedText = null;
		
		if( !( theValueId <= MAX_VALUE_ID && theValueId >= MIN_VALUE_ID ) ) {
			failedText = String.format( "value '%s' is out of the acceptable range", theValueId );
			valid = false;
		}
		if( !( theTypeId <= MAX_TYPE_ID && theTypeId >= MIN_VALUE_ID ) ) {
			failedText = String.format( "type '%s' is out of the acceptable range", theTypeId );
			valid = false;
		}
		if( !( theSourceId <= MAX_SOURCE_ID && theSourceId >= MIN_SOURCE_ID ) ) {
			failedText = String.format( "source '%s' is out of the acceptable range", shouldExcept );
			valid = false;
		}
		
		if( !valid && shouldExcept ) {
			throw new IllegalArgumentException( failedText );
		} else {
			return valid;
		}
	}
}
