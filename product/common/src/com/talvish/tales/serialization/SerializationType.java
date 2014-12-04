package com.talvish.tales.serialization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.TypeDescriptor;

public class SerializationType <T extends SerializationType<T, F>, F extends SerializationField<T, F>> extends TypeDescriptor<T,F>{
	private final boolean supportsValidation;
	private final Method deserializedHook;
	
	protected SerializationType( String theName, JavaType theType, Method theDeserializedHook, boolean validationSupport ) {
    	super( theName, theType );
    	
    	Preconditions.checkArgument( theDeserializedHook == null || theDeserializedHook.getParameters().length == 0, "'%s.%s' cannot have method parameters.", theType.getName(), theDeserializedHook == null ? "" : theDeserializedHook.getName( ) );

    	deserializedHook = theDeserializedHook;
    	supportsValidation = validationSupport;
    }
	
    /**
     * Indicates if the type has validation supports.
     * @return true if the data contract type has validation support, false otherwise
     */
    public boolean supportsValidation( ) {
    	// TODO: this isn't suppoted yet
    	return this.supportsValidation;
    }
    
    /**
     * Indicates if a deserialized hook is available.
     * @return true if available, false otherwise
     */
    public boolean supportsDeserializedHook( ) {
    	return deserializedHook != null;
    }

    /**
     * Calls the deserialized hook if available.
     * If the hook is not available a call is
     * not made but an error isn't thrown 
     * either. 
     * @param theInstance the instance to call the hook against
     */
    public void callDeserializedHook( Object theInstance ) {
    	if( deserializedHook != null ) {
    		Preconditions.checkNotNull( theInstance, "'%s' was not given an instance", this.getType().getName( ) );

	    	try {
				deserializedHook.invoke( theInstance );
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException( String.format( "'%s' was unable to execute the deserialized hook", this.getType().getName() ), e );
			}
    	}
    }

}
