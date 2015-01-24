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
package com.talvish.tales.system.configuration.annotated;

import com.talvish.tales.parts.naming.RegExValidator;

/**
 * A name validator that ensures the name contains parts segmented by period, where the
 * parts contain lowercase letters, underscores and numbers, but cannot start with a number
 * and it contain 'parameters' identified by a parameter in curly braces.
 * e.g. "value.{name}._value" 
 * @author jmolnar
 *
 */
public class SegmentedLowercaseParameterizedValidator extends RegExValidator {
	public static final String PREFIX_REGEX = "\\{prefix\\}";
	public static final String PARAM_REGEX = "\\{name\\}";
	private static final String CHAR_ELEMENT_REGEX = String.format( "[\\p{javaLowerCase}_0-9]|(%1$s)", PARAM_REGEX );
	private static final String START_CHAR_ELEMENT_REGEX = String.format( "[\\p{javaLowerCase}_]|(%1$s)|(%2$s)", PREFIX_REGEX, PARAM_REGEX );
	private static final String PART_REGEX = String.format( "(%1$s)(%2$s)*", START_CHAR_ELEMENT_REGEX, CHAR_ELEMENT_REGEX );
	private static final String FULL_REGEX = String.format( "^(%1$s)(\\.(%1$s))*$", PART_REGEX );

	public SegmentedLowercaseParameterizedValidator( ) {
		super( FULL_REGEX );
	}
}
