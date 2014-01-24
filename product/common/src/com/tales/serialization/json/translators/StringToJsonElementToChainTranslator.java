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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;

/**
 * A translator that parses a string into {@code JsonElements}s before passing to 
 * another translator.
 * @author jmolnar
 *
 */
public class StringToJsonElementToChainTranslator  implements Translator {
	// TODO: consider inheriting from one of the string translators
	private static final JsonParser parser = new JsonParser(); // this is thread safe, so we only need one
	private final Translator chainedTranslator;

	public StringToJsonElementToChainTranslator( Translator theChainedTranslator ) {
		chainedTranslator = theChainedTranslator;
	}

	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		if( anObject == null ) {
			returnValue = chainedTranslator.translate( JsonNull.INSTANCE );
		} else {
			try {
				// NOTE: there is a bug in GSON that if the string is missing an ending curly brace then it doesn't report a json parsing exception
				returnValue = chainedTranslator.translate( parser.parse( ( String )anObject ) );
			} catch( JsonParseException e ) {
				throw new TranslationException( e );
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}