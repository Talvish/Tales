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

import com.google.common.base.Strings;

/**
 * This annotation is used as a constraint on parameters and class members 
 * that are integers (e.g int, long, etc) to indicate the maximum value of the 
 * parameter or class member. 
 * Currently this supports int, long and BigDecimal.
 * @author jmolnar
 */
public class NotEmptyValidator implements ValueValidator<String> {

	@Override
	public boolean isValid(String theValue) {
		return !Strings.isNullOrEmpty( theValue ); 
	}
}
