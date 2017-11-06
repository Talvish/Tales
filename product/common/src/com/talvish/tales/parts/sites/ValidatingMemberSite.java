// ***************************************************************************
// *  Copyright 2017 Joseph Molnar
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
package com.talvish.tales.parts.sites;

import java.lang.annotation.Annotation;

import com.talvish.tales.parts.ValidationException;
import com.talvish.tales.parts.constraints.ValueValidator;
import com.talvish.tales.parts.reflection.JavaType;

/**
 * {@link MemberSite} extension that validates items during the setter calls.
 * @author jmolnar
 *
 */
public class ValidatingMemberSite implements MemberSite {
	private final MemberSite memberSite;
	private final ValueValidator[] validators;
	
	public ValidatingMemberSite( MemberSite theMemberSite, ValueValidator[] theValidators ) {
		// TODO: param validation
		// TODO: copy the validators
		memberSite = theMemberSite;
		validators = theValidators;
		
	}
	
	/**
	 * The class that contains the member this site targets
	 * @return the class containing the member
	 */
	public Class<?> getContainingType( ) {
		return memberSite.getContainingType( );
	}
	
	/**
	 * The name of the member this site targets.
	 * @return the name of the member
	 */
	public String getName( ) {
		return memberSite.getName( );
	}

	/**
     * Returns the annotation on the member for the specified type.
     * @param theAnnotationClass the class of the annotation to get
     * @return the annotation or null if it doesn't exist
     */
    public <A extends Annotation> A getAnnotation( Class<A> theAnnotationClass ) {
    	return memberSite.getAnnotation(theAnnotationClass);
    }
    
    /**
     * Returns all annotation on the member.
     * @return the annotations, which will be a zero length array if none
     */
    public Annotation[] getAnnotations( ) {
    	return memberSite.getAnnotations();
    }

	/**
	 * The type of the data in this member.
	 * @return class representing the type of data
	 */
	public JavaType getType( ) {
		return memberSite.getType();
	}
	
	/**
	 * Indicates if this particular member should be treated read-only.
	 * @return
	 */
	public boolean isReadOnly( ) {
		return memberSite.isReadOnly();
	}

	/**
	 * Gets data from a member off the source object.
	 * @param theSource the source object to get data from.
	 * @return the value from the field off the source object
	 */
	public Object getData( Object theSource ) {
		return memberSite.getData(theSource);
	}
	/**
	 * Sets data on a member of a sink object
	 * @param theSink the object to set a value on
	 * @param theValue the value to set the member on the sink object
	 */
	public void setData( Object theSink, Object theValue ) {
		// TODO: optimize this, check for null first, or size 0 or something
		for( ValueValidator validator : validators ) {
			if( !validator.isValid( theValue ) ) {
				throw new ValidationException( 
						String.format( 
								"Member '%s.%s' could not be set due to failing validator '%s'", 
								memberSite.getContainingType().getSimpleName(), 
								memberSite.getName( ), 
								validator.getClass().getSimpleName( ) ) );
			}
		}

		memberSite.setData(theSink, theValue);
	}

}
