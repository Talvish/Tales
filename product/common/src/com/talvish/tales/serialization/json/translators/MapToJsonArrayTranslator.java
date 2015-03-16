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
package com.talvish.tales.serialization.json.translators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.TypeFormatAdapter;


/**
 * Translator that converts a map into a json array of key/value json objects.,
 * or, if null, {@code JsonNull}. 
 * If the keys or values support more than one type for translation, 
 * it isn't possible (and may be very difficult or impossible to do in Java) 
 * to have key or value types differ by their type parameters (if generic types) 
 * since the information is not available during the serialization process.
 * @author jmolnar
 *
 */
public class MapToJsonArrayTranslator implements Translator {
	private final Map<Class<?>, TypeFormatAdapter> keyTypeAdapters = new HashMap<>( 2 );
	private final Map<Class<?>, TypeFormatAdapter> valueTypeAdapters = new HashMap<>( 2 );
	
	private final Translator keyTranslator;
	private final Translator valueTranslator;
	
	/**
	 * Constructor taking just the translators needed for the map.
	 * This constructor is used when only one type is possible for
	 * each of the key and value types.
	 */
	public MapToJsonArrayTranslator( Translator theKeyTranslator, Translator theValueTranslator ) {
		Preconditions.checkNotNull( theKeyTranslator );
		Preconditions.checkNotNull( theValueTranslator );
		
		keyTranslator = theKeyTranslator;
		valueTranslator = theValueTranslator;
	}
	
	/**
	 * Constructor taking the list of supported key and value type adapters.
	 */
	public MapToJsonArrayTranslator( List<TypeFormatAdapter> theKeyTypeAdapters, List<TypeFormatAdapter> theValueTypeAdapters ) {
		Preconditions.checkNotNull( theKeyTypeAdapters );
		Preconditions.checkArgument( theKeyTypeAdapters.size( ) > 0, "Need at least one key type adapter." );
		Preconditions.checkNotNull( theValueTypeAdapters );
		Preconditions.checkArgument( theValueTypeAdapters.size( ) > 0, "Need at least one value type adapter." );

		for( TypeFormatAdapter keyTypeAdapter : theKeyTypeAdapters ) {
			Preconditions.checkArgument( !valueTypeAdapters.containsKey( keyTypeAdapter.getType().getUnderlyingClass()), String.format( "Attempting to add key type adapter '%s' more than once (differences in generic type parameters are not sufficient).", keyTypeAdapter.getType( ).getUnderlyingClass().getName()));
			keyTypeAdapters.put( keyTypeAdapter.getType( ).getUnderlyingClass(), keyTypeAdapter );
		}
		// if we only have one key type than pull out the translator directly 
		// since it will speed things up at runtime during translation
		if( theKeyTypeAdapters.size() == 1 ) {
			keyTranslator = theKeyTypeAdapters.get( 0 ).getToFormatTranslator();
		} else {
			keyTranslator = null;
		}
		
		for( TypeFormatAdapter valueTypeAdapter : theValueTypeAdapters ) {
			Preconditions.checkArgument( !valueTypeAdapters.containsKey( valueTypeAdapter.getType().getUnderlyingClass()), String.format( "Attempting to add value type adapter '%s' more than once (differences in generic type parameters are not sufficient).", valueTypeAdapter.getType( ).getUnderlyingClass().getName()));
			valueTypeAdapters.put( valueTypeAdapter.getType( ).getUnderlyingClass(), valueTypeAdapter );
		}
		// if we only have one key type than pull out the translator directly 
		// since it will speed things up at runtime during translation
		if( theValueTypeAdapters.size() == 1 ) {
			valueTranslator = theValueTypeAdapters.get( 0 ).getToFormatTranslator();
		} else {
			valueTranslator = null;
		}
	}

	/**
	 * Translates the received object into a json array with a key/value objects.
	 * If the object is of the wrong type or translator doesn't return JsonElements, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = JsonNull.INSTANCE;
		} else {
			try {
				Map<?,?> map = ( Map<?,?> )anObject;
				JsonArray jsonArray = new JsonArray( );
				JsonObject jsonEntry;
				TypeFormatAdapter typeAdapter;
				
				for( Entry<?, ?> entry : map.entrySet() ) {
					jsonEntry = new JsonObject();
					
					// translate the key side
					if( keyTranslator == null ) { // meaning we have more than one so didn't pull out the only translator
						// we need to find the translator to use
						// then save out the key type
						// and the key value
						typeAdapter = keyTypeAdapters.get( entry.getKey().getClass( ) );
						if( typeAdapter == null ) {
							throw new TranslationException( String.format( "An object of type '%s' was attempting to be converted to a json object as a key in a map, but this object isn't supported", entry.getKey().getClass( ).getName( ) ));
						} else {
							jsonEntry.addProperty( "key_type", typeAdapter.getName() );
							jsonEntry.add( "key", ( JsonElement )typeAdapter.getToFormatTranslator().translate( entry.getKey( ) ) );
						}
					} else {
						jsonEntry.add( "key", ( JsonElement )keyTranslator.translate( entry.getKey() ) );
					}
					
					// translate the value side
					if( valueTranslator == null ) { // meaning we have more than one so didn't pull out the only translator
						// we need to find the translator to use
						// then save out the key type
						// and the key value
						typeAdapter = valueTypeAdapters.get( entry.getValue().getClass( ) );
						if( typeAdapter == null ) {
							throw new TranslationException( String.format( "An object of type '%s' was attempting to be converted to a json object as a value in a map, but this object isn't supported", entry.getValue().getClass( ).getName( ) ));
						} else {
							jsonEntry.addProperty( "value_type", typeAdapter.getName() );
							jsonEntry.add( "value", ( JsonElement )typeAdapter.getToFormatTranslator().translate( entry.getValue( ) ) );
						}
					} else {
						jsonEntry.add( "value", ( JsonElement )valueTranslator.translate( entry.getValue() ) );
					}
					jsonArray.add( jsonEntry );
				}
				returnValue = jsonArray;

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
