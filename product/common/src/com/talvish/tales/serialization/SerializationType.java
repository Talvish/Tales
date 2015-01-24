package com.talvish.tales.serialization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.TypeDescriptor;

public class SerializationType <T extends SerializationType<T, F>, F extends SerializationField<T, F>> extends TypeDescriptor<T,F>{
	private final Method validationHook;
	private final Method deserializedHook;
	
	protected SerializationType( String theName, JavaType theType, Method theDeserializedHook, Method theValidationHook, T theBaseType ) {
    	super( theName, theType, theBaseType );
    	
    	Preconditions.checkArgument( theDeserializedHook == null || theDeserializedHook.getParameters().length == 0, "'%s.%s' cannot have method parameters.", theType.getName(), theDeserializedHook == null ? "" : theDeserializedHook.getName( ) );
    	Preconditions.checkArgument( theValidationHook == null || theValidationHook.getParameters().length == 0, "'%s.%s' cannot have method parameters.", theType.getName(), theValidationHook == null ? "" : theValidationHook.getName( ) );

    	deserializedHook = theDeserializedHook;
    	validationHook = theValidationHook;
    }
	
    /**
     * Indicates if the type has validation supports.
     * @return true if the data contract type has validation support, false otherwise
     */
    public boolean supportsValidationHook( ) {
    	return validationHook != null;
    }
    
    /**
     * Returns the method, if one was set, called when validation occurs.
     * @return the validation method or null if not set
     */
    public Method getValidationHook( ) {
    	return validationHook;
    }
    
    /**
     * Indicates if a deserialized hook is available.
     * @return true if available, false otherwise
     */
    public boolean supportsDeserializedHook( ) {
    	return deserializedHook != null;
    }
    
    /**
     * Returns the method, if one was set, called when deserialization occurs.
     * @return the deserialization method or null if not set
     */
    public Method getDeserializationHook( ) {
    	return deserializedHook;
    }

    /**
     * Calls the validation hook if available.
     * If the hook is not available a call is
     * not made but an error isn't thrown 
     * either. 
     * @param theInstance the instance to call the hook against
     */
    public void callValidationHook( Object theInstance ) {
    	if( validationHook != null ) {
    		Preconditions.checkNotNull( theInstance, "'%s' was not given an instance", this.getType().getName( ) );

	    	try {
	    		validationHook.invoke( theInstance );
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException( String.format( "Failure executing validation hook for type '%s'.", this.getType().getName() ), e );
			}
    	}
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
				throw new IllegalStateException( String.format( "Failure executing deserialized hook for type '%s'.", this.getType().getName() ), e );
			}
    	}
    }

}
