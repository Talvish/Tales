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

import java.math.BigDecimal;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.validation.constraints.Max;
import com.talvish.tales.validation.validators.MaxBigDecimalValidator;
import com.talvish.tales.validation.validators.MaxIntegerValidator;
import com.talvish.tales.validation.validators.MaxLongValidator;
import com.talvish.tales.validation.validators.ValueValidator;

/**
 * Produces a validator for the specified annotation and value type. 
 * @author jmolnar
 */
public class MaxProducer implements ValidatorProducer<Max> {

	/**
	 * Produces a ValueValidator for the specified Max annotation and associated type.
	 * @param theAnnotation the annotation instance
	 * @param theType the type the annotation was used on
	 */
	@Override
	public ValueValidator<?> produceValidator( Max theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "need an annotation" );
		Preconditions.checkNotNull( theType, "need a type to produce a validator for the Max annotation" );

		ValueValidator<?> validator;
		
		if( theType.getUnderlyingClass().equals( long.class ) || theType.getUnderlyingClass( ).equals( Long.class ) ) {
			validator = new MaxLongValidator( theAnnotation.value() ); 
			
		} else if( theType.getUnderlyingClass().equals( int.class ) || theType.getUnderlyingClass( ).equals( Integer.class ) ) {
			if( theAnnotation.value( ) < Integer.MIN_VALUE ) {
				throw new IllegalStateException( String.format( "Cannot put a Max annotation with value '%s' on type '%s' since it smaller than the type allows.", theAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
			} else {
				validator = new MaxIntegerValidator( ( int )theAnnotation.value( ) );
			}
			
		} else if( theType.getUnderlyingClass().equals( BigDecimal.class ) ) {
			validator = new MaxBigDecimalValidator( new BigDecimal( theAnnotation.value( ) ) );
			
		} else {
			throw new IllegalStateException( String.format( "Cannot put a Max annotation with value '%s' on unsupported type '%s'.", theAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
		}
		return validator;
	}
}
