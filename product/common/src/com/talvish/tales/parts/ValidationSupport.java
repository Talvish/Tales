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

/**
 * This interface is used to indicate that a class supports validation.
 * @author jmolnar
 *
 */
public interface ValidationSupport {
	/**
	 * Method called to validate. If validation fails
	 * ValidationException is expected to be thrown.
	 * This allows the type of failure to be exposed.
	 */
	void validate( );
}
