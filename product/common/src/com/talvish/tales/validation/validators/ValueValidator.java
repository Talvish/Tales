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

/**
 * The interface representing representing a validator.
 * @author jmolnar
 * @param <T> The type the validator will be checking.
 *
 */
public interface ValueValidator<T> {
	/**
	 * Indicates with the value is considered valid based on  
	 * constraints the subclass will define.
	 * @param theValue the value to compare
	 * @return true if the valid is valid, false otherwise
	 */
	boolean isValid( T theValue );
	
	/**
	 * Generates a fragment of a message for when a value is wrong.
	 * @param theValue the value considered wrong
	 * @param theBuilder the builder to append text to
	 */
	void generateMessageFragment( T theValue, StringBuilder theBuilder );
}
