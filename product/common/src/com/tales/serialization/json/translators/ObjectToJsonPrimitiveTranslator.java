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

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.tales.parts.translators.Translator;


/**
 * Translator that converts an object into a Json Primitive or
 * or if null, then {@code JsonNull}.
 * @author jmolnar
 *
 */
public class ObjectToJsonPrimitiveTranslator implements Translator {
	/**
	 * Empty default constructor.
	 */
	public ObjectToJsonPrimitiveTranslator( ) {
	}

	/**
	 * Translates the received object into a json primitive.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		if( anObject == null ) {
			return JsonNull.INSTANCE;
		} else {
			return new JsonPrimitive( anObject.toString( ) );
		}
	}
}
