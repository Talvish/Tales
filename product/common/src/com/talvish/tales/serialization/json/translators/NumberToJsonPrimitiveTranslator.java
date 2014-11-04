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

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;


/**
 * Translator that converts a Number (aka int, long, float, etc) into a Json Primitive
 * or if null, then {@code JsonNull}.
 * @author jmolnar
 *
 */
public class NumberToJsonPrimitiveTranslator implements Translator {
	/**
	 * Empty default constructor.
	 */
	public NumberToJsonPrimitiveTranslator( ) {
	}

	/**
	 * Translates the received object into a json primitive.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		try {
			if( anObject == null ) {
				return JsonNull.INSTANCE;
			} else {
				return new JsonPrimitive( ( Number )anObject );
			}
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
}
