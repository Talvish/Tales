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
package com.talvish.tales.auth.accesscontrol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a claim is an integer and that 
 * the value must be between a minimum and 
 * maximum value.
 * @author jmolnar
 *
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.METHOD )
@Repeatable( ClaimValueRangeSet.class )
public @interface ClaimValueRange {
	/**
	 * The nane of the claim.
	 * @return
	 */
	String claim( );
	/**
	 * The minimum, inclusive, value for the claim.
	 * @return
	 */
	int minimum( ) default Integer.MIN_VALUE;
	/**
	 * The maximum, inclusive, value for the claim.
	 * @return
	 */
	int maximum( ) default Integer.MAX_VALUE;
}