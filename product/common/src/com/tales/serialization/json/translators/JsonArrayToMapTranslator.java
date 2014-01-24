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
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;


/**
 * Translator that converts a json array containing key/value json object
 * into a Java map object.
 * @author jmolnar
 *
 */
public class JsonArrayToMapTranslator implements Translator {
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
		Preconditions.checkArgument( Map.class.isAssignableFrom( theMapType ), "needs to implement map" );
		
		keyTranslator = theKeyTranslator;
		valueTranslator = theValueTranslator;
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
				JsonElement value;
				
				for( JsonElement element : jsonArray ) {
					entry = ( JsonObject )element;
					key = entry.get( "key" );
					value = entry.get( "value" );
					if( key == null || value == null ) {
						throw new TranslationException( "Could not find the key or the value properties to create a proper map." );
					} else {
						map.put( keyTranslator.translate( key ), valueTranslator.translate( value ) );
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
