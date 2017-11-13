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
import com.talvish.tales.validation.constraints.NotEmpty;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.validation.validators.NotEmptyValidator;
import com.talvish.tales.validation.validators.ValueValidator;

/**
 * Produces a validator for the specified annotation and value type. 
 * @author jmolnar
 */
public class NotEmptyProducer implements ValidatorProducer<NotEmpty> {
	/**
	 * Produces a ValueValidator for the specified NotEmpty annotation and associated type.
	 * @param theAnnotation the annotation instance
	 * @param theType the type the annotation was used on
	 */
	@Override
	public ValueValidator<?> produceValidator( NotEmpty theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "need an annotation" );
		Preconditions.checkNotNull( theType, "need a type to produce a validator for the NotEmpty annotation" );

		ValueValidator<?> validator;

		if( theType.getUnderlyingClass().equals( String.class ) ) {
			validator = new NotEmptyValidator( );
		} else {
			throw new IllegalStateException( String.format( "Cannot put a NotEmpty annotation with value on unsupported type '%s'.", theType.getUnderlyingClass().getSimpleName( ) ) );
		}

		return validator;
	}
}
