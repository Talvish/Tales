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
package com.tales.parts.reflection;

import java.lang.reflect.Type;

import com.google.common.base.Preconditions;
//Element
//Descriptor
//Component
//Unit
//Aspect
//Detail
public class ValueType <T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
    protected final Class<?> type;
    protected final Type genericType;
    protected final T reflectedType;
    
    public ValueType( Class<?> theType ) {
    	this( theType, null, null );
    }

    public ValueType( Class<?> theType, Type theGenericType ) {
    	this( theType, theGenericType, null );
    }

    public ValueType( Class<?> theType, Type theGenericType, T theReflectedType ) {
    	Preconditions.checkNotNull( theType, "need a type" );
    	
    	type = theType;
    	genericType = theGenericType;
    	reflectedType = theReflectedType;
    }

    public Class<?> getType( ) {
    	return type;
    }
    
    public Type getGenericType( ) {
    	return genericType;
    }
    
    public T getReflectedType( ) {
    	return reflectedType;
    }
}
