// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.serialization.json.JsonTypeReference;

public class JsonObjectToPolymorphicObjectTranslator implements Translator {
	private Map<String, JsonTypeReference> typeReferences = new HashMap<>( 2 );

	/**
	 * Constructor taking the needed references.
	 */
	public JsonObjectToPolymorphicObjectTranslator( List<JsonTypeReference> theTypeReferences ) {
		Preconditions.checkNotNull( theTypeReferences );

		for( JsonTypeReference typeReference : theTypeReferences ) {
			Preconditions.checkArgument( !typeReferences.containsKey( typeReference.getType()), String.format( "Attempting to add type reference '%s' more than once.", typeReference.getType( ).getName()));
			typeReferences.put( typeReference.getName( ), typeReference );
		}
	}

	/**
	 * Translates the received object into the appropriate json representation.
	 * If the object is of the wrong type or the translator used doesn't produce
	 * JsonElements's then a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null || anObject.equals( JsonNull.INSTANCE ) ) {
			returnValue = null;
		} else {
			try {
				// need to extract two things, the "value" and the "value_type"
				// and if they don't exist then we have a problem
				JsonObject jsonObject = ( JsonObject )anObject;
				JsonPrimitive valueTypeJson = jsonObject.getAsJsonPrimitive( "value_type" );
				JsonElement valueJson = jsonObject.get( "value" );
				
				if( valueTypeJson == null || !valueTypeJson.isString() ) {
					throw new TranslationException( String.format( "The associate value type is missing." ) );
				} else if( valueJson == null ) {
					throw new TranslationException( String.format( "The associate value for type '%s' is missing.", valueTypeJson.getAsString() ) );
				} else {
					String valueTypeString = valueTypeJson.getAsString();
					JsonTypeReference typeReference = typeReferences.get( valueTypeString );
					
					if( typeReference == null ) {
						throw new TranslationException( String.format( "Json is referring to a type '%s' that isn't supported.", valueTypeString ) );
					} else {
						returnValue = typeReference.getFromJsonTranslator().translate( valueJson );
					}
				}

			// TODO: catch the various gson json exceptions
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
