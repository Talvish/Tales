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


import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;


/**
 * Translator is meant to fail if data is being sent for a void.
 * @author jmolnar
 *
 */
public class JsonObjectToVoidTranslator implements Translator {
	/**
	 * Empty default constructor.
	 */
	public JsonObjectToVoidTranslator( ) {
	}

	/**
	 * Called when a translation is to occur but it will always except 
	 * since void data should not be received.
	 */
	@Override
	public Object translate(Object anObject) {
		throw new TranslationException( "should not be receiving void data" );
	}
}
