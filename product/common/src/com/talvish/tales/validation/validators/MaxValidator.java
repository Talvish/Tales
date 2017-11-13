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

import com.google.common.base.Preconditions;

/**
 * A base class for the Max annotation validators.
 * @author jmolnar
 *
 * @param <T> the type of value (e.g. int, long, etc)
 */
public abstract class MaxValidator<T> implements ValueValidator<T> {
	// NOTE: this means primitive types will be boxed when making this call, and it
	//       does look a bit odd but this is in the end we want a fast isValid call that
	//       does as little unboxing, but also it is nice to have a default implementation
	//       for generating the message fragment below

	/**
	 * The value that will be compared against when checking for valid values.
	 */
	public abstract T getValue( );

	/**
	 * Generates a message for a value that is considered invalid.
	 * @param theValue the value to generate a message for 
	 * @param theBuilder the builder to create the message in
	 */
	@Override 
	public void generateMessageFragment( T theValue, StringBuilder theBuilder ) {
		Preconditions.checkNotNull( theBuilder, "need a builder to make a message fragment" );
		
		theBuilder.append( "the attempted value " );
		theBuilder.append( theValue );
		theBuilder.append( " is not smaller than or equal to ");
		theBuilder.append( getValue( ) );
	}
}
