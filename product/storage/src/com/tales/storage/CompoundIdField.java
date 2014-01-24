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
package com.tales.storage;

import com.google.common.base.Preconditions;
import com.tales.parts.reflection.ValueType;
import com.tales.parts.reflection.FieldDescriptor;
import com.tales.parts.sites.MemberSite;

/**
 * This class represents an id field in a compound id class for a 
 * storage type.
 * Also, the same compound id type can be used for more than one  
 * storage class, so nothing specific to the storage class is kept
 * here
 * @author jmolnar
 *
 */
public class CompoundIdField extends FieldDescriptor<CompoundIdType,CompoundIdField> {
    private final int order;
    
    /**
     * The constructor taking the nessary items.
     * @param theName the name of the field
     * @param theOrder the order in the id
     * @param theFieldSite the site location for the reflected field
     * @param theContainingType the type that contains this field
     */
    public CompoundIdField( String theName, int theOrder, MemberSite theFieldSite, CompoundIdType theContainingType ) {
    	// TODO: want to get reflected element somehow merged with the field site
    	super( theName, new ValueType<CompoundIdType, CompoundIdField>( theFieldSite.getType(), theFieldSite.getGenericType() ), theFieldSite, theContainingType, theContainingType );
    	Preconditions.checkArgument( theOrder > 0, "order must be greater than zero" );
        
        order = theOrder;
    }

    /**
     * The order of this member in the id.
     * @return
     */
    public int getOrder( ) {
    	return this.order;
    }
}