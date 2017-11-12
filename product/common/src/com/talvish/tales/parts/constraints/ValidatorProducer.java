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

import java.lang.annotation.Annotation;

import com.talvish.tales.parts.reflection.JavaType;

/**
 * Produces a validator for a particular value type. 
 * @author jmolnar
 *
 * @param <T> the type of annotation it produces annotations 
 */
public interface ValidatorProducer<T extends Annotation > {
	ValueValidator<?> produceValidator( T theAnnotation, JavaType theType );
}
