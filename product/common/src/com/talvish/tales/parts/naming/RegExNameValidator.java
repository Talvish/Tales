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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Matches a name to a specified regular expression. 
 * The class does not enforce that the full name is matched by the expression. 
 * If that is the intent then the expression must start with a '^' and end with a '$'. 
 * @author jmolnar
 *
 */
public class RegExNameValidator implements NameValidator {
	private final String expression;
	private final Pattern pattern;

	
	public RegExNameValidator( String theExpression ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theExpression ), "need an expression" );
		
		try {
			expression = theExpression;
			pattern = Pattern.compile( theExpression );
		} catch( PatternSyntaxException e ) {
			throw new IllegalArgumentException( String.format( "'%s' is not a valid regular expression", theExpression ), e );
		}
	}
	
	/**
	 * Returns the expression used for matching.
	 * @return the expression
	 */
	public String getExpression( ) {
		return this.expression;
	}
	
	/**
	 * Returns the compiled form of the expression used for matching.
	 * @return the compiled form of the expression
	 */
	public Pattern getPattern( ) {
		return this.pattern;
	}
	
	/**
	 * Returns true if the name matches the expression, false otherwise.
	 */
	public boolean isValid(String theName) {
		Preconditions.checkNotNull( theName, "need a name" );
		
		return pattern.matcher( theName ).matches();
	}
}
