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

/**
 * A validator that checks to see if a value is null or not.
 * @author jmolnar
 *
 */
public class NotNullValidator implements ValueValidator<Object> {

	/**
	 * Performs the check to see if the  value is null.
	 * Null values are treated as valid.
	 * @param theValue the value to check
	 * @return true if the passed in value is not null, false otherwise
	 */
	@Override
	public boolean isValid(Object theValue) {
		return theValue != null; 
	}
	
	/**
	 * Generates a message for a value that is considered invalid.
	 * @param theValue the value to generate a message for 
	 * @param theBuilder the builder to create the message in
	 */
	@Override 
	public void generateMessageFragment( Object theValue, StringBuilder theBuilder ) {
		theBuilder.append( "the value is missing or null" );
	}
}
