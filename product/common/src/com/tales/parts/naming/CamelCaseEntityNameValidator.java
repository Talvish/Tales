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
package com.tales.parts.naming;

/**
 * A name validator that ensures the name starts with lower case or underscore, but can then 
 * have lower/uppers case letters, underscores and numbers. 
 * @author jmolnar
 *
 */
public class CamelCaseEntityNameValidator extends RegExNameValidator {
	private static final String PART_REGEX = "[\\p{javaLowerCase}_][\\p{javaLowerCase}\\p{javaUpperCase}_0-9]*";
	private static final String FULL_REGEX = String.format( "^%1$s$", PART_REGEX );

	public CamelCaseEntityNameValidator( ) {
		super( FULL_REGEX );
	}
}
