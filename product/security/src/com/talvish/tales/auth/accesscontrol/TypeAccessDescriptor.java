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
package com.talvish.tales.auth.accesscontrol;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * The descriptor for the type that contains methods that are access controlled.
 * The types and methods work within the scope of a specific access control manager.
 * @author jmolnar
 *
 */
public class TypeAccessDescriptor {
	// in the future we could consider putting some shared claims 
	// at the root for the type that all children must have
	private final Class<?> type;
	private final Map<String, MethodAccessDescriptor> accessMethods = new HashMap<>( );
	private final Map<String, MethodAccessDescriptor> externalAccessMethods = Collections.unmodifiableMap( accessMethods );

	/**
	 * The constructor taking the type and access control manager that this descriptor
	 * will be part of.
	 * @param theType the reflected type this descriptor represents
	 */
	public TypeAccessDescriptor( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "cannot create the authentication type for the methods of a null type" );
		
		type = theType;
	}
	
	/**
	 * The underlying reflected type this class represents.
	 * @return the underlying reflected type
	 */
	public Class<?> getType( ) {
		return type;
	}
	
	/**
	 * Retrieves a particular method with the given name.
	 * @param theName the name from the annotation.
	 * @return the access descriptor for the method if the name exists, null otherwise.
	 */
	public MethodAccessDescriptor getMethod( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Cannot get an auth-related method for type '%s' when the name is missing.", type.getSimpleName( ) );
		
		return accessMethods.get( theName );
	}
	
	/**
	 * All of the methods that were found to be access controlled.
	 * @return the access controlled methods
	 */
	public Collection<MethodAccessDescriptor> getMethods( ) {
		return externalAccessMethods.values( );
	}

	/**
	 * Internal method method that allows the access control manager to set the 
	 * methods on this type descriptor.
	 * @param theAccessMethods the methods to add to the type
	 */
	protected void setMethods( Collection<MethodAccessDescriptor> theAccessMethods ) {
		Preconditions.checkNotNull( theAccessMethods, "attempting to set null methods on type '%s'", type.getSimpleName( ) );
		for( MethodAccessDescriptor accessMethod : theAccessMethods ) {
			Preconditions.checkState( !accessMethods.containsKey( accessMethod.getName( ) ), "attempting to add annotated name '%s' on method '%s' to type '%s' when the name was already was added", accessMethod.getName( ), accessMethod.getMethod().getName(), type.getSimpleName( ) );
			accessMethods.put( accessMethod.getName( ), accessMethod );
		}
	}
}
