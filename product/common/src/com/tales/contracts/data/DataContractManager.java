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
package com.tales.contracts.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.ValidationSupport;
import com.tales.parts.reflection.ValueType;
import com.tales.parts.sites.FieldSite;
import com.tales.system.Facility;

/**
 * Manager class that creates detailed objects for types that represent data contracts.
 * @author jmolnar
 *
 */
public class DataContractManager implements Facility {
    private final Map<Class<?>, DataContractType> dataContractTypes = new ConcurrentHashMap< Class<?>, DataContractType>( 16, 0.75f, 1 );
    private final Object lock = new Object( );

    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
	public DataContractType generateType( Class<?> theType ) {
        Preconditions.checkNotNull( theType, "theType" );
        synchronized( lock ) { // TODO: need to support the symbol table mechanism instead
	        DataContract dataTypeAnnotation = theType.getAnnotation( DataContract.class );
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
	private DataContractType _generateType( Class<?> theType ) {
        DataContract dataTypeAnnotation = theType.getAnnotation( DataContract.class );
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
            Class<?> baseType = theType.getSuperclass( );
            DataContractType baseContractType = null;
            Collection<DataContractField> baseTypeFields = null;
            
            // if we have a base class, we analyze it
            if( baseType != null && !baseType.equals( Object.class ) ) {
                baseContractType = generateType( baseType ); // TODO: consider not having to have the parent have the annotation (but still look at the fields)
                baseTypeFields = baseContractType.getFields( );
            }

            // now create the data contract type
            DataContractType contractType = new DataContractType( 
            		typeName, 
            		theType,
            		ValidationSupport.class.isAssignableFrom( theType ),
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
            for( Field field : theType.getDeclaredFields( ) ) {
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
     * @param theContainingType the type the field is a member of
     * @param theField the field we are to look at 
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	private DataContractField generateField( DataContractType theContainingType, Field theField ) {
		DataContractField dataContractField = null;
        DataMember dataMemberAnnotation = theField.getAnnotation( DataMember.class );
        // if we have serialization declaration we will save it
        if( dataMemberAnnotation != null ) {
			Class<?> fieldType = theField.getType();
			Type fieldGenericType = theField.getGenericType();
			FieldSite fieldSite = new FieldSite( theField );
	
	        // make sure the field is accessible
	        theField.setAccessible( true ); 
	        // get the proper names for the field
	        String fieldName = Strings.isNullOrEmpty( dataMemberAnnotation.name( ) ) ? theField.getName( ) : dataMemberAnnotation.name( );
	
	        // TODO: BELOW we should probably track the types in question and then come back to it later after we have processed all field
	        
			if( Map.class.isAssignableFrom( fieldType ) && ( fieldGenericType instanceof ParameterizedType ) ) {
				// if we have a map (hashtable, treemap, etc)
				// we need to get type information for the keys and values
				ValueType<DataContractType, DataContractField> reflectedFieldType = new ValueType<DataContractType, DataContractField>( fieldType, fieldGenericType );

				Class<?> keyType = ( Class<?> )( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ];
	            ValueType<DataContractType, DataContractField> reflectedKeyType = new ValueType<DataContractType, DataContractField>( keyType, null, _generateType( keyType ) );
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 1 ];
	            ValueType<DataContractType, DataContractField> reflectedValueType = new ValueType<DataContractType, DataContractField>( valueType, null, _generateType( valueType ) );
	            
	            dataContractField = new DataContractField( fieldName, reflectedFieldType, reflectedKeyType, reflectedValueType, fieldSite, theContainingType, theContainingType ); 
			
			} else if( ( Collection.class.isAssignableFrom( fieldType ) && ( fieldGenericType instanceof ParameterizedType ) ) ) {
				// if we have a collection (e.g list, set, collection itself, etc)
				// we need to get the type information for the collection element
				ValueType<DataContractType, DataContractField> reflectedFieldType = new ValueType<DataContractType, DataContractField>( fieldType, fieldGenericType );

				Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	            ValueType<DataContractType, DataContractField> reflectedElementType = new ValueType<DataContractType, DataContractField>( elementType, null, _generateType( elementType ) );

	            dataContractField = new DataContractField( fieldName, reflectedFieldType, reflectedElementType, fieldSite, theContainingType, theContainingType ); 

	    	} else if( fieldType.isArray( ) ) {
	    		// if we have an array we basically do the same thing as a collection which means
	    		// we need to get the type information for the array element
				ValueType<DataContractType, DataContractField> reflectedFieldType = new ValueType<DataContractType, DataContractField>( fieldType, fieldGenericType );

				Class<?> elementType = fieldType.getComponentType();
	            ValueType<DataContractType, DataContractField> reflectedElementType = new ValueType<DataContractType, DataContractField>( elementType, null, _generateType( elementType ) );

	            dataContractField = new DataContractField( fieldName, reflectedFieldType, reflectedElementType, fieldSite, theContainingType, theContainingType ); 

	    	} else {
	    		// so we have either a simple type, primitive type, enum or non-collection complex type
				ValueType<DataContractType, DataContractField> reflectedFieldType = new ValueType<DataContractType, DataContractField>( fieldType, fieldGenericType, _generateType( fieldType ) );
	            dataContractField = new DataContractField( fieldName, reflectedFieldType, fieldSite, theContainingType, theContainingType ); 
	    	}
    	}
        return dataContractField;
	}
}
