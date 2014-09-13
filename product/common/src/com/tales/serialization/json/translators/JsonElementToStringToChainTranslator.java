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
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;

/**
 * A translator that calls .getAsString on the object to translate before passing to 
 * another translator.
 * @author jmolnar
 *
 */
public class JsonElementToStringToChainTranslator implements Translator {
	private final Translator chainedTranslator;

	/**
	 * Constructor taking the translator used to translate the underlying object, which is translated as a string first.
	 * @param theChainedTranslator
	 */
	public JsonElementToStringToChainTranslator( Translator theChainedTranslator ) {
		Preconditions.checkNotNull( theChainedTranslator, "need the chained translator" );
		chainedTranslator = theChainedTranslator;
	}

	/**
	 * Translates the object as a string first and then into the chained translator.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		if( anObject == null || anObject.equals( JsonNull.INSTANCE )) {
			returnValue = chainedTranslator.translate( null );
		} else {
			try {
				returnValue = chainedTranslator.translate( ( ( JsonElement )anObject ).getAsString( ) );
			} catch( ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}