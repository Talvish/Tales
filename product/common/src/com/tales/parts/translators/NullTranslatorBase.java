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
package com.tales.parts.translators;

/**
 * Base class for translators dealing with null values.
 * @author jmolnar
 *
 */
public abstract class NullTranslatorBase {
	protected Object nullValue;

	/**
	 * Constructor taking the value to use for null
	 * @param theNullValue the value to use for null
	 */
	protected NullTranslatorBase( Object theNullValue ) {
		nullValue = theNullValue;
	}
	
	/**
	 * The value that will be used for null.
	 * @return the value that will be used for null
	 */
	public Object getNullValue( ) {
		return nullValue;
	}
}
