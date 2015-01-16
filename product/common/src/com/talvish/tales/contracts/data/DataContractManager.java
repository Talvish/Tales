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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.naming.SegmentedLowercaseValidator;
import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.TypeUtility;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.FieldSite;
import com.talvish.tales.serialization.SerializationTypeManager;

/**
 * Manager class that creates detailed objects for types that represent data contracts.
 * @author jmolnar
 *
 */
public class DataContractManager extends SerializationTypeManager< DataContractType, DataContractField> {
	public static final String CONTRACT_TYPE_NAME_VALIDATOR = "tales.contracts.contract_type_name";
	public static final String CONTRACT_MEMBER_NAME_VALIDATOR = "tales.contracts.contract_member_name";
	
	static {
		if( !NameManager.hasValidator( DataContractManager.CONTRACT_TYPE_NAME_VALIDATOR ) ) {
			NameManager.setValidator( DataContractManager.CONTRACT_TYPE_NAME_VALIDATOR, new SegmentedLowercaseValidator( ) );
		}
		if( !NameManager.hasValidator( DataContractManager.CONTRACT_MEMBER_NAME_VALIDATOR ) ) {
			NameManager.setValidator( DataContractManager.CONTRACT_MEMBER_NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}

    public DataContractManager( ) {
    	this( null, null );
    }
    
    public DataContractManager( NameValidator theTypeNameValidator, NameValidator theMemberNameValidator ) {
    	super(
    			DataContract.class,
    			theTypeNameValidator == null ? NameManager.getValidator( DataContractManager.CONTRACT_TYPE_NAME_VALIDATOR ) : theTypeNameValidator,
    			DataMember.class,
    			theMemberNameValidator == null ? NameManager.getValidator( DataContractManager.CONTRACT_MEMBER_NAME_VALIDATOR ) : theMemberNameValidator );
    }

	@Override
	protected DataContractType instantiateTypeDescriptor(
			String theTypeName,
			JavaType theType, 
			Method theDeserializationHook,
			boolean supportsValid, 
			DataContractType theBaseTypeDescriptor) {
		return new DataContractType( 
				theTypeName, 
				theType, 
				theDeserializationHook, 
				supportsValid, 
				theBaseTypeDescriptor );
	}
	
    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theField the field we are to look at 
     * @param theDeclaringType the type the field is a member of
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	@Override
	protected DataContractField generateField( Field theField, DataContractType theDeclaringType, Object aDeclaringInstance ) {
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
	        
	        if( !memberNameValidator.isValid( fieldName ) ) {
	        	throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", theDeclaringType.getType().getName(), fieldSite.getName(), fieldName, memberNameValidator.getClass().getSimpleName() ) );
	        }

	        // TODO: BELOW we should probably track the types in question and then come back to it later after we have processed all field
	        
	        // TODO: should highly consider failing these if they are generic but have not generic parameters
			if( Map.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) {
				// if we have a map (hashtable, treemap, etc)
				// we need to get type information for the keys and values

				// first we deal with the keys
				JavaType declaredKeyType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	    		List<ValueType<DataContractType,DataContractField>> keyTypes = extractValueTypes(theField, declaredKeyType, dataMemberAnnotation.keyTypes(), aDeclaringInstance );
	    		// if there was nothing on the attribute, then we use the type's key type itself
	            if( keyTypes.size() == 0 ) {
	            	keyTypes.add( new ValueType<>( declaredKeyType, generateType( declaredKeyType, aDeclaringInstance ) ) );
	            }
	            
	            // next we deal with the values
	    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 1 ] );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes(), aDeclaringInstance );
	    		// if there was nothing on the attribute, then we use the type's value type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, generateType( declaredValueType, aDeclaringInstance ) ) );
	            }
	            
	            dataContractField = new DataContractField( fieldName, keyTypes, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 
			
			} else if( ( Collection.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) ) {
				// if we have a collection (e.g list, set, collection itself, etc)
				// we need to get the type information for the collection element
	    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes(), aDeclaringInstance );
	    		// if there was nothing on the attribute, then we use the type's value type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, generateType( declaredValueType, aDeclaringInstance ) ) );
	            }
	            // now create the field object we need
	            dataContractField = new DataContractField( fieldName, FieldDescriptor.FieldValueType.COLLECTION, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 

	    	} else if( fieldClass.isArray( ) ) {
	    		// if we have an array we basically do the same thing as a collection which means
	    		// we need to get the type information for the array element
	    		JavaType declaredValueType = new JavaType( TypeUtility.extractComponentType( fieldGenericType ) );
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, declaredValueType, dataMemberAnnotation.valueTypes(), aDeclaringInstance );
	    		// if there was nothing on the attribute, then we use the type's component type itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( declaredValueType, generateType( declaredValueType, aDeclaringInstance ) ) );
	            }
	            // now create the field object we need
	            dataContractField = new DataContractField( fieldName, FieldDescriptor.FieldValueType.COLLECTION, valueTypes, fieldSite, theDeclaringType, theDeclaringType ); 

	    	} else {
	    		// we have either a simple type, primitive type, enum or non-collection complex type
	    		List<ValueType<DataContractType,DataContractField>> valueTypes = extractValueTypes(theField, fieldSite.getType(), dataMemberAnnotation.valueTypes(), aDeclaringInstance );
	    		// if there was nothing on the attribute, then we use the type of field itself
	            if( valueTypes.size() == 0 ) {
	            	valueTypes.add( new ValueType<>( fieldSite.getType(), generateType( fieldSite.getType(), aDeclaringInstance ) ) );
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
	private List<ValueType<DataContractType,DataContractField>> extractValueTypes( Field theField, JavaType theDeclaredType, Class<?>[] theDesiredTypes, Object aDeclaringInstance ) {
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
        		valueTypes.add( new ValueType<>( desiredType, generateType( desiredType, aDeclaringInstance ) ) );
        	}
        }

        return valueTypes;
	}
}
