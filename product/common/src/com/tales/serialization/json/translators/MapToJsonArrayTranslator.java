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

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;


/**
 * Translator that converts a map into a json array of key/value json objects.,
 * or, if null, {@code JsonNull}.
 * @author jmolnar
 *
 */
public class MapToJsonArrayTranslator implements Translator {
	private final Translator keyTranslator;
	private final Translator valueTranslator;
	
	/**
	 * Constructor taking the needed translator.
	 */
	public MapToJsonArrayTranslator( Translator theKeyTranslator, Translator theValueTranslator ) {
		Preconditions.checkNotNull( theKeyTranslator );
		Preconditions.checkNotNull( theValueTranslator );
		
		keyTranslator = theKeyTranslator;
		valueTranslator = theValueTranslator;
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
				
				for( Entry<?, ?> entry : map.entrySet() ) {
					jsonEntry = new JsonObject();
					jsonEntry.add( "key", ( JsonElement )keyTranslator.translate( entry.getKey() ) );
					jsonEntry.add( "value", ( JsonElement )valueTranslator.translate( entry.getValue() ) );
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
