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
package com.talvish.tales.system.configuration.annotated;

import java.lang.reflect.Method;

import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.serialization.SerializationType;

/**
 * This class represents a class which contains setting fields for configuration.
 * @author jmolnar
 */
public class SettingType extends SerializationType< SettingType, SettingField > {
	private final String settingPrefix;
	
    /**
     * Constructor taking the name to use and the underlying type represented.
     * This implementation does not construct the fields since it would rely
     * on the assumption that only annotations can be used to create the 
     * objects. 
     * @param theName the name to give the type
     * @param theType the underlying type
     * @param theDeserializedHook the method to call if the type wants to participate when an instance is deserialized 
     * @param theValidationHook the method called if the instance is going to participate in validation
     * @param theBaseType the base class, if applicable
     */
    protected SettingType( String theName, JavaType theType, Method theDeserializedHook, Method theValidationHook, String theSettingPrefix, SettingType theBaseType ) {
    	super( theName, theType, theDeserializedHook, theValidationHook, theBaseType );
    
    	settingPrefix = theSettingPrefix;
    }
    
    /**
     * The prefix to use for settings that request a prefix.
     * @return prefix to use for setting names for settings that request a prefix
     */
    public String getSettingPrefix( ) {
    	return settingPrefix;
    }
}
