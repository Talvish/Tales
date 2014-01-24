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
 * This annotation is meant for a class to indicate
 * that the class may be serialized.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE )
public @interface StoredType {
    /**
     * The name to give to the serialized class.
     * If none is given, it defaults to the simple
     * name of the class.
     * @return the serialized name of the class
     */
    String name( ) default "";
    
    /**
     * This is family name, a name prepended 
     * to each data member name (aka prepended
     * to family, or qualifier names)
     * @return
     */
    String family( ) default "";
}