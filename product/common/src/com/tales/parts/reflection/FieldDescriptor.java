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
package com.tales.parts.reflection;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.sites.MemberSite;

public abstract class FieldDescriptor<T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
	protected final T declaringType;
	protected final T containingType;
    protected final String name;
    protected final MemberSite site;
    
    protected final ValueType<T,F> fieldType; // we always have this
    
    protected final ValueType<T,F> elementType; // we have this if we are a collection

    protected final ValueType<T,F> keyType; // we have these if we are a map
    protected final ValueType<T,F> valueType;
    
    protected FieldDescriptor(
    		String theName, 
    		ValueType<T,F> theFieldType, 
    		MemberSite theFieldSite, T 
    		theDeclaringType, 
    		T theContainingType ) {
    	this( theName, theFieldType, null, null, null, theFieldSite, theDeclaringType, theContainingType );
    }

    protected FieldDescriptor(
    		String theName, 
    		ValueType<T,F> theFieldType, 
    		ValueType<T,F> theElementType, 
    		MemberSite theFieldSite, T 
    		theDeclaringType, 
    		T theContainingType ) {
    	this( theName, theFieldType, theElementType, null, null, theFieldSite, theDeclaringType, theContainingType );
    }

    protected FieldDescriptor( 
    		String theName, 
    		ValueType<T,F> theFieldType, 
    		ValueType<T,F> theKeyType, 
    		ValueType<T,F> theValueType, 
    		MemberSite theFieldSite, T 
    		theDeclaringType, 
    		T theContainingType ) {
    	this( theName, theFieldType, null, theKeyType, theValueType, theFieldSite, theDeclaringType, theContainingType );
    }

    protected FieldDescriptor( String theName, ValueType<T,F> theFieldType, ValueType<T,F> theElementType, ValueType<T,F> theKeyType, ValueType<T,F> theValueType, MemberSite theFieldSite, T theDeclaringType, T theContainingType ) {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "theName must be specified" );
        Preconditions.checkNotNull( theFieldType, String.format( "field '%s' is missing the field type", theName ) );
        Preconditions.checkArgument( !( theElementType != null && theKeyType != null ), String.format( "field '%s' must not have both an element and a key type", theName ) );
        Preconditions.checkArgument( ( theKeyType == null && theValueType == null) || ( theKeyType != null && theValueType != null), String.format( "field '%s' either needs both a key and value type, or neitheris missing the value type", theName ) );
        Preconditions.checkNotNull( theFieldSite, String.format( "field '%s' is missing the field site", theName ) );
        Preconditions.checkNotNull( theDeclaringType, String.format( "field '%s' is missing the declaring type", theName ) );
        Preconditions.checkNotNull( theContainingType, String.format( "field '%s' is missing the containing type", theName ) );
        
        name = theName;
        fieldType = theFieldType;
        site = theFieldSite;
        declaringType = theDeclaringType;
        containingType = theContainingType;
        elementType = theElementType;
        keyType = theKeyType;
        valueType = theValueType;
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
     * Returns the type information for this field.
     */
    public ValueType<T,F> getFieldType( ) {
    	return this.fieldType;
    }

    /**
     * Indicates if this field is a collection or an array.
     */
    public boolean isCollection( ) {
    	return this.elementType == null ? false : true;
    }

    /**
     * Returns the type information for the element in the collection.
     */
    public ValueType<T,F> getElementType( ) {
    	return this.elementType;
    }

    /**
     * Indicates if this field is a map.
     */
    public boolean isMap( ) {
    	return this.keyType == null ? false : true;
    }
    
    /**
     * Returns the type information for the key in the map.
     */
    public ValueType<T,F> getKeyType( ) {
    	return this.keyType;
    }

    /**
     * Returns the type information for the value in the map.
     * @return
     */
    public ValueType<T,F> getValueType( ) {
    	return this.valueType;
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