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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public abstract class TypeDescriptor<T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
    protected final String name;
    protected final Class<?> type;
    private final Constructor<?> defaultConstructor;
    
    protected Map<String,F> fields = Collections.unmodifiableMap( new HashMap<String, F>( 0 ) );
    
    public TypeDescriptor( String theName, Class<?> theType ) {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "theName must be specified" );
        Preconditions.checkNotNull( theType, "the type must not be null" );
        
        name = theName;
        type = theType;
        
        try {
			defaultConstructor = type.getDeclaredConstructor( );
			defaultConstructor.setAccessible( true );
		} catch (SecurityException e) {
			throw new IllegalArgumentException( String.format( "Type '%s' does not have an accessible constructor.", theType.getName( ) ) );
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( String.format( "Type '%s' does not have an accessible constructor.", theType.getName( ) ) );
		}
    }

    /**
     * The name given to the type, which may not be the actual class name.
     * @return the name, which may not be the class name
     */
    public String getName( ) {
        return this.name;
    }
    
    /**
     * The underlying type this class represents.
     * @return the underlying class
     */
    public Class<?> getType( ) {
    	return this.type;
    }
    
    /**
     * Gets the field info for the specified field name.
     * @param theName the name of the field to get
     * @return the field or null if that field could not be found
     */
    public F getField( String theName ) {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "theName" );
        return fields.get( theName );
    }

    public boolean containsField( String theFieldName ) {
    	return fields.containsKey( theFieldName );
    }
    
    public Collection<F> getFields( ) {
    	return fields.values();
    }
    
    public void setFields( Collection<F> theFields ) {
    	Preconditions.checkNotNull( theFields, "need fields" );
    	Preconditions.checkState( fields.size() == 0, "fields are already set" );
    	
    	HashMap<String,F> newFields = new HashMap<String, F>( theFields.size() );
    	
    	for( F field : theFields ) {
    		if( newFields.containsKey( field.getName( ) ) ) {
    			throw new IllegalStateException( String.format( "The type info with name '%s' and type '%s' is attempting to add more than one fieled called '%s'.", this.name, this.type.getName(), field.getName( ) ) );
    		} else if( field.getContainingType() != this ) {
    			throw new IllegalStateException( String.format( "The type info with name '%s' and type '%s' is attempting to add a field called '%s', but the field is associated to the type '%s'.", this.name, this.type.getName(), field.getName( ), field.getContainingType().getType().getName() ) );
    		} else {
    			newFields.put( field.getName( ), field );
    		}
    	}
    	
    	setFields( newFields );
    }
    
    protected void setFields( HashMap<String, F> theFields ) {
    	fields = Collections.unmodifiableMap( theFields );
    }

    /**
     * Creates a new instance of the class.
     * @return
     */
    public Object newInstance( ) {
        try {
			return defaultConstructor.newInstance( );
        } catch( IllegalArgumentException e ) {
            throw new IllegalStateException( String.format( "Cannot create a new instance of class '%s' due to an illegal argument exception.", type.getName( ) ), e );
        } catch( InvocationTargetException e ) {
            throw new IllegalStateException( String.format( "Cannot create a new instance of class '%s' due to an invocation exception.", type.getName( ) ), e );
        } catch( InstantiationException e ) {
            throw new IllegalStateException( String.format( "Cannot create a new instance of class '%s' due to an instantiation exception.", type.getName( ) ), e );
        } catch( IllegalAccessException e ) {
            throw new IllegalStateException( String.format( "Cannot create a new instance of class '%s' due to an illegal access exception.", type.getName( ) ), e );
        }
    }
}