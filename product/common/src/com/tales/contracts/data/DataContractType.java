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

import com.tales.parts.reflection.JavaType;
import com.tales.parts.reflection.TypeDescriptor;

/**
 * This class represents a class which is to be serialized.
 * @author jmolnar
 */
public class DataContractType extends TypeDescriptor< DataContractType, DataContractField> {
	private final boolean supportsValidation;
    private final DataContractType baseType;
    
    /**
     * Constructor taking the name to use and the underlying type represented.
     * This implementation does not construct the fields since it would rely
     * on the assumption that only annotations can be used to create the 
     * objects. 
     * @param theName the name to give the type
     * @param theType the underlying type
     * @param validationSupport indicates if the contract supports validation
     * @param theBaseType the base class, if applicable
     */
    DataContractType( String theName, JavaType theType, boolean validationSupport, DataContractType theBaseType ) {
    	super( theName, theType );
    	this.supportsValidation = validationSupport;
        this.baseType = theBaseType;        
    }
    
    /**
     * Indicates if the data contract type has validation supports.
     * @return true if the data contract type has validation support, false otherwise
     */
    public boolean supportsValidation( ) {
    	return this.supportsValidation;
    }

    /**
     * The type info for the superclass.
     * @return
     */
    public DataContractType getBaseType( ) {
    	return this.baseType;
    }
}
