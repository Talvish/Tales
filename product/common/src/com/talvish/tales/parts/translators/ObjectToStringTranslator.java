// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.parts.translators;

/**
 * This is a simple translator that calls 'toString' on the object.
 * @author jmolnar
 *
 */
public class ObjectToStringTranslator extends NullTranslatorBase implements Translator {
	/**
	 * Constructor that instructs a null object to be set to null string.
	 */
	public ObjectToStringTranslator( ) {
		super( null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received. 
	 * @param theNullValue the null value to use
	 */
	public ObjectToStringTranslator( String theNullValue ) {
		super( theNullValue );
	}

	/**
	 * Translates the received object into a string.
	 */
	@Override
	public Object translate( Object anObject ) {
		Object returnValue;

		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			returnValue = anObject.toString();
		}
		
		return returnValue;
	}
}
