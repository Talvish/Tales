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
package com.talvish.tales.validation.validators;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.system.Facility;
import com.talvish.tales.validation.constraints.Max;
import com.talvish.tales.validation.constraints.Min;
import com.talvish.tales.validation.constraints.NotEmpty;
import com.talvish.tales.validation.constraints.NotNull;
import com.talvish.tales.validation.producer.MaxProducer;
import com.talvish.tales.validation.producer.MinProducer;
import com.talvish.tales.validation.producer.NotEmptyProducer;
import com.talvish.tales.validation.producer.NotNullProducer;
import com.talvish.tales.validation.producer.ValidatorProducer;

/**
 * A manager that manages validators for particular annotations.
 * @author jmolnar
 *
 */
public class ValidatorManager implements Facility {
	private Map<Class<? extends Annotation>,ValidatorProducer<?>> producers;
	private final Object lock = new Object();
	
	private static volatile ValidatorManager instance;
	private static final Object instanceLock = new Object( );

	// TODO: this is temporary
	public static ValidatorManager getInstance( ) {
		if( instance == null ) {
			synchronized( instanceLock ) {
				if( instance == null ) {
					instance = new ValidatorManager( );
				}
			}
		}
		return instance;
	}
	
	
	public ValidatorManager( ) {
		producers = new HashMap< Class<? extends Annotation>, ValidatorProducer<?> >( );
		
		// let's do some default registration, without checking since we are in the constructor
		producers.put( Min.class, new MinProducer( ) );
		producers.put( Max.class, new MaxProducer( ) );
		producers.put( NotNull.class, new NotNullProducer( ) );
		producers.put( NotEmpty.class, new NotEmptyProducer( ) );
		
		producers = Collections.unmodifiableMap( producers );
	}
	
	/**
	 * Returns all of the producers
	 * @return the collection of producers
	 */
	public Collection<ValidatorProducer<?>> getProducers( ) {
		return producers.values();
	}

	/**
	 * Retrieves a particular producer for a type of annotation
	 * @param the annotation class for which a producer will be grabbed
	 * @return the producer if found, null otherwise
	 */
	public ValidatorProducer<?> getProducer( Class<? extends Annotation> theClass ) {
		Preconditions.checkNotNull( theClass, "need an annotation class" );
		return producers.get( theClass );
	}
	
	/**
	 * Simply checks to see if the annotation class has been previously registered.
	 * @param theClass the type of annotation to check
	 * @return returns true if the type of annotation has previously registeres, false otherwise
	 */
	public boolean isRegistered( Class<? extends Annotation> theClass ) {
		return producers.containsKey( theClass );
	}
	
	/**
	 * Registers a producer with the manager. 
	 * If a producer for a particular annotation class already exist then 
	 * an exception is thrown.
	 * @param theClass the annotation class to register for
	 * @param theProducer the validator producer to register
	 */
	public <T extends Annotation> void register( Class<T> theClass, ValidatorProducer<T> theProducer ) {
		Preconditions.checkNotNull( theClass, "need a class to register for" );
		Preconditions.checkNotNull( theProducer, String.format( "need the producer for the annotation class '%s'", theClass.getName( ) ) );

		synchronized( lock ) {
			if( producers.containsKey( theClass ) ) {
				throw new IllegalStateException( String.format( "Producer for type '%s' is already registered.", theClass.getName( ) ) );
			} else {
				Map<Class<? extends Annotation>,ValidatorProducer<?>> newproducers = new HashMap< Class<? extends Annotation>, ValidatorProducer<?> >( producers );
				
				newproducers.put( theClass, theProducer);
				producers = Collections.unmodifiableMap( newproducers );
			}
		}
	}
	
	/**
	 * Asks the manager to generate a validator for the specified annotation and type.
	 * @param theAnnotation the instance of an annotation to generate for
	 * @param theType the type of object the annotation is for
	 * @return the validator
	 */
	public <T extends Annotation> ValueValidator<?> generateValidator( T theAnnotation, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotation, "Cannot generate a validator for a null annotation." );
		Preconditions.checkNotNull( theType, "Cannot generate a validator for the annotation of type '%s' when the type is null.", theAnnotation.getClass().getName( ) );
		
		@SuppressWarnings("unchecked")
		ValidatorProducer<T> producer = ( ValidatorProducer<T> )producers.get( theAnnotation.annotationType( ) );
		Preconditions.checkNotNull( producer, "Cannot generate a validator for unregister annotation type '%s'.", theAnnotation.annotationType( ).getName( ) );
		return producer.produceValidator(theAnnotation, theType);
	}
	
	/**
	 * Asks the manager to generate validators for set of annotations and type.
	 * @param theAnnotations the instance of an annotations to generate for
	 * @param theType the type of object the annotations is for
	 * @return a series of validators generate for the annotation
	 */
	public ValueValidator<?>[] generateValidators( Annotation[] theAnnotations, JavaType theType ) {
		Preconditions.checkNotNull( theAnnotations, "Cannot generate a validators for null annotations." );

		ArrayList<ValueValidator<?>> validatorList = new ArrayList<ValueValidator<?>>( theAnnotations.length );
		ValueValidator<?> validator;
		
		for( Annotation annotation : theAnnotations ) {
			if( isRegistered( annotation.annotationType( ) )  ) {
				validator = generateValidator( annotation, theType );
				validatorList.add( validator );
			}
		}
		
		ValueValidator<?>[] validators = new ValueValidator<?>[ validatorList.size( ) ];
		validatorList.toArray( validators );
		
		return validators;
	}
}
