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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import com.talvish.tales.auth.jwt.JsonWebToken;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;

/**
 * This class represents a method that has access control enabled.
 * @author jmolnar
 *
 */
public class MethodAccessDescriptor {
	public static final String NAME_VALIDATOR = "tales.access_control.method_name";
	
	static {
		if( !NameManager.hasValidator( NAME_VALIDATOR ) ) {
			NameManager.setValidator( NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}
	
	private final String name;
	private final TypeAccessDescriptor typeDescriptor;
	private final Method method;
	private final List<ClaimVerifier> claimVerifiers = new ArrayList<>( );
	
	/**
	 * The constructor taking the required data.
	 * @param theName the name to give the method (used for lookup)
	 * @param theMethod the underlying reflected method this class represents
	 * @param theTypeDescriptor the type that contains this method
	 */
	public MethodAccessDescriptor( String theName, Method theMethod, TypeAccessDescriptor theTypeDescriptor ) {
		NameValidator nameValidator = NameManager.getValidator( NAME_VALIDATOR );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theMethod, "the method '%s' needs the reflected method", theName );
		Preconditions.checkNotNull( theTypeDescriptor, "the method '%s' needs the type descriptor", theName );
		
		name = theName;
		typeDescriptor = theTypeDescriptor;
		method = theMethod;
	}
	
	/**
	 * The name representing this method. 
	 * @return the name given to represent this method
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The underlying reflected type that this access control is for.
	 * @return the underlying method method this access control is for
	 */
	public Method getMethod( ) {
		return method;
	}
	
	/**
	 * The descriptor for the class that this method was found on.
	 * @return the descriptor for the class this method was found on.
	 */
	public TypeAccessDescriptor getTypeDescriptor( ) {
		return typeDescriptor;
	}
	
	/**
	 * Adds a verify to the list of verifiers that will be called
	 * when access is verified. This method is not thread-safe
	 * but is only called during the creation of the type 
	 * and should not be an issue.
	 * @param theVerifier the verifier to add
	 */
	public void addVerifier( ClaimVerifier theVerifier ) {
		// TODO: look at the threading issues
		Preconditions.checkNotNull( theVerifier, "Attempting to add a null verifier to '%s' on type '%s'", name, typeDescriptor.getType().getSimpleName( ) );
		claimVerifiers.add( theVerifier );
	}

	/**
	 * The method checks that the json web token is valid for each verifier.
	 * @param theToken the token to verify
	 * @param theAccessResult the class that the implementor writes the success, this is passed in and not returned to minimize potential performance impact 
	 * @return the status for the verify, it will not be null
	 */
	public void verifyAccess( JsonWebToken theToken, AccessResult theAccessResult ) {
		for( ClaimVerifier claimVerifier : claimVerifiers ) {
			claimVerifier.verify( theToken, theAccessResult );
			if( !AccessStatus.VERIFIED.equals( theAccessResult.getStatus( ) ) ) {
				break;
			}
		}
	}
}


