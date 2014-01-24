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
package com.tales.parts.naming;

/**
 * This interface is used to validate that the name given, typically for a field or type, is valid. e.g.
 * <ol> 
 *   <li>enforce that underscores exist between words and characters are all lower case for JSON output</li>
 *   <li>enforce that names aren't too long for hbase</li>
 * </ol>  
 * It does not modify the names since that can lead to unknown or potentially side-effect behaviour.
 * @author jmolnar
 */
public interface NameValidator {
	/**
	 * The method that validates the name.
	 * @param theName
	 * @return true if the name is valid, false otherwise
	 */
	boolean isValid( String theName );
}
