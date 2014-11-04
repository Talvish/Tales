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

import com.google.common.base.Preconditions;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.json.JsonTypeMap;


/**
 * Translator that converts a json object to an object.
 * @author jmolnar
 *
 */
public class JsonObjectToObjectTranslator implements Translator {
	private final JsonTypeMap typeMap;
	/**
	 * Empty default constructor.
	 */
	public JsonObjectToObjectTranslator( JsonTypeMap theTypeMap ) {
		Preconditions.checkNotNull( theTypeMap );
		
		typeMap = theTypeMap;
	}

	/**
	 * Translates the received object into a json primitive.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null || anObject.equals( JsonNull.INSTANCE ) ) {
			returnValue = null;
		} else {
			try {
				JsonObject jsonObject = ( JsonObject )anObject;
				
				returnValue = typeMap.getReflectedType().newInstance();
				typeMap.setData( returnValue, jsonObject );

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
