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
 * This annotation is meant for fields within
 * a class to indicate they should be serialized 
 * when an instance of the class is serialized.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.FIELD )
public @interface StoredMember {
	/**
     * The name to give to the serialized field.
     * If none is given, it defaults to the name
     * of the members. For dynamic column names,
     * this name can refer to other members, or
     * itself by using curly brace encapsulated 
     * names, e.g. "{name}"
     * @return the serialized name of the class
     */
    String name( ) default "";
}