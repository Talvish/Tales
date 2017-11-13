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
package com.talvish.tales.serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.system.Facility;
import com.talvish.tales.validation.OnValidation;

/**
 * A helper class for systems that are trying to analyze classes to pull out fields and types 
 * for serialization purposes, be it to be serialized, from a serialized form, or both
 * @author jmolnar
 *
 */
public abstract class SerializationTypeManager <T extends SerializationType<T, F>, F extends SerializationField<T, F>> implements Facility {
	protected final Map<JavaType, T> typeDescriptors = new ConcurrentHashMap< JavaType, T>( 16, 0.75f, 1 );
    private final Object lock = new Object( );
    
    public SerializationTypeManager( ) { 
    }

    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	public T generateType( JavaType theType ) {
        Preconditions.checkNotNull( theType, "theType" );
        synchronized( lock ) { // TODO: need to support the symbol table mechanism instead
        	return generateType( theType, null );
        }
	}
	
    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	protected T generateType( JavaType theType, Object anInstance ) {
        if( !canGenerateTypeDescriptor( theType ) ) {
        	// we check if we can generate type descriptors for the given type
        	// if not (e.g. simple types like String, Integer, etc) then we
        	// return a null
        	return null;
        
        } else if( typeDescriptors.containsKey( theType ) ) {
        	// if we can generate, we see if we have already generated and if so
        	// we return a previously generated version
            return typeDescriptors.get( theType ); 

        } else {
        	// otherwise we can generate the type BUT we haven't done so, so we generate now ...
            
        	// we need to grab a default constructor here so we can create a instance that has default values
        	// this is need here since it will be sent to base classes that cannot be instantiated but may
        	// have default values
            if( anInstance == null ) {
            	anInstance = this.generateTypeInstance( theType.getUnderlyingClass( ) );
            }
        	
            // then let's get the base class
            JavaType baseType = theType.getSupertype( );
            T baseTypeDescriptor = null;
            Collection<F> baseTypeFieldDescriptors = null;
            
            // if we have a base class, we analyze it
            if( baseType != null && !baseType.getUnderlyingClass().equals( Object.class ) ) {
                baseTypeDescriptor = generateType( baseType, anInstance );
                if( baseTypeDescriptor != null ) { 
                	// base classes must have any annotation on them as well
                	baseTypeFieldDescriptors = baseTypeDescriptor.getFields( );
                }
            }
            
            // and we need to see if the class supports serialization and validation hooks
            // but we start with setting the validation hook to what the base class may have
            // it may still be overwritten later if the subclass defined methods 
            Method baseDeserializedHook = baseTypeDescriptor != null ? baseTypeDescriptor.getDeserializationHook( ) : null;
            Method baseValidationHook = baseTypeDescriptor != null ? baseTypeDescriptor.getValidationHook( ) : null;

            Method deserializedHook = null;
            Method validationHook = null;
            
            for( Method method : theType.getUnderlyingClass().getDeclaredMethods( ) ) {
            	if( method.getAnnotation( OnDeserialized.class ) != null ) {
            		Preconditions.checkState( deserializedHook == null, "'%s' already has a deserialized hook, '%s', defined.", theType.getName( ), method.getName( ) );
            		deserializedHook = method;
            		deserializedHook.setAccessible( true ); // make sure we can access it
            		// we keep looping in case more than one hook is defined
            	} else if( method.getAnnotation( OnValidation.class ) != null ) {
            		Preconditions.checkState( validationHook == null, "'%s' already has a validation hook, '%s', defined.", theType.getName( ), method.getName( ) );
            		validationHook = method;
            		validationHook.setAccessible( true ); // make sure we can access it
            		// we keep looping in case more than one hook is defined
            	}
            }

            // if we didn't find any hooks, we use the hooks from the base class, if available
            if( deserializedHook == null ) {
            	deserializedHook = baseDeserializedHook;
            }
            if( validationHook == null ) {
            	validationHook = baseValidationHook;
            }

            // now create the type descriptor
            T typeDescriptor = generateTypeDescriptor( 
            		theType,
            		deserializedHook,
            		validationHook,
            		baseTypeDescriptor );
            F fieldDescriptor = null;
            
            // we now store this early since the children types (as we recurse) may need it
            typeDescriptors.put( theType, typeDescriptor );

            // we now prepare to get all the fields
            ArrayList<F> fieldDescriptors = new ArrayList<F>( );

            // first put the base type fields into what will be our field collection
            if( baseTypeFieldDescriptors != null ) {
                for( F baseFieldDescriptor : baseTypeFieldDescriptors ) {
                    F clonedFieldDescriptor = baseFieldDescriptor.cloneForSubclass( typeDescriptor );
                    fieldDescriptors.add( clonedFieldDescriptor ); // now we add a copy to the new type (even if the base has it)
                }
            }

            // now grab the directly declared fields
            for( Field field : theType.getUnderlyingClass().getDeclaredFields( ) ) {
            	fieldDescriptor = generateFieldDescriptor( field, typeDescriptor, anInstance );
            	if( fieldDescriptor != null ) {
            		fieldDescriptors.add( fieldDescriptor );
            	}
            }
            // now save the list of fields on the type
            typeDescriptor.setFields( fieldDescriptors );
            
            return typeDescriptor;
        }
    }

	/**
	 * This helper method helps determine which types can or cannot be
	 * generated for. As an example it could be we cannot generate for
	 * simple types (e.g. String) or if an annotation is missing, etc. 
	 * @param theType the type to verify if we can generate for
	 * @return true means we can generate, false means we cannot
	 */
	abstract protected boolean canGenerateTypeDescriptor( JavaType theType );
	
	/**
	 * Creates an instance of a particular type descriptor.
	 * @param theType the type to generate for
	 * @param theDeserializationHook a deserialization hook to use
	 * @param supportsValid indicates if the type supports validation
	 * @param theBaseTypeDescriptor the parent/base for the type, if any
	 * @return a newly minted type descriptor for the particular type
	 */
	abstract protected T generateTypeDescriptor( 
			JavaType theType, 
			Method theDeserializationHook,
			Method theValidationHook, 
			T theBaseTypeDescriptor );

	protected Object generateTypeInstance( Class<?> theClass ) {
		Constructor<?> defaultConstructor;
		
        try {
			defaultConstructor = theClass.getDeclaredConstructor( );
			defaultConstructor.setAccessible( true );
		} catch (SecurityException | NoSuchMethodException e) {
			throw new IllegalArgumentException( String.format( "Class '%s' does not have an accessible constructor.", theClass.getSimpleName() ) );
		}
        try {
			return defaultConstructor.newInstance( );
        } catch( IllegalArgumentException |  InvocationTargetException | InstantiationException | IllegalAccessException e ) {
            throw new IllegalStateException( String.format( "Cannot create a new instance of class '%s'.", theClass.getSimpleName( ) ), e );
        }
	}
	
    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theField the field we are to look at 
     * @param theDeclaringType the type the field is a member of
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	abstract protected F generateFieldDescriptor( Field theField, T theDeclaringType, Object aDeclaringInstance );
}
