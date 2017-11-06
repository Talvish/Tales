package com.talvish.tales.parts.constraints;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;

// TODO: these are static, but should they be?
// TODO: we could update the translations so they take validators
//       that are in fact type specific so we don't keep boxing items

public class ValidatorHelper {
	public static ValueValidator generateValidator( Annotation theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "Cannot generate a validator for a null annotation." );
		
		ValueValidator validator = null;
		
		// TODO: need to check that it makes sense given the parameter or member associated with it
		//       since it may not (e.g. min value higher than size of the item)
		
		if( theAnnotation instanceof Min ) {
			Min minAnnotation = ( Min )theAnnotation;
			
			if( theType.getUnderlyingClass().equals( long.class ) || theType.getUnderlyingClass( ).equals( Long.class ) ) {
				validator = new MinLongValidator( minAnnotation.value() ); 
			} else if( theType.getUnderlyingClass().equals( int.class ) || theType.getUnderlyingClass( ).equals( Integer.class ) ) {
				if( minAnnotation.value( ) > Integer.MAX_VALUE ) {
					throw new IllegalStateException( String.format( "Cannot put a Min annotation with value '%s' on type '%s' since it is bigger than the type allows.", minAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
				} else {
					validator = new MinIntegerValidator( ( int )minAnnotation.value( ) );
				}
			} else if( theType.getUnderlyingClass().equals( BigDecimal.class ) ) {
				validator = new MinBigDecimalValidator( new BigDecimal( minAnnotation.value( ) ) );
			} else {
				throw new IllegalStateException( String.format( "Cannot put a Min annotation with value '%s' on unsupported type '%s'.", minAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
			}
			
		} else if( theAnnotation instanceof Max ) {
			Max maxAnnotation = ( Max )theAnnotation;
			
			if( theType.getUnderlyingClass().equals( long.class ) || theType.getUnderlyingClass( ).equals( Long.class ) ) {
				validator = new MaxLongValidator( maxAnnotation.value() ); 
			} else if( theType.getUnderlyingClass().equals( int.class ) || theType.getUnderlyingClass( ).equals( Integer.class ) ) {
				if( maxAnnotation.value( ) < Integer.MIN_VALUE ) {
					throw new IllegalStateException( String.format( "Cannot put a Max annotation with value '%s' on type '%s' since it smaller than the type allows.", maxAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
				} else {
					validator = new MaxIntegerValidator( ( int )maxAnnotation.value( ) );
				}
			} else if( theType.getUnderlyingClass().equals( BigDecimal.class ) ) {
				validator = new MaxBigDecimalValidator( new BigDecimal( maxAnnotation.value( ) ) );
			} else {
				throw new IllegalStateException( String.format( "Cannot put a Max annotation with value '%s' on unsupported type '%s'.", maxAnnotation.value( ), theType.getUnderlyingClass().getSimpleName( ) ) );
			}

			
			
			validator = new MaxLongValidator( ( ( Max )theAnnotation ).value() ); 
		}
		return validator;
	}
	
	public static ValueValidator[] generateValidators( Annotation[] theAnnotations, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotations, "Cannot generate a validators for null annotations." );

		ArrayList<ValueValidator> validatorList = new ArrayList<ValueValidator>( theAnnotations.length );
		ValueValidator validator;
		
		for( Annotation annotation : theAnnotations ) {
			validator = generateValidator( annotation, theType );
			if( validator != null ) {
				validatorList.add( validator );
			}
		}
		
		ValueValidator[] validators = new ValueValidator[ validatorList.size( ) ];
		validatorList.toArray( validators );
		
		return validators;
	}
}
