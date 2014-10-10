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
package com.tales.serialization.json.translators;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.serialization.json.JsonTypeReference;


/**
 * Translator that converts a json array containing key/value json object
 * into a Java map object.
 * @author jmolnar
 *
 */
public class JsonArrayToMapTranslator implements Translator {
	// these are used if we have more than one possible element type
	private final Map<String, JsonTypeReference> keyTypeReferences = new HashMap<>( 2 ); 
	private final Map<String, JsonTypeReference> valueTypeReferences = new HashMap<>( 2 );
	
	// these are used if we only have one possible element type
	private final Translator keyTranslator; 
	private final Translator valueTranslator;
	
	private final Class<?> mapType;
	private final Constructor<?> constructor;
	
	/**
	 * Constructor taking the needed translator.
	 */
	public JsonArrayToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator, Class<?> theMapType ) {
		Preconditions.checkNotNull( theKeyTranslator, "need a key translator" );
		Preconditions.checkNotNull( theValueTranslator, "need a value translator" );
		Preconditions.checkNotNull( theMapType, "need a map type" );
		Preconditions.checkArgument( Map.class.isAssignableFrom( theMapType ), String.format( "'%s' needs to implement map.", theMapType.getName( ) ) );
		
		keyTranslator = theKeyTranslator;
		valueTranslator = theValueTranslator;
		
		// now deal with the type for the map, need to get a constructor
		if( Modifier.isAbstract( theMapType.getModifiers( ) ) || theMapType.isInterface( ) ) {
			if( theMapType.isAssignableFrom( HashMap.class ) ) {
				mapType = HashMap.class; // a standard map
			} else if( theMapType.isAssignableFrom( TreeMap.class ) ) {
				mapType = TreeMap.class; // a sorted map
			} else {
				throw new IllegalArgumentException( String.format( "unclear how to use the map of type '%s'", theMapType.getName() ) );
			}
		} else {
			mapType = theMapType;
		}
		try {
			constructor = mapType.getDeclaredConstructor( );
		} catch (SecurityException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for map of type '%s'", theMapType.getName() ), e );
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for map of type '%s'", theMapType.getName() ), e );
		}
	}
	
	/**
	 * Constructor taking the list of supported key and value types.
	 */
	public JsonArrayToMapTranslator( List<JsonTypeReference> theKeyTypeReferences, List<JsonTypeReference> theValueTypeReferences, Class<?> theMapType ) {
		Preconditions.checkNotNull( theKeyTypeReferences );
		Preconditions.checkArgument( theKeyTypeReferences.size( ) > 0, "Need at least one key type reference." );
		Preconditions.checkNotNull( theValueTypeReferences );
		Preconditions.checkArgument( theValueTypeReferences.size( ) > 0, "Need at least one value type reference." );
		Preconditions.checkNotNull( theMapType, "need a map type" );
		Preconditions.checkArgument( Map.class.isAssignableFrom( theMapType ), String.format( "'%s' needs to implement map.", theMapType.getName( ) ) );

		for( JsonTypeReference keyTypeReference : theKeyTypeReferences ) {
			Preconditions.checkArgument( !valueTypeReferences.containsKey( keyTypeReference.getName()), String.format( "Attempting to add key type reference '%s' more than once.", keyTypeReference.getType( ).getName()));
			keyTypeReferences.put( keyTypeReference.getName(), keyTypeReference );
		}
		// if we only have one key type than pull out the translator directly 
		// since it will speed things up at runtime during translation
		if( theKeyTypeReferences.size() == 1 ) {
			keyTranslator = theKeyTypeReferences.get( 0 ).getToJsonTranslator();
		} else {
			keyTranslator = null;
		}
		
		for( JsonTypeReference valueTypeReference : theValueTypeReferences ) {
			Preconditions.checkArgument( !valueTypeReferences.containsKey( valueTypeReference.getName()), String.format( "Attempting to add value type reference '%s' more than once.", valueTypeReference.getType( ).getName()));
			valueTypeReferences.put( valueTypeReference.getName( ), valueTypeReference );
		}
		// if we only have one key type than pull out the translator directly 
		// since it will speed things up at runtime during translation
		if( theValueTypeReferences.size() == 1 ) {
			valueTranslator = theValueTypeReferences.get( 0 ).getToJsonTranslator();
		} else {
			valueTranslator = null;
		}
		
		// now deal with the type for the map, need to get a constructor
		if( Modifier.isAbstract( theMapType.getModifiers( ) ) || theMapType.isInterface( ) ) {
			if( theMapType.isAssignableFrom( HashMap.class ) ) {
				mapType = HashMap.class; // a standard map
			} else if( theMapType.isAssignableFrom( TreeMap.class ) ) {
				mapType = TreeMap.class; // a sorted map
			} else {
				throw new IllegalArgumentException( String.format( "unclear how to use the map of type '%s'", theMapType.getName() ) );
			}
		} else {
			mapType = theMapType;
		}
		try {
			constructor = mapType.getDeclaredConstructor( );
		} catch (SecurityException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for map of type '%s'", theMapType.getName() ), e );
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for map of type '%s'", theMapType.getName() ), e );
		}
	}

	/**
	 * Translates the received object into a map;
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null || anObject.equals( JsonNull.INSTANCE ) ) {
			returnValue = null;
		} else {
			try {
				// things to look at later
				JsonArray jsonArray = ( JsonArray )anObject;
				@SuppressWarnings("unchecked")
				Map<Object, Object> map = ( Map<Object,Object> )constructor.newInstance();
				JsonObject entry;
				JsonElement key;
				JsonElement keyType;
				JsonElement value;
				JsonElement valueType;
				JsonTypeReference typeReference;
				
				Translator selectedKeyTranslator;
				Translator selectedValueTranslator;

				for( JsonElement element : jsonArray ) {
					entry = ( JsonObject )element;
					key = entry.get( "key" );
					keyType = entry.get( "key_type" );
					value = entry.get( "value" );
					valueType = entry.get( "value_type" );
					
					selectedKeyTranslator = keyTranslator; // set a default, though it could be null
					selectedValueTranslator = valueTranslator; // set a default, though it could be null
					
					if( key == null ) {
						throw new TranslationException( "Could not find the key to create a proper map." );
					} else if( value == null ) {
						throw new TranslationException( "Could not find the value to create a proper map." );
					} else {
						if( keyType != null ) {
							String keyTypeString = keyType.getAsString();
							typeReference = keyTypeReferences.get( keyTypeString );
							
							if( typeReference == null ) {
								throw new TranslationException( String.format( "Json is referring to a key type '%s' that isn't supported.", keyTypeString ) );
							} else {
								selectedKeyTranslator = typeReference.getFromJsonTranslator();
							}
						} 
						if( valueType != null ) {
							String valueTypeString = valueType.getAsString();
							typeReference = valueTypeReferences.get( valueTypeString );
							
							if( typeReference == null ) {
								throw new TranslationException( String.format( "Json is referring to a value type '%s' that isn't supported.", valueTypeString ) );
							} else {
								selectedValueTranslator = typeReference.getFromJsonTranslator();
							}
						}
						if( selectedKeyTranslator == null ) {
							throw new TranslationException( "An appropriate key type was not provided." );
						}
						if( selectedValueTranslator == null ) {
							throw new TranslationException( "An appropriate value type was not provided." );
						}
						map.put( selectedKeyTranslator.translate( key ), selectedValueTranslator.translate( value ) );
					}
				}
				
				returnValue = map;

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			} catch( NullPointerException e ) {
				throw new TranslationException( String.format( "Unable to use null in the map of type '%s'", mapType.getName() ), e );
			} catch (InstantiationException e) {
				throw new TranslationException( String.format( "Unable to create a map of type '%s'", mapType.getName() ), e );
			} catch (IllegalArgumentException e) {
				throw new TranslationException( String.format( "Unable to create a map of type '%s'", mapType.getName() ), e );
			} catch (InvocationTargetException e) {
				throw new TranslationException( String.format( "Unable to create a map of type '%s'", mapType.getName() ), e );
			} catch (IllegalAccessException e) {
				throw new TranslationException( String.format( "Unable to create a map of type '%s'", mapType.getName() ), e );
			}
		}
		return returnValue;	
	}
}
