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

import java.lang.reflect.Method;

import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.serialization.SerializationType;

/**
 * This class represents a class which is to be serialized.
 * @author jmolnar
 */
public class DataContractType extends SerializationType< DataContractType, DataContractField > {
    /**
     * Constructor taking the name to use and the underlying type represented.
     * This implementation does not construct the fields since it would rely
     * on the assumption that only annotations can be used to create the 
     * objects. 
     * @param theName the name to give the type
     * @param theType the underlying type
     * @param theDeserializedHook the method to call if the type wants to participate when an instance is deserialized 
     * @param theValidationHook the method to call if the type wants to participate validation
     * @param theBaseType the base class, if applicable
     */
    protected DataContractType( String theName, JavaType theType, Method theDeserializedHook, Method theValidationHook, DataContractType theBaseType ) {
    	super( theName, theType, theDeserializedHook, theValidationHook, theBaseType  );
    }
}