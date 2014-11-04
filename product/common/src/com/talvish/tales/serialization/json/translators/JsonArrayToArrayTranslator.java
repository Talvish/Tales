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
import com.google.gson.JsonNull;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;


/**
 * Translator that converts a json array into a Java array.
 * @author jmolnar
 *
 */
public class JsonArrayToArrayTranslator implements Translator {
	private final Translator elementTranslator;
	private final Class<?> elementType;
	
	/**
	 * Constructor taking the element type and the translator type.
	 */
	public JsonArrayToArrayTranslator( Class<?> theElementType, Translator theElementTranslator ) {
		Preconditions.checkNotNull( theElementType, "need a type" );
		Preconditions.checkNotNull( theElementTranslator, "need a translator" );
		
		elementType = theElementType;
		elementTranslator = theElementTranslator;
	}

	/**
	 * Translates the received object into an array;
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
				Object array = Array.newInstance( elementType, jsonArray.size() );
				
				for( int count = 0; count < jsonArray.size( ); count += 1 ) {
					Array.set( array, count, elementTranslator.translate( jsonArray.get( count ) ) );
				}
				
				returnValue = array;
			} catch( IllegalArgumentException e ) {
				throw new TranslationException( e );
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
