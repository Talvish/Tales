// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.talvish.tales.parts.naming;

/**
 * A name validator that ensures the name contains lowercase letters, underscores
 * and numbers, but cannot start with a number. 
 * @author jmolnar
 *
 */
public class LowerCaseEntityNameValidator extends RegExNameValidator {
	private static final String PART_REGEX = "[\\p{javaLowerCase}_][\\p{javaLowerCase}_0-9]*";
	private static final String FULL_REGEX = String.format( "^%1$s$", PART_REGEX );

	public LowerCaseEntityNameValidator( ) {
		super( FULL_REGEX );
	}
}
