// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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
 * This annotation marks this class as a holder of a set of configuration settings.
 * @author jmolnar
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE )
public @interface Settings {
    /**
     * This is used to identify the different classes that
     * may have settings. It currently doesn't serve a strong 
     * purpose.
     * If none is given, it defaults to the simple
     * name of the class.
     * @return an identifying more for this class of settings
     */
    String name( ) default "";
}