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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.ValidationSupport;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.system.Facility;

/**
 * A helper class for systems that are trying to analyze classes to pull out fields and types 
 * for serialization purposes, be it to be serialized, from a serialized form, or both
 * @author jmolnar
 *
 */
public abstract class SerializationTypeManager <T extends SerializationType<T, F>, F extends SerializationField<T, F>> implements Facility {

	protected final NameValidator typeNameValidator;
	protected final NameValidator memberNameValidator;

	protected final Map<JavaType, T> typeDescriptors = new ConcurrentHashMap< JavaType, T>( 16, 0.75f, 1 );
    private final Object lock = new Object( );
    
    protected final Class<? extends Annotation> typeAnnotationClass;
    protected final Class<? extends Annotation> memberAnnotationClass;

    
    public SerializationTypeManager( 
    		Class<? extends Annotation> theTypeAnnotationClass, 
    		NameValidator theTypeNameValidator, 
    		Class<? extends Annotation> theMemberAnnotationClass, 
    		NameValidator theMemberNameValidator ) {
		Preconditions.checkNotNull( theTypeNameValidator, "missing a type name validator" );
        Preconditions.checkNotNull( theMemberNameValidator, "missing a member name validator" );
        
        typeAnnotationClass = theTypeAnnotationClass;
        typeNameValidator = theTypeNameValidator;

        memberNameValidator = theMemberNameValidator;
        memberAnnotationClass = theMemberAnnotationClass;
    }
    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	public T generateType( JavaType theType ) {
        Preconditions.checkNotNull( theType, "theType" );
        synchronized( lock ) { // TODO: need to support the symbol table mechanism instead
	        Annotation typeAnnotation = theType.getUnderlyingClass().getAnnotation( typeAnnotationClass );
	        if( typeAnnotation == null ) {
	            throw new IllegalArgumentException( String.format( "Manager '%s' was given type '%s' to analyze, but the type doesn't have the '%s' annotation.", this.getClass().getSimpleName( ), theType.getSimpleName( ), typeAnnotationClass.getSimpleName( ) ) );
	        } else {
	        	return generateType( theType, null );
	        }
        }
	}
	
    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	protected T generateType( JavaType theType, Object anInstance ) {
		Annotation typeAnnotation = theType.getUnderlyingClass().getAnnotation( typeAnnotationClass );
		
        if( typeAnnotation == null ) {
        	// if no annotation, then not a contract data type, so return null
        	// and the caller can decide what to do
        	return null;
        } else if( typeDescriptors.containsKey( theType ) ) {
        	// if there is an annotation, we check to see if we have seen it and return it if so
            return typeDescriptors.get( theType );
        } else {
        	// otherwise, we have an annotation BUT we haven't seen it before so must build it up
        	// which may result in recursive calls when it gets down to the fields
            
        	// get the names we want
        	// since we cannot do inheritance of types, we look for the method
        	String typeName = null;
        	try {
	        	Method nameMethod = typeAnnotation.annotationType().getMethod( "name" );
	        	typeName = ( String )nameMethod.invoke( typeAnnotation );
	        	
	        	if( Strings.isNullOrEmpty( typeName ) ) {
	        		typeName = theType.getSimpleName();
	        	}
	
				if( !typeNameValidator.isValid( typeName ) ) {
					throw new IllegalStateException( String.format( "Type '%s' is using the name '%s' that does not conform to validator '%s'.", theType.getUnderlyingClass().getSimpleName(), typeName, typeNameValidator.getClass().getSimpleName() ) );
				}
        	} catch( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				throw new IllegalStateException( String.format( "Type '%s' has annotation of type '%s' but a 'name' method could not be found or could not be retrieved.", theType.getUnderlyingClass().getSimpleName(), typeAnnotationClass.getSimpleName( ) ), e );
        	}
        	
        	// we need to grab a default constructor here so we can create a instance that has default values
        	// this is need here since it will be sent to base classes that cannot be instantiated but may
        	// have default values
            if( anInstance == null ) {
            	anInstance = this.generateInstance( theType.getUnderlyingClass( ) );
            }
        	
            // then let's get the base class
            JavaType baseType = theType.getSupertype( );
            T baseTypeDescriptor = null;
            Collection<F> baseTypeFieldDescriptors = null;
            
            // if we have a base class, we analyze it
            if( baseType != null && !baseType.getUnderlyingClass().equals( Object.class ) ) {
                baseTypeDescriptor = generateType( baseType, anInstance );
                // TODO: by calling this generateType, IF there isn't an annotation on the class
                //		 then it won't consider the children fields, not sure if I like that
                if( baseTypeDescriptor != null ) { 
                	baseTypeFieldDescriptors = baseTypeDescriptor.getFields( );
                }
            }
            
            // and we need to see if the class supports serialization hooks
            Method deserializedHook = null;
            
            for( Method method : theType.getUnderlyingClass().getDeclaredMethods( ) ) {
            	if( method.getAnnotation( OnDeserialized.class ) != null ) {
            		Preconditions.checkState( deserializedHook == null, "'%s' already has a deserialized hook, '%s', defined.", theType.getName( ), method.getName( ) );
            		deserializedHook = method;
            		deserializedHook.setAccessible( true ); // make sure we can access it
            		// we keep looping in case more than one hook is defined
            	}
            }

            // now create the type descriptor
            T typeDescriptor = instantiateTypeDescriptor( 
            		typeName, 
            		theType,
            		deserializedHook,
            		ValidationSupport.class.isAssignableFrom( theType.getUnderlyingClass() ), // TODO: not sure I like the idea of using inheritance
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
            	fieldDescriptor = generateField( field, typeDescriptor, anInstance );
            	if( fieldDescriptor != null ) {
            		fieldDescriptors.add( fieldDescriptor );
            	}
            }
            // now save the list of fields on the type
            typeDescriptor.setFields( fieldDescriptors );
            
            return typeDescriptor;
        }
    }
	
	protected Object generateInstance( Class<?> theClass ) {
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
	
	abstract protected T instantiateTypeDescriptor( 
			String theTypeName, 
			JavaType theType, 
			Method theDeserializationHook,
			boolean supportsValid, 
			T theBaseTypeDescriptor );

    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theField the field we are to look at 
     * @param theDeclaringType the type the field is a member of
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	abstract protected F generateField( Field theField, T theDeclaringType, Object aDeclaringInstance );
}
