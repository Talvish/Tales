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
package com.talvish.tales.serialization.json.translators;


import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;


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
	 * Called when a translation is to occur for void, but generally
	 * it will only ever receive null, JsonNull or empty JsonObject's
	 * and that is fine, anything else is a problem (since this is for
	 * void)
	 */
	@Override
	public Object translate( Object anObject ) {
		if( anObject == null || anObject.equals( JsonNull.INSTANCE ) ) {
			return null;
		} else {
			try {
				JsonObject jsonObject = ( JsonObject )anObject;
	
				if( jsonObject.entrySet().size() != 0  ) {
					throw new TranslationException( "should not be receiving void data" );
				} else {
					return null; // no data here, so let's return null
				}
	
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
	}
}
