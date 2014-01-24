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

import com.google.common.base.Preconditions;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;

/**
 * A translator that calls .getAsString on the object to translate before passing to 
 * another translator.
 * @author jmolnar
 *
 */
public class ChainToStringToJsonPrimitiveTranslator implements Translator {
	private final Translator chainedTranslator;

	public ChainToStringToJsonPrimitiveTranslator( Translator theChainedTranslator ) {
		Preconditions.checkNotNull( theChainedTranslator, "need the chained translator" );
		chainedTranslator = theChainedTranslator;
	}
	
	/**
	 * Translates the received object into a json primitive.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		try {
			anObject = chainedTranslator.translate( anObject );
			if( anObject == null ) {
				return JsonNull.INSTANCE;
			} else {
				return new JsonPrimitive( ( String )anObject );
			}
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
}