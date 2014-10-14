// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.tales.parts.sites;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.common.base.Preconditions;
import com.tales.parts.reflection.JavaType;
import com.tales.parts.reflection.TypeUtility;

/**
 * This is a simple {@link DataSite} that abstracts getting 
 * and setting a field value from a particular type 
 * @author jmolnar
 *
 */
public class FieldSite implements MemberSite {
	private final Field field;
	private final JavaType type;
	
	/**
	 * Constructs the data site based on the {@code Field} reflection instance. 
	 * @param theField the field to create a site for
	 */
	public FieldSite( Field theField ) {
		Preconditions.checkNotNull( theField, "need a field object" );
		
		field = theField;
		// make sure, if private, we can access it properly
		field.setAccessible( true );
		type = new JavaType( theField.getGenericType() );
	}
	
	/**
	 * Constructs the data site based on the {@code Field} reflection instance and a
	 * parent with more detail than the Field.getDeclaringClass() gives. 
	 * @param theDeclaringType the class the declared the field
	 * @param theField the field to create a site for
	 */
	public FieldSite( Type theDeclaringType, Field theField ) {
		Preconditions.checkNotNull( theField, "need a field object" );
		Preconditions.checkArgument( 
				theField.getDeclaringClass().equals( TypeUtility.extractClass( theDeclaringType ) ), 
				"Type '%s' is being used as declaring class for field '%s.%s'.", 
				theDeclaringType.getTypeName(), 
				theField.getDeclaringClass().getName(), 
				theField.getName() );
		
		field = theField;
		// make sure, if private, we can access it properly
		field.setAccessible( true );
		type = new JavaType( TypeUtility.determineFieldType( theDeclaringType, theField ) );
	}
	
	/**
	 * The class that contains the field this site targets
	 * @return the class containing the field
	 */
	public Class<?> getContainingType( ) {
		return this.field.getDeclaringClass();
	}
	
	/**
	 * The type of the data in this field.
	 * @return class representing the type of data
	 */
	public JavaType getType( ) {
		return this.type;
	}
	
	/**
	 * The name of the field this site targets.
	 * @return the name of the field
	 */
	public String getName( ) {
		return field.getName();
	}
	
	/**
	 * The underlying reflected {@code Field} object.
	 * @return the field object
	 */
	public Field getField( ) {
		return field;
	}
	
	/**
	 * Indicates if this particular field should be treated read-only.
	 * @return
	 */
	public boolean isReadOnly( ) {
		return false;
	}
	/**
	 * Gets data from a field off the source object.
	 * @param theSource the source object to get data from.
	 * @return the value from the field off the source object
	 */
	public Object getData( Object theSource ) {
		try {
			return this.field.get( theSource );
		} catch (IllegalArgumentException e) {
			throw new DataSiteException( String.format( "Could not get data from %s.%s.", field.getDeclaringClass().getName(), field.getName() ), e );
		} catch (IllegalAccessException e) {
			throw new DataSiteException( String.format( "Could not get data from %s.%s.", field.getDeclaringClass().getName(), field.getName() ), e );
		}
	}
	/**
	 * Sets data on a field of a sink object
	 * @param theSink the object to set a value on
	 * @param theValue the value to set the field on the sink object
	 */
	public void setData( Object theSink, Object theValue ) {
		try {
			this.field.set( theSink, theValue );
		} catch (IllegalArgumentException e) {
			throw new DataSiteException( String.format( "Could not set data on %s.%s.", field.getDeclaringClass().getName(), field.getName() ), e );
		} catch (IllegalAccessException e) {
			throw new DataSiteException( String.format( "Could not set data on %s.%s.", field.getDeclaringClass().getName(), field.getName() ), e );
		}
	}
}
