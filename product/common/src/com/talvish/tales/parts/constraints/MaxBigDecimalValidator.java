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
package com.talvish.tales.parts.constraints;

import java.math.BigDecimal;

/**
 * A validator that ensures an value to compare is equal to or less 
 * than the value the instance was constructed with.
 * @author jmolnar
 *
 */
public class MaxBigDecimalValidator implements ValueValidator {
	private final BigDecimal value;


	/**
	 * The constructor taking the value to compare against.
	 * @param theValue the value to compare against
	 */
	public MaxBigDecimalValidator( BigDecimal theValue ) {
		value = theValue;
	}

	/**
	 * The value that will be compared against.
	 */
	public BigDecimal getValue( ) {
		return value;
	}
	
	/**
	 * Performs the comparison assuming the correct type.
	 * Null values are treated as valid.
	 * @param theValue the value to check
	 * @return true if the passed in value is the equal to or less than, false otherwise
	 */
	public boolean isValid( Object theValue ) {
		if( theValue == null ) {
			return true;
		} else {
			return ( ( BigDecimal ) theValue ).compareTo( value ) <= 0;
		}
	}
}
