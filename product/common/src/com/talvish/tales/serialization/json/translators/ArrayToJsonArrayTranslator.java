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

import java.lang.reflect.Array;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;


/**
 * Translator that converts a collection into a json array,
 * or, if null, {@code JsonNull}.
 * @author jmolnar
 *
 */
public class ArrayToJsonArrayTranslator implements Translator {
	private final Translator elementTranslator;
	private final boolean writeSingle;

	/**
	 * Constructor taking the element translator.
	 * There is no special handling of single element arrays.
	 * @param theElementTranslator the translator to use on the elements
	 */
	public ArrayToJsonArrayTranslator( Translator theElementTranslator ) {
		this( theElementTranslator, false );
	}

	/**
	 * Constructor taking the element translator and indication of 
	 * special handling of a single element array.
	 * @param theElementTranslator the translator to use on the elements
	 * @param writeSingle true means that if there is a single element it isn't written as an array but as the object
	 */
	public ArrayToJsonArrayTranslator( Translator theElementTranslator, boolean writeSingle ) {
		Preconditions.checkNotNull( theElementTranslator );
		
		elementTranslator = theElementTranslator;
		this.writeSingle = writeSingle; 
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
				if( anObject.getClass( ).isArray() ) {
					int arrayLength = Array.getLength( anObject ); 
					if( arrayLength != 1 || !writeSingle) {
						JsonArray jsonArray = new JsonArray( );
						for( int count = 0; count < arrayLength; count += 1 ) {
							jsonArray.add( ( JsonElement )elementTranslator.translate( Array.get( anObject, count ) ) );
						}
						returnValue = jsonArray;
					} else {
						// this happens we are allowing the single item arrays to be written out as a non-array
						returnValue = ( JsonElement )elementTranslator.translate( Array.get( anObject, 0 ) );
					}
				} else {
					throw new TranslationException( String.format( "Received a '%s' instead of an array, so unable to translate into a json array.", anObject.getClass( ).getName( ) ) );
				}
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
