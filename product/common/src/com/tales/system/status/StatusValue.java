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
package com.tales.system.status;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.services.NameManager;

/**
 * This class represents a status value used
 * to aid in the monitoring and health of
 * a contract, engine or service.
 * @author jmolnar
 *
 */
public class StatusValue {
	//private final enum valuetype // value, rate, average,
	private final String name;
	private final String description;
	private final Object source;
	private final Method method;
	private final Class<?> type;
	
	/**
	 * The constructor taking the required elements.
	 * @param theName the name to use for status value
	 * @param theSource the source of the value
	 * @param theMethod the method use to extract the status value
	 */
	public StatusValue( String theName, String theDescription, Object theSource, Method theMethod ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Status value has to have a name" );
		Preconditions.checkArgument( NameManager.getStatusValueNameValidator().isValid( theName ), String.format( "Status value name '%s' does not conform to validator '%s'.", theName, NameManager.getStatusValueNameValidator().getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theSource, "Need the object that contains the status value." );
		Preconditions.checkNotNull( theMethod, "Need the method to get the status value from the source." );
		
		name = theName;
		description = theDescription;
		source = theSource;
		method = theMethod;
		type = method.getReturnType();
	}
	
	/**
	 * Returns the name given to the status value.
	 * @return the name of the status value
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * Returns the description of the status value.
	 * @return the description of the status value
	 */
	public String getDescription( ) {
		return description;
	}
	
	/**
	 * Returns the class that represents the type of the status value.
	 * @return the type
	 */
	public Class<?> getType( ) {
		return type;
	}
	
	/**
	 * Returns the source object used by the status value.
	 * @return the source object.
	 */
	public Object getSource( ) {
		return this.source;
	}

	/**
	 * Gets the status value.
	 * @return the status value
	 */
	public Object getValue( ) {
		try {
			return method.invoke( source, ( Object[] )null );
		} catch (IllegalArgumentException e) {
			throw new StatusException( String.format( "Unable to get value for status '%s' on type '%s'", name, source.getClass().getName() ), e );
		} catch (IllegalAccessException e) {
			throw new StatusException( String.format( "Unable to get value for status '%s' on type '%s'", name, source.getClass().getName() ), e );
		} catch (InvocationTargetException e) {
			throw new StatusException( String.format( "Unable to get value for status '%s' on type '%s'", name, source.getClass().getName() ), e );
		}
	}
}