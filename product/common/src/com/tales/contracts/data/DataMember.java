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
package com.tales.contracts.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is meant for fields to indicate
 * they are part of a contract.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.FIELD )
public @interface DataMember {
	/**
     * The name to give to the serialized field.
     * If none is given, it defaults to the name
     * of the members.
     * @return the serialized name of the class
     */
    String name( ) default "";
    /**
     * An optional set of types for key types used in maps.
     * @return the types allowed, or empty if directly based on the return type
     */
    Class<?>[] keyTypes( ) default {};
    /**
     * An optional set of types that are allowed for objects
     * values in collections and values in maps.
     * @return the types allowed, or empty if directly based on the return type
     */
    Class<?>[] valueTypes( ) default {};
}