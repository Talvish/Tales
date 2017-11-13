// ***************************************************************************
// *  Copyright 2017 Joseph Molnar
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
package com.talvish.tales.validation.validators;

import com.google.common.base.Strings;

/**
 * A validator that checks to see if a string value is empty or null.
 * @author jmolnar
 *
 */
public class NotEmptyValidator implements ValueValidator<String> {

	/**
	 * Performs the check to see if the string value is empty or null.
	 * Null values are treated as valid.
	 * @param theValue the value to check
	 * @return true if the passed in value is not empty or null, false otherwise
	 */
	public boolean isValid(String theValue) {
		return !Strings.isNullOrEmpty( theValue ); 
	}
	
	/**
	 * Generates a message for a value that is considered invalid.
	 * @param theValue the value to generate a message for 
	 * @param theBuilder the builder to create the message in
	 */
	@Override 
	public void generateMessageFragment( String theValue, StringBuilder theBuilder ) {
		theBuilder.append( "the value is missing, empty or null" );
	}
}
