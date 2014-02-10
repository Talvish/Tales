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
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.serialization.json.JsonTypeReference;

public class PolymorphicObjectToJsonObjectTranslator implements Translator {
	private Map<Class<?>, JsonTypeReference> typeReferences = new HashMap<>( 2 );

	/**
	 * Constructor taking the needed references.
	 */
	public PolymorphicObjectToJsonObjectTranslator( List<JsonTypeReference> theTypeReferences ) {
		Preconditions.checkNotNull( theTypeReferences );

		for( JsonTypeReference typeReference : theTypeReferences ) {
			Preconditions.checkArgument( !typeReferences.containsKey( typeReference.getType()), String.format( "Attempting to add type reference '%s' more than once.", typeReference.getType( ).getName()));
			typeReferences.put( typeReference.getType( ), typeReference );
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
		
		if( anObject == null ) {
			returnValue = JsonNull.INSTANCE;
		} else {
			JsonTypeReference typeReference = typeReferences.get( anObject.getClass( ) );
			if( typeReference == null ) {
				throw new TranslationException( String.format( "An object of type '%s' was attempting to be converted to a json object, but this object isn't supported", anObject.getClass( ).getName( ) ));
			} else {
				try {
					JsonObject jsonEntry = new JsonObject( );
					
					jsonEntry.addProperty( "value_type", typeReference.getName() );
					jsonEntry.add( "value", ( JsonElement )typeReference.getToJsonTranslator().translate( anObject ) );
	
					returnValue = jsonEntry;
	
				} catch( ClassCastException e ) {
					throw new TranslationException( e );
				}
			}
		}
		return returnValue;	
	}
}
