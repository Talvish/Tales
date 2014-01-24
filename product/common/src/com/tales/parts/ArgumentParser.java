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

package com.tales.parts;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This is a helper class that takes command-line arguments and turns
 * them into key value pairs. Keys start with a dash and then must
 * match the expression \w+(\.\w+)*. The separator between keys and
 * values must be either ':', '=' or whitespace. Values can be any 
 * combination of characters.</p>
 * 
 * <p>Examples: 
 * <ul>
 *   <li>key=value</li>
 *   <li>com.test.setting:b</li>
 *   <li>key1 value</li>
 * </ul></p>
 *   
 * <p>Since java parses part of the command line automatically double
 * quotes must be used to ensure that values contain all their
 * characters, including spaces.</p>
 * 
 * <p>Examples:
 * <ul>
 *   <li>key " this is a value with spaces"</li>
 *   <li>key "-value"</li>
 * </ul></p>
 *   
 * <p>If you wish to have a quote in the text, then it must be escaped with
 * a \ character.</p>
 * 
 * <p>Examples:
 * <ul>
 *   <li>key "\"quoted valuevalue\""</li>
 * </ul></p>
 * 
 * @author jmolnar
 *
 */
public class ArgumentParser {
	// java regular expressions as defined here: http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
	private final static String NAME_REGEX = "[\\w]+(?:\\.\\w+)*";
	
	private final static String VALUE_ALONE_REGEX = "^[^-.].*$"; 
	private final static Pattern VALUE_ALONE_PATTERN = Pattern.compile( VALUE_ALONE_REGEX );
	
	private final static String NAME_VALUE_REGEX = String.format( "^(?:-)(%s)(?:(?:=|:|\\s+)(.+))?$", NAME_REGEX );
	private final static Pattern NAME_VALUE_PATTERN = Pattern.compile( NAME_VALUE_REGEX );
	
	
	/**
	 * Returns a key/value map from the command line arguments.
	 * @param theArgs the array of command line argument strings
	 * @return the map of keys and values
	 */
	public static Map<String, String> parse( String ... theArgs) {
		String name		= null;
		Matcher matcher	= null;
		
		Map<String, String> returnMap = new HashMap<String, String>( );
		
		for( String arg : theArgs ) {
			if( name == null ) {
				name = parseNameValue( arg, name, returnMap );
			} else {
				// we have a name, so we need a value
				matcher = VALUE_ALONE_PATTERN.matcher( arg );
				if( matcher.matches( ) ) {
					returnMap.put( name, matcher.group( ) );
					name = null;
				} else {
					name = parseNameValue( arg, name, returnMap );
				}
			}
		}
		if( name != null ) {
			returnMap.put( name, null );
		}
    	
		return returnMap;
	}
	
	/**
	 * Private method used to draw out a name value pair, if possible. 
	 * @param theArg the command line argument to parse
	 * @param thePreviousName the previous name, which will get saved if we do find 
	 * @param theMap the map to save the name value pairs in
	 * @return returns the name if a value wasn't found, otherwise null
	 */
	private static String parseNameValue( String theArg, String thePreviousName, Map<String, String> theMap ) {
		String name = null;
		String value = null;
		Matcher matcher = NAME_VALUE_PATTERN.matcher( theArg );

		// we always save the previous name, if available if set
		// and it is okay to not have a value for the name
		// we can always save because it is assumed that the
		// check for a value has already occurred before the call
		if( thePreviousName != null ) {
			theMap.put( thePreviousName, null );
		}
		
		if( matcher.matches( ) ) {
			// we should have a name at a minimum
			name = matcher.group( 1 );
			value = matcher.group( 2 );
			if( value != null) {
				// and we have a value as well
				theMap.put( name, value );
				name = null;
			} // if no value, we see if the value is the next argument
			
		} else {
			// report a problem
		}
		return name;
	}
}
