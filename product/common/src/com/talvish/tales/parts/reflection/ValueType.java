// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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

import com.google.common.base.Preconditions;

/**
 * This is a helper class that maps a TypeDescriptor to a particular JavaType.
 * @author jmolnar
 *
 * @param <T> the TypeDescriptor associated with the JavaType
 * @param <F> the FieldDescriptor associatec with the JavaType
 */
public class ValueType <T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
    protected final JavaType type;
    protected final T reflectedType;
    
    public ValueType( JavaType  theType ) {
    	this( theType, null );
    }

    public ValueType( JavaType theType, T theReflectedType ) {
    	Preconditions.checkNotNull( theType, "need a type" );
    	
    	type = theType;
    	reflectedType = theReflectedType;
    }

    public JavaType getType( ) {
    	return type;
    }
    
    public T getReflectedType( ) {
    	return reflectedType;
    }
}
