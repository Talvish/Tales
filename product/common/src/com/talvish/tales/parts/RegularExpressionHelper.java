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
package com.talvish.tales.parts;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A utility class containing helper regular expressions.
 * @author jmolnar
 *
 */
public final class RegularExpressionHelper {
	/**
	 * Helper method that makes sure a user supplied regular expression
	 * doesn't contain capturing groups, which would otherwise cause problems
	 * if the regex is being used for needs where other capturing groups are
	 * important
	 * @param theRegEx the regex to escape
	 * @return the non-capturing regex
	 */
	public final static String toNoncapturingExpression( String theRegEx ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theRegEx), "need a regex" );
		// I feel there is a better way, but this is good enough for now ...
		StringBuilder builder = new StringBuilder();
		
		int charClassCount = 0;
		
		int startOffset = 0;
		char currentChar;
		
		for( int offset = 0; offset < theRegEx.length(); offset += 1 ) {
			currentChar = theRegEx.charAt( offset );
			if( currentChar == '\\' ) { // ignore escaping character
				offset += 1;
			} else if( currentChar == '[' ) { // we are in a capturing group (java supports some nesting, though I don't fully here)
				charClassCount += 1;
			} else if( currentChar == ']' ) { // we are leaving one
				charClassCount -= 1;
			} else if( currentChar == '(' && charClassCount == 0 ) {
				if( ( offset == theRegEx.length( ) - 1 ) || ( theRegEx.charAt( offset + 1 ) != '?' ) ) { // found at the end or next character isn't a quote/meaning non-capturing already
					builder.append( theRegEx.substring( startOffset, offset + 1 ) );
					builder.append( "?:" ); // turn into a non capturing group
					startOffset = offset + 1;
				}
			}
		}
		builder.append( theRegEx.substring( startOffset, theRegEx.length()));
		return builder.toString();
	}
}
