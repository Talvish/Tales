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

import java.util.Collection;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;


/**
 * Translator that converts a collection into a json array,
 * or, if null, {@code JsonNull}.
 * @author jmolnar
 *
 */
public class CollectionToJsonArrayTranslator implements Translator {
	private final Translator elementTranslator;
	
	/**
	 * Empty default constructor.
	 */
	public CollectionToJsonArrayTranslator( Translator theElementTranslator ) {
		Preconditions.checkNotNull( theElementTranslator );
		
		elementTranslator = theElementTranslator;
	}

	/**
	 * Translates the received object into a json array.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = JsonNull.INSTANCE;
		} else {
			try {
				Collection<?> array = ( Collection<?> )anObject;
				JsonArray jsonArray = new JsonArray( );
				
				for( Object object : array ) {
					jsonArray.add( ( JsonElement )elementTranslator.translate( object ) );
				}
				returnValue = jsonArray;

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
