// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
 * A translator that essentially just passes the object through. The exception
 * is when the object to translate is null, the value that was indicated to
 * use for null will be used instead.
 * @author jmolnar
 *
 */
public class PassthroughTranslator extends NullTranslatorBase implements Translator {
	/**
	 * Constructor that sets a null value should be used when the object to translate is null.
	 */
	public PassthroughTranslator( ) {
		super( null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received. 
	 * @param theNullValue the null value to use
	 */
	public PassthroughTranslator( Object theNullValue ) {
		super( theNullValue );
	}

	/**
	 * Doesn't do any transformation, except perhaps, if the value is null.
	 */
	@Override
	public Object translate( Object anObject ) {
		Object returnValue;

		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			returnValue = anObject;
		}
		
		return returnValue;
	}
}
