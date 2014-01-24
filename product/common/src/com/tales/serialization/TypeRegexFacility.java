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
package com.tales.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This is a simple mapping facility between types and a regular expression.
 * This class is primarily designed around the needs for aiding serialization.
 * @author jmolnar
 *
 */
public class TypeRegexFacility {
	private final Map<Class<?>, String> typeRegexes = new ConcurrentHashMap<Class<?>, String>( 16, 0.75f, 1 );

	/**
	 * Simple default constructor.
	 */
	public TypeRegexFacility( ) {
		// the regular expressions to help with value matching
		
		String integerRegex ="[0-9]+";
		typeRegexes.put( Integer.class, integerRegex );
		typeRegexes.put( int.class, integerRegex );

		String longRegex = "[0-9]+";
		typeRegexes.put( Long.class, longRegex );
		typeRegexes.put( long.class, longRegex );

		String floatRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
		typeRegexes.put( Float.class, floatRegex );
		typeRegexes.put( float.class, floatRegex );

		String doubleRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?"; 
		typeRegexes.put( Double.class, doubleRegex );
		typeRegexes.put( double.class, doubleRegex );

		String booleanRegex = "1|0|true|false";
		typeRegexes.put( Boolean.class, booleanRegex );
		typeRegexes.put( boolean.class, booleanRegex );

		// from: http://www.pelagodesign.com/blog/2009/05/20/iso-8601-date-validation-that-doesnt-suck/
		String datetimeRegex = "([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$";
		typeRegexes.put( DateTime.class, datetimeRegex );

		String stringRegex = ".+";
		typeRegexes.put( String.class, stringRegex );

	}

	/***
	 * This method is used to add regular expressions for a particular type.
	 * To ensure things work properly  
	 * @param theClass
	 * @param theRegex
	 */
	public void registerRegex( Class<?> theClass, String theRegex ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theRegex ), "need a regex" );
		Preconditions.checkArgument( !theRegex.startsWith( "^") && !theRegex.endsWith( "$" ), "the regular expressions should not start with '^' or end with '$'." );

		typeRegexes.put( theClass, theRegex );
	}
	
	/**
	 * Gets a regular expression to match strings for the specified type.
	 * The expression DO NOT match the string end to end. To do that add 
	 * '^' to the front and '$' to the end of the expression.
	 * @param theType the type to get a regex for
	 * @return the regex, or {@code null} if one was not found
	 */
	public String getRegex( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a type to get a regex");
		String regex = typeRegexes.get( theType );
		
		if( regex == null && theType.isEnum( ) ) {
			boolean looped = false;
			// enums regex are generated as needed and then saved
			StringBuilder regexBuilder = new StringBuilder();
			for( Object value : theType.getEnumConstants() ) {
				if( looped) {
					regexBuilder.append( '|' );
				}
				regexBuilder.append( value );
				looped = true;
			}
			regex = regexBuilder.toString();
			typeRegexes.put( theType, regex );
		}
		return regex;
	}	
}
