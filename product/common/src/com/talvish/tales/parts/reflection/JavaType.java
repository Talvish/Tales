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

package com.talvish.tales.parts.reflection;

import java.lang.reflect.Type;

import com.google.common.base.Preconditions;

/**
 * A class that holds references to the class and generic type information.
 * Technically the class information can be gotten from the type information
 * but we cache them together to ease overall access.
 * @author jmolnar
 *
 */
public class JavaType {
    private final Type type;
    private final Class<?> underlyingClass;
    private final JavaType supertype;

    /**
     * Constructor taking the type.
     * @param theType the type in question
     */
    public JavaType( Type theType ) {
    	Preconditions.checkNotNull( theType, "need a type" );
    	
    	type = theType;
    	underlyingClass = TypeUtility.extractClass( theType );    	
    	Preconditions.checkNotNull( underlyingClass, "Attempting to get a reference to a type, '%s' (of type '%s'), whose underlying type could not be retrieved, likely due to a generic type having a type parameter that is also a generic type, or an array using a generic type parameter.", theType.getTypeName(), theType.getClass().getSimpleName( ) );
    	if( underlyingClass.getGenericSuperclass( ) != null ) {
    		supertype = new JavaType( underlyingClass.getGenericSuperclass( ) );
    	} else {
    		supertype = null;
    	}
    }

    /**
     * The type of the class this reference represents.
     * @return the type of the class
     */
    public Type getType( ) {
    	return type;
    }    
    
    /**
     * The class associated with the type. 
     * This is essentially a cached value to ease work.
     * @return the class associated with the type.
     */
    public Class<?> getUnderlyingClass( ) {
    	return underlyingClass;
    }
    
    /**
     * The JavaType representation of the direct superclass the entity represented by this type. 
     * This will be null if there isn't a superclass.
     * @return the JavaType of the superclass
     */
    public JavaType getSupertype( ) {
    	return supertype;
    }

    /**
     * Simple method that returns the underlying type simple name.
     * @return
     */
    public String getSimpleName( ) {
    	return underlyingClass.getName();
    }

    /**
     * Simple method that returns the underlying type name.
     * @return
     */
    public String getName( ) {
    	return underlyingClass.getName();
    }

    /**
     * Equals method that ensures the types are the same. 
     * @param theObject the object to compare
     * @return true if the same, false otherwise
     */
    @Override
    public boolean equals( Object theObject) {
		if( theObject instanceof JavaType ) {
			return type.equals( ( ( JavaType )theObject ).type );
		} else {
			return false;
		}
    }
    
    /**
     * Hashcode based on the type.
     * @return the hashcode
     */
    @Override
    public int hashCode() {
    	return type.hashCode();
    }
}
