package com.talvish.tales.serialization;

import java.util.List;

import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.MemberSite;

public class SerializationField <T extends SerializationType<T, F>, F extends SerializationField<T, F>> extends FieldDescriptor<T,F> {

    /**
     * Primary constructor used to create a field that isn't a collection, array or map.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     */
	protected SerializationField( 
    		String theName, 
    		FieldDescriptor.FieldValueType theFieldValueType,
    		List<ValueType<T, F>> theObjectTypes, 
    		MemberSite theFieldSite, 
    		T theDeclaringType, 
    		T theContainingType ) {
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
	protected SerializationField( 
    		String theName, 
    		List<ValueType<T, F>> theKeyTypes, 
    		List<ValueType<T, F>> theValueTypes, 
    		MemberSite theFieldSite, 
    		T theDeclaringType, 
    		T theContainingType ) {
    	super( theName, theKeyTypes, theValueTypes, theFieldSite, theDeclaringType, theContainingType );
    }
}
