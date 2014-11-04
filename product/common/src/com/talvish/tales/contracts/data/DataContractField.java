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

import java.util.List;

import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.MemberSite;

/**
 * This class represents a field that is meant to be serialized.
 * @author jmolnar
 */
public class DataContractField extends FieldDescriptor<DataContractType, DataContractField> {
    /**
     * Primary constructor used to create a field that isn't a collection, array or map.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     */
    DataContractField( 
    		String theName, 
    		FieldDescriptor.FieldValueType theFieldValueType,
    		List<ValueType<DataContractType, DataContractField>> theObjectTypes, 
    		MemberSite theFieldSite, 
    		DataContractType theDeclaringType, 
    		DataContractType theContainingType ) {
    	super( theName, theFieldValueType, theObjectTypes, theFieldSite, theDeclaringType, theContainingType );
    }

    /**
     * Primary constructor used to create a map.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theKeyType the type of the key for the map
     * @param theValueType the type of the value for the map
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     */
    DataContractField( 
    		String theName, 
    		List<ValueType<DataContractType, DataContractField>> theKeyTypes, 
    		List<ValueType<DataContractType, DataContractField>> theValueTypes, 
    		MemberSite theFieldSite, 
    		DataContractType theDeclaringType, 
    		DataContractType theContainingType ) {
    	super( theName, theKeyTypes, theValueTypes, theFieldSite, theDeclaringType, theContainingType );
    }

    /**
     * Clones the existing object but specifying a different current type, which will
     * be a subclass of the original declaring type.
     */
    DataContractField cloneForSubclass( DataContractType theContainingType ) {
    	if( this.isMap() ) {
	        return new DataContractField( 
	        		this.name,
	        		this.keyTypes,
	        		this.valueTypes,
	        		this.site, 
	        		this.declaringType, 
	        		theContainingType );
    		
    	} else {
	        return new DataContractField( 
	        		this.name,
	        		this.fieldValueType,
	        		this.valueTypes,
	        		this.site, 
	        		this.declaringType, 
	        		theContainingType );
    	}
    }
}
