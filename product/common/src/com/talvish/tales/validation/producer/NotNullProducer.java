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

import com.google.common.base.Preconditions;
import com.talvish.tales.validation.constraints.NotNull;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.validation.validators.NotNullValidator;
import com.talvish.tales.validation.validators.ValueValidator;

/**
 * Produces a validator for the specified annotation and value type. 
 * @author jmolnar
 *
 */
public class NotNullProducer implements ValidatorProducer<NotNull> {
	private static final NotNullValidator validator = new NotNullValidator( );

	/**
	 * Produces a ValueValidator for the specified NotNull annotation and associated type.
	 * @param theAnnotation the annotation instance
	 * @param theType the type the annotation was used on
	 */
	@Override
	public ValueValidator<?> produceValidator( NotNull theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "need an annotation" );
		Preconditions.checkNotNull( theType, "need a type to produce a validator for the NotNull annotation" );

		return validator;
	}
}
