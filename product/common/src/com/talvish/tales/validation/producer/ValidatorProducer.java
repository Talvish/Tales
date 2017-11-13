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
package com.talvish.tales.validation.producer;

import java.lang.annotation.Annotation;

import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.validation.validators.ValueValidator;

/**
 * Produces a validator for the specified annotation and value type. 
 * @author jmolnar
 *
 * @param <T> the type of annotation it produces annotations 
 */
public interface ValidatorProducer<T extends Annotation > {
	/**
	 * Produces a ValueValidator for the specified annotation and associated type.
	 * @param theAnnotation the annotation instance
	 * @param theType the type the annotation was used on
	 */
	ValueValidator<?> produceValidator( T theAnnotation, JavaType theType );
}
