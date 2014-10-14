package com.tales.parts.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.google.common.base.Preconditions;

public final class TypeUtility {
	/**
	 * This is a helper method that, given a type, will return the actual underlying Class.
	 * This is helpful when trying to extract/understand the actual class being used when
	 * instances of generic class have generic type parameters themselves. As an example:
	 * List<Map<String,Object>>.
	 * There are times when this will not work if the associate class cannot be found.
	 * Given the use case analyzing field members, return types, etc, this shouldnt' happen.
	 * This was based on work found here:
	 * http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 * @param theType the type to try to find 
	 * @return the class, though it may be null if it cannot be determined
	 */
	public static Class<?> extractClass( Type theType ) {
		Preconditions.checkNotNull( theType );
		if( theType instanceof Class<?>) {
			return ( Class<?> )theType;
			
		} else if( theType instanceof ParameterizedType ) {
			return extractClass( ( ( ParameterizedType )theType ).getRawType( ) );
			
		} else if( theType instanceof GenericArrayType) {
			Type componentType = ( ( GenericArrayType ) theType ).getGenericComponentType( );
		    Class<?> componentClass = extractClass( componentType );
		    if (componentClass != null ) {
		    	return Array.newInstance(componentClass, 0).getClass(); // we do this since it is the only way to make sure we have something created
		    } else {
		        return null;
		    }
		} else {
		    return null;
		}
	}
	
	/**
	 * Helper method that will, given a type, return the generic type 
	 * of the component type used for the array. As an example if
	 * the array was declared String[] then this will return the type
	 * for String.
	 * @param theArrayType the array type to get the component type for
	 * @return the type of the component
	 */
	public static Type extractComponentType( Type theArrayType ) {
		Preconditions.checkNotNull( theArrayType );
		Type componentType = null;
		
		if( theArrayType instanceof GenericArrayType ) {
			componentType = ( ( GenericArrayType )theArrayType ).getGenericComponentType( );
		} else if( theArrayType instanceof Class<?> ) {
			Class<?> arrayClass = ( Class<?> )theArrayType;
			if( arrayClass.isArray( ) && arrayClass.getComponentType() != null ) {
				componentType = arrayClass.getComponentType();
			} else {
				throw new IllegalArgumentException( String.format( "Class '%s' is not an array or is missing the component type.", theArrayType.getTypeName() ) );
			}			
		} else {
			throw new IllegalArgumentException( String.format( "Type '%s' is not an array so a component type cannot be extracted.", theArrayType.getTypeName() ) );
		}
		return componentType;
	}
	
	/**
	 * Based on the field given and the associate parent that declared it, this will
	 * determine the generic's based type to use. This is because in cases where 
	 * generic classes have members using the generic type parameters, Java doesn't
	 * give you the type information needed, but instead the TypeVariable.
	 * This cannot handle cases where the field is using more than one type variable.
	 * @param theDeclaringType the parent that declared the field
	 * @param theField the field being looked at
	 * @return the type of the field
	 */
	public static Type determineFieldType( Type theDeclaringType, Field theField ) {
		// NOTE: this will not help, I don't believe, when there field is a parametric
		//		 type using type variables from the parent (probably doesn't for generic
		//       arrays either)
		Type fieldType = theField.getGenericType();
		if( fieldType instanceof Class<?>) {
			return fieldType;
		} else if( fieldType instanceof ParameterizedType ) {
			return fieldType;
		} else if( fieldType instanceof GenericArrayType ) {
			return fieldType;
		} else if( fieldType instanceof TypeVariable ) {
			String fieldName = ( ( TypeVariable<?> )fieldType ).getName();
			ParameterizedType parentType = ( ParameterizedType )theDeclaringType;
			Class<?> parentClass = ( Class<?> )parentType.getRawType();
			
			Type actualFieldType = null;
			
			TypeVariable<?> parentTypeVariable;
			int index = 0;

			// so we go through the type parameters from the parent and look for
			// the same name (which we can do because we can assume the field
			// is declared in the containing type)
			for( ; index < parentClass.getTypeParameters( ).length; index += 1 ) {
				parentTypeVariable = parentClass.getTypeParameters()[ index ];
				if( parentTypeVariable.getName().equals( fieldName ) ) {
					// we found the variable, so we can now determine the field type
					actualFieldType = parentType.getActualTypeArguments()[ index ];
					break;
				}
			}
			if( actualFieldType == null ) {
				throw new IllegalStateException( String.format( "Could not find an appropriate field type for '%s.%s'.", theDeclaringType.getTypeName( ), theField.getName( )  ) );
			}
			return actualFieldType;
		} else {
			throw new IllegalArgumentException( String.format( "Could not create a field type for '%s.%s' due to unknown field type '%s'.", theDeclaringType.getTypeName( ), fieldType.getTypeName( ), fieldType.getClass( ).getName() ) );
		}	
	}
}
