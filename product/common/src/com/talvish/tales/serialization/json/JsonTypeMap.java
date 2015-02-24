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
package com.talvish.tales.serialization.json;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.serialization.SerializationType;

/**
 * Represents a type and its representation as a json object.
 * @author jmolnar
 *
 */
public class JsonTypeMap {
    private static final Logger logger = LoggerFactory.getLogger( JsonTypeMap.class ); // log against the id, so we can group up from anywhere
	private final boolean strictMatch = false;
	private final SerializationType<?,?> reflectedType;
	private Map<String, JsonMemberMap> members = Collections.unmodifiableMap( new HashMap<String, JsonMemberMap>( ) );
	
	/**
	 * Constructor taking the type it represents.
	 * @param theContractType the type 
	 */
	public JsonTypeMap( SerializationType<?,?> theReflectedType ) {
		Preconditions.checkNotNull( theReflectedType, "need a reflected type type" );
		reflectedType = theReflectedType;
	}
	
	/**
	 * The type the map represents.
	 * @return
	 */
	public SerializationType<?,?> getReflectedType( ) {
		return reflectedType;
	}

	/**
	 * A helper method that, given an instance of this type of object, 
	 * will return a json version.
	 * @param theInstance the instance of the type to get a json version of
	 * @return the json object representing the type
	 */
	public JsonObject getData( Object theInstance ) {
		Preconditions.checkNotNull( theInstance, "need a non-null instance");
		JsonObject jsonObject = new JsonObject( );
		
		for( JsonMemberMap member : members.values() ) {
			jsonObject.add( member.getReflectedField().getName(), member.getData( theInstance ) );
		}
		return jsonObject;
	}
	
	/**
	 * A helper method that, given an instance and json data, will
	 * set data on the instance based on the json data.
	 * @param theInstance the instance to set
	 * @param theElement the data to place on the instance
	 */
	public void setData( Object theInstance, JsonObject theElement ) {
		Preconditions.checkNotNull( theInstance, "need a non-null instance");
		Preconditions.checkNotNull( theElement, "need a non-null element");
		
		JsonMemberMap member = null;
		Set<Entry<String,JsonElement>> set = theElement.entrySet();
		
		for( Entry<String, JsonElement> entry : set ) {
			member = members.get( entry.getKey() );
			if( member == null ) {
				if( strictMatch ) {
					throw new TranslationException( String.format( "Json for type '%s' refers to a member '%s' that does not exist.", reflectedType.getType().getName(), entry.getKey() ) );
				} else {
					logger.debug( "Ignoring unknown member '{}.{}'", this.getReflectedType().getName(), entry.getKey() );
				}
			} else {
				try {
					member.setData( theInstance, entry.getValue( ) );
				} catch( TranslationException e ) {
					throw new TranslationException( String.format( "Error attempting to set data on member '%s.%s'.", this.reflectedType.getName(), member.getReflectedField().getName( ) ), e );
				}
			}
		}
		// call the deserialized hook (this method verifies the hook is there)
		reflectedType.callDeserializedHook( theInstance );
		// after deserialization, we call the validation hook
		reflectedType.callValidationHook( theInstance );
	}

	/**
	 * Get a member based on the member name.
	 * @param theMemberName the name of member to get
	 * @return the member map
	 */
	public JsonMemberMap getMember( String theMemberName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theMemberName ), "need a member name to get a member" );
		return members.get( theMemberName );
	}
	
	/**
	 * Returns the members that make up this type.
	 * @return the members of the type
	 */
	public Collection<JsonMemberMap> getMembers( ) {
		return members.values( );
	}
	
    /**
     * Sets the members on this object. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theMembers the members to use
     */
    void setMembers( Collection<JsonMemberMap> theMembers ) {
    	Preconditions.checkNotNull( theMembers, "need members" );
    	Preconditions.checkState( members.size() == 0, "members are already set" );

    	HashMap<String,JsonMemberMap> newMembers = new HashMap<String, JsonMemberMap>( theMembers.size() );
    	
    	for( JsonMemberMap member : theMembers ) {
    		if( newMembers.containsKey( member.getReflectedField().getName( ) ) ) {
    			throw new IllegalStateException( String.format( "The type with name '%s' and type '%s' is attempting to add more than one member called '%s'.", this.reflectedType.getName(), this.reflectedType.getType().getName(), member.getReflectedField().getName( ) ) );
    		} else if( member.getContainingType() != this ) {
    			throw new IllegalStateException( String.format( "The type with name '%s' and type '%s' is attempting to add a member called '%s', but the member is associated to the type '%s'.", this.reflectedType.getName(), this.reflectedType.getType().getName(), member.getReflectedField().getName( ), member.getContainingType().getReflectedType().getType( ).getName( ) ) );
    		} else {
    			newMembers.put( member.getReflectedField( ).getName( ), member );
    		}
    	}
    	
    	members = Collections.unmodifiableMap( newMembers );
    }
}
