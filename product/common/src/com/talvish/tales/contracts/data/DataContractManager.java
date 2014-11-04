// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.contracts.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.ValidationSupport;
import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.TypeUtility;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.FieldSite;
import com.talvish.tales.system.Facility;

/**
 * Manager class that creates detailed objects for types that represent data contracts.
 * @author jmolnar
 *
 */
public class DataContractManager implements Facility {
    private final Map<JavaType, DataContractType> dataContractTypes = new ConcurrentHashMap< JavaType, DataContractType>( 16, 0.75f, 1 );
    private final Object lock = new Object( );

    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	public DataContractType generateType( JavaType theType ) {
        Preconditions.checkNotNull( theType, "theType" );
        synchronized( lock ) { // TODO: need to support the symbol table mechanism instead
	        DataContract dataTypeAnnotation = theType.getUnderlyingClass().getAnnotation( DataContract.class );
	        if( dataTypeAnnotation == null ) {
	            throw new IllegalArgumentException( String.format( "Data type '%s' does not have the DataContract annotation.", theType.getName( ) ) );
	        } else {
	        	return _generateType( theType );
	        }
        }
	}
	
    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	private DataContractType _generateType( JavaType theType ) {
        DataContract dataTypeAnnotation = theType.getUnderlyingClass().getAnnotation( DataContract.class );
        if( dataTypeAnnotation == null ) {
        	// if no annotation, then not a contract data type, so return null
        	// and the caller can decide what to do
        	return null;
        } else if( dataContractTypes.containsKey( theType ) ) {
        	// if there is an annotation, we check to see if we have seen it and return it if so
            return dataContractTypes.get( theType );
        } else {
        	// otherwise, we have an annotation BUT we haven't seen it before so must build it up
        	// which may result in recursive calls when it gets down to the fields
            
        	// get the names we want
            String typeName =  Strings.isNullOrEmpty( dataTypeAnnotation.name( ) ) ? theType.getSimpleName( ) : dataTypeAnnotation.name( );

            // then let's get the base class
            JavaType baseType = theType.getSupertype( );
            DataContractType baseContractType = null;
            Collection<DataContractField> baseTypeFields = null;
            
            // if we have a base class, we analyze it
            if( baseType != null && !baseType.getUnderlyingClass().equals( Object.class ) ) {
                baseContractType = generateType( baseType ); // TODO: consider not having to have the parent have the annotation (but still look at the fields)
                baseTypeFields = baseContractType.getFields( );
            }

            // now create the data contract type
            DataContractType contractType = new DataContractType( 
            		typeName, 
            		theType,
            		ValidationSupport.class.isAssignableFrom( theType.getUnderlyingClass() ),
            		baseContractType );
            DataContractField contractField = null;
            
            // we now store this early since the children types (as we recurse) may need it
            dataContractTypes.put( theType, contractType );

            // we now prepare to get all the fields
            ArrayList<DataContractField> fields = new ArrayList<DataContractField>( );

            // first put the base type fields into what will be our field collection
            if( baseTypeFields != null ) {
                for( DataContractField baseContractField : baseTypeFields ) {
                    DataContractField fieldClone = ( DataContractField )baseContractField.cloneForSubclass( contractType );
                    fields.add( fieldClone ); // now we add a copy to the new type (even if the base has it)
                }
            }

            // now grab the directly declared fields
            for( Field field : theType.getUnderlyingClass().getDeclaredFields( ) ) {
            	contractField = generateField( contractType, field );
            	if( contractField != null ) {
            		fields.add( contractField );
            	}
            }
            // now save the list of fields on the type
            contractType.setFields( fields );
            
            return contractType;
        }
    }

    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theDeclaringType the type the field is a member of
     * @param theField the field we are to look at 
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	private DataContractField generateField( DataContractType theDeclaringType, Field theField ) {
		DataContractField dataContractField = null;
        DataMember dataMemberAnnotation = theField.getAnnotation( DataMember.class );
        // if we have serialization declaration we will save it
        if( dataMemberAnnotation != null ) {
			Class<?> fieldClass = theField.getType();
			Type fieldGenericType = theField.getGenericType();
			FieldSite fieldSite = new FieldSite( theDeclaringType.getType( ).getType(), theField ); // we use this constructor to ensure we get fields that use type parameters
	
	        // make sure the field is accessible
	        theField.setAccessible( true ); 
	        // get the proper names for the field
	        String fieldName = Strings.isNullOrEmpty( dataMemberAnnotation.name( ) ) ? theField.getName( ) : dataMemberAnnotation.name( );
	        
	        // TODO: BELOW we should probably track the types in question and then come back to it later after we have processed all field
	        
	        // TODO: should highly consider failing these if they are generic but have not generic parameters
			if( Map.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) {
				// if we have a map (hashtable, treemap, etc)
				// we need to get type information for the keys and values

				// first we deal with the keys
				JavaType declaredKeyType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	    		List<ValueType<DataContractType,DataContractField>> keyTypes = extractValueTypes(theField, declaredKeyType, dataMemberAnnotation.keyTypes() );
	    		// if there was nothing on the attribute, then we use the type's key type itself
	            if( keyTypes.size() == 0 ) {
	            	keyTypes.add( new ValueType<>( declaredKeyType, _generateType( declaredKeyType ) ) );
	            }
	            
	            // next we deal with the values
	    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 1 ] );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes() );
	    		// if there was nothing on the attribute, then we use the type's value type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, _generateType( declaredValueType ) ) );
	            }
	            
	            dataContractField = new DataContractField( fieldName, keyTypes, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 
			
			} else if( ( Collection.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) ) {
				// if we have a collection (e.g list, set, collection itself, etc)
				// we need to get the type information for the collection element
	    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes() );
	    		// if there was nothing on the attribute, then we use the type's value type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, _generateType( declaredValueType ) ) );
	            }
	            // now create the field object we need
	            dataContractField = new DataContractField( fieldName, FieldDescriptor.FieldValueType.COLLECTION, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 

	    	} else if( fieldClass.isArray( ) ) {
	    		// if we have an array we basically do the same thing as a collection which means
	    		// we need to get the type information for the array element
	    		JavaType declaredValueType = new JavaType( TypeUtility.extractComponentType( fieldGenericType ) );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes() );
	    		// if there was nothing on the attribute, then we use the type's component type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, generateType( declaredValueType ) ) );
	            }
	            // now create the field object we need
	            dataContractField = new DataContractField( fieldName, FieldDescriptor.FieldValueType.COLLECTION, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 

	    	} else {
	    		// we have either a simple type, primitive type, enum or non-collection complex type
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, fieldSite.getType(), dataMemberAnnotation.valueTypes() );
	    		// if there was nothing on the attribute, then we use the type of field itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( fieldSite.getType(), _generateType( fieldSite.getType() ) ) );
	            }
	            // now create the field object we need
	            dataContractField = new DataContractField( fieldName, FieldDescriptor.FieldValueType.OBJECT, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 
	    	}
    	}
        return dataContractField;
	}
	
	/**
	 * Helper method that ensures that each of the desired types that are request can be 
	 * assigned to the type used by the field and then setups up the set of value types
	 * @param theField the field in question
	 * @param theDeclaredType the Type of the field
	 * @param theDesiredTypes the set of types request by the developer for use as polymorphic data
	 * @return the list of value types that can be used on the field
	 */
	private List<ValueType<DataContractType,DataContractField>> extractValueTypes( Field theField, JavaType theDeclaredType, Class<?>[] theDesiredTypes ) {
        // we grab the value types field from the annotation,
        // for each type it lists, we confirm that it can be held
        // by the type on the object itself (no type mis-matches)
        // then for each type we need to do a generate type on
        // to ensure it will work out okay and then we store that 
        // into the data contract field
		
		// as a note, the desired types do not have generic information 
		// and therefore we cannot get Types with generic information 
		// this is because an annotation was used to declare the class
		// and generic information cannot be specified
		
		List<ValueType<DataContractType,DataContractField>> valueTypes = new ArrayList<>( );
		JavaType desiredType;
        for( Class<?> desiredClass : theDesiredTypes ) {
        	if( !theDeclaredType.getUnderlyingClass().isAssignableFrom( desiredClass ) ) {
        		throw new IllegalArgumentException( String.format(
        				"The data member annotation on '%s.%s' refers to a desired value type '%s' which cannot be cast to the declared value type '%s'.",
        				theField.getDeclaringClass().getName( ),
        				theField.getName(),
        				desiredClass.getName(),
        				theDeclaredType.getName( ) ) );
        	} else {
        		desiredType = new JavaType( desiredClass );
        		valueTypes.add( new ValueType<>( desiredType, _generateType( desiredType ) ) );
        	}
        }

        return valueTypes;
	}
}
