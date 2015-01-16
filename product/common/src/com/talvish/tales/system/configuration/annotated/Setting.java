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
package com.talvish.tales.system.configuration.annotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is meant for fields to indicate they represent a setting value.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.FIELD )
public @interface Setting {
	/**
     * This represents the setting name from the configuration source.
     * If none is given, it defaults to the name of the member.
     * @return the name of the setting
     */
    String name( ) default "";

    /**
     * Indicates if this member is considered required, meaning that 
     * when settings are loaded that the setting must appear in the
     * source and the default value is not to be used.
     * @return true if required, false otherwise
     */
    boolean required( ) default false;
}