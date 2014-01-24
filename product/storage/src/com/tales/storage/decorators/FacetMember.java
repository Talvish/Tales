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
package com.tales.storage.decorators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is meant to save a collection of
 * facets on a given object.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.FIELD)
public @interface FacetMember {
	/**
     * The name to give the qualifier/column name.
     * Using this implies that there won't be a 
     * separate column for each field, but one per 
     * object. You cannot have a prefix 
     * AND a name. 
     * For dynamic column names, this name can 
     * refer to members, by using curly brace
     * encapsulated names, e.g. "device_{id}"
     * @return the name to give the object
     */
	String name( ) default "";

	/**
     * The prefix to qualifier/column names.
     * Using this implies that each field will be a 
     * separate column. You cannot have a prefix 
     * AND a name. 
     * For dynamic column names, this name can 
     * refer to members, by using curly brace
     * encapsulated names, e.g. "d_{id}_"
     * @return the prefix to all qualifier/column names
     */
	String prefix( ) default "";
}

