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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.tales.parts.reflection.TypeDescriptor;

/**
 * The type that represents the compound id for a storage type.
 * Not all storage types use a compound key, so this class may not
 * get used. 
 * Also, the same compound id type can be used for more than one  
 * storage class, so nothing specific to the storage class is kept
 * here
 * @author jmolnar
 *
 */
public class CompoundIdType extends TypeDescriptor<CompoundIdType, CompoundIdField> {
	private List<CompoundIdField> orderedFields = Collections.unmodifiableList( new ArrayList<CompoundIdField>( 0 ) );
	/**
	 * The constructor taking the needed parameters.
	 * @param theName the name of the compound id type
	 * @param theType the underlying reflected type
	 */
    public CompoundIdType( String theName, Class<?> theType ) {
    	super( theName, theType );
    }

    /**
     * Returns the fields, in the order they requested.
     */
    @Override
    public Collection<CompoundIdField> getFields() {
    	return orderedFields;
    }
    
    /**
     * Overrides the fields setter to ensure we have fields.
     */
    @Override
    public void setFields(Collection<CompoundIdField> theFields) {
    	Preconditions.checkArgument( theFields != null && theFields.size() > 1, "compound id types require two or more fields");
    	Preconditions.checkArgument( orderedFields.size() == 0, String.format( "compound type '%s' has had it's fields set already", this.getType().getName( ) ) );
    	
    	// now we save the fields
    	CompoundIdField[] newOrderedFields = new CompoundIdField[ theFields.size( ) ];
    	// set the fields, saving them in the order specified
    	for( CompoundIdField field : theFields ) {
    		int requestedLocation = field.getOrder() - 1; // they give us something that is one-based
    		if( requestedLocation < 0 || requestedLocation >= theFields.size( ) ) {
    			throw new IllegalStateException( String.format( "Compound id type '%s' has a field '%s' requesting an invalid position '%d.", this.getType( ).getName( ), field.getName(), requestedLocation + 1 ) );
    		} else if( newOrderedFields[ requestedLocation ] != null ) {
    			throw new IllegalStateException( String.format( "Compound id type '%s' has a field '%s' requesting position '%d' which is used by field '%s'.", this.getType( ).getName( ), field.getName(), requestedLocation + 1, newOrderedFields[ requestedLocation ].getName( ) ) );
    		} else {
    			newOrderedFields[ requestedLocation ] = field;
    		}
    	}
    	// save the newly set lsit
    	orderedFields = Collections.unmodifiableList( Arrays.asList( newOrderedFields ) );
    	// now set in the parent class (which allows lookups)
    	super.setFields(theFields);
    	
    }
}
