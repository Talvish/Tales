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
package com.talvish.tales.parts.reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.sites.MemberSite;

public abstract class FieldDescriptor<T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
	/**
	 * Indicates the type of the values on this field.
	 * @author jmolnar
	 *
	 */
	public enum FieldValueType {
		/**
		 * Field is an object.
		 */
		OBJECT,
		/**
		 * Field is a collection.
		 */
		COLLECTION,
		/**
		 * Field is a MAP.
		 */
		MAP
	}
	
	protected final T declaringType;
	protected final T containingType;
    protected final String name;
    protected final FieldValueType fieldValueType;
    protected final MemberSite site;
    
    protected final List<ValueType<T,F>> keyTypes;
    protected final List<ValueType<T,F>> valueTypes;
    
    /**
     * Constructor used when creating a field that holds object.
     */
    protected FieldDescriptor(
    		String theName, 
    		FieldValueType theFieldValueType, 
    		List<ValueType<T,F>> theObjectTypes, 
    		MemberSite theFieldSite,
    		T theDeclaringType, 
    		T theContainingType ) {
    	this( theName, theFieldValueType, theObjectTypes, null, theFieldSite, theDeclaringType, theContainingType );
    }

    /**
     * Constructor used when creating a field that holds maps.
     */
    protected FieldDescriptor( 
    		String theName, 
    		List<ValueType<T,F>> theKeyTypes, 
    		List<ValueType<T,F>> theValueTypes, 
    		MemberSite theFieldSite,
    		T theDeclaringType, 
    		T theContainingType ) {
    	this( theName, FieldValueType.MAP, theValueTypes, theKeyTypes, theFieldSite, theDeclaringType, theContainingType );
    }

    protected FieldDescriptor(
    		String theName, 
    		FieldValueType theFieldValueType, 
    		List<ValueType<T,F>> theValueTypes, 
    		List<ValueType<T,F>> theKeyTypes, 
    		MemberSite theFieldSite, 
    		T theDeclaringType, 
    		T theContainingType ) {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "theName must be specified" );
        Preconditions.checkNotNull( theFieldValueType, String.format( "field '%s' is missing the field value type", theName ) );
        Preconditions.checkArgument( theValueTypes != null && theValueTypes.size() > 0, String.format( "field '%s' must have at list one value type", theName ) );
        Preconditions.checkArgument( 
        		( theFieldValueType.equals( FieldValueType.OBJECT) && theKeyTypes == null ) || 
        		( theFieldValueType.equals( FieldValueType.COLLECTION) && theKeyTypes == null ) || 
        		( theFieldValueType.equals( FieldValueType.MAP ) && theKeyTypes != null && theKeyTypes.size() > 0 ), String.format( "if field '%s' is an object or collection a key type cannot be given, if a map a key type can be given", theName ) );
        Preconditions.checkNotNull( theFieldSite, String.format( "field '%s' is missing the field site", theName ) );
        Preconditions.checkNotNull( theDeclaringType, String.format( "field '%s' is missing the declaring type", theName ) );
        Preconditions.checkNotNull( theContainingType, String.format( "field '%s' is missing the containing type", theName ) );
        
        name = theName;
        fieldValueType = theFieldValueType;
        valueTypes = Collections.unmodifiableList( new ArrayList<ValueType<T,F>>( theValueTypes ) );
        if( theKeyTypes == null ) {
        	keyTypes = Collections.unmodifiableList( new ArrayList<ValueType<T,F>>( 0 ) );
        } else {
        	keyTypes = Collections.unmodifiableList( new ArrayList<ValueType<T,F>>( theKeyTypes ) );
        }
        site = theFieldSite;
        declaringType = theDeclaringType;
        containingType = theContainingType;
    }

    /**
     * The name of the field, either as declared by the
     *  attribute, or the name of the field itself.
     * @return the name of the field
     */
    public String getName( ) {
        return this.name;
    }
    
    
    /**
     * Indicates if this field is just an object, not a collection or map.
     * @return
     */
    public boolean isObject( ) {
    	return this.fieldValueType == FieldValueType.OBJECT;
    }

    /**
     * Indicates if this field is a collection or an array.
     */
    public boolean isCollection( ) {
    	return this.fieldValueType == FieldValueType.COLLECTION;
    }

    /**
     * Indicates if this field is a map.
     */
    public boolean isMap( ) {
    	return this.fieldValueType == FieldValueType.MAP;
    }
    
    /**
     * Returns the type information for the keys in the map.
     */
    public List<ValueType<T,F>> getKeyTypes( ) {
    	return this.keyTypes;
    }

    /**
     * Returns the type information for the values in the map, collection or object.
     * @return
     */
    public List<ValueType<T,F>> getValueTypes( ) {
    	return this.valueTypes;
    }
    
    /**
     * Returns the underlying Java reflected field.
     * @return The Java reflected field.
     */
    public MemberSite getSite( ) {
    	return this.site;
    }

    /**
     * Returns the type that declared this field
     * @return the type that declared this field
     */
    public T getDeclaringType( ) {
    	return declaringType;
    }
    
    /**
     * Returns the type that contains this field
     * @return the type that contains this field
     */
    public T getContainingType( ) {
    	return containingType;
    }
    
    /**
     * Gets the value from the specified object for the particular field.
     * @param theInstance the object to get the value from
     * @return the value
     */
    public Object getData( Object theInstance ) {
        return this.site.getData( theInstance );
    }

    /**
     * Sets the value on the specified object.
     * @param theInstance the instance an object to set a field value on
     * @param theValue the value to assign
     */
    public void setData( Object theInstance, Object theValue ) {
        this.site.setData( theInstance, theValue );
    }
}