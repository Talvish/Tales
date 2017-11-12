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

import java.math.BigDecimal;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;

public class MinProducer implements ValidatorProducer<Min> {

	@Override
	public ValueValidator<?> produceValidator( Min theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "need an annotation" );
		Preconditions.checkNotNull( theType, "need a type to produce a validator for the Min annotation" );
		
		ValueValidator<?> validator;
		
		if( theType.getUnderlyingClass().equals( long.class ) || theType.getUnderlyingClass( ).equals( Long.class ) ) {
			validator = new MinLongValidator( theAnnotation.value() ); 
		} else if( theType.getUnderlyingClass().equals( int.class ) || theType.getUnderlyingClass( ).equals( Integer.class ) ) {
			if( theAnnotation.value( ) > Integer.MAX_VALUE ) {
				throw new IllegalStateException( String.format( "Cannot put a Min annotation with value '%s' on type '%s' since it is bigger than the type allows.", theAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
			} else {
				validator = new MinIntegerValidator( ( int )theAnnotation.value( ) );
			}
		} else if( theType.getUnderlyingClass().equals( BigDecimal.class ) ) {
			validator = new MinBigDecimalValidator( new BigDecimal( theAnnotation.value( ) ) );
		} else {
			throw new IllegalStateException( String.format( "Cannot put a Min annotation with value '%s' on unsupported type '%s'.", theAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
		}

		return validator;
	}
}
