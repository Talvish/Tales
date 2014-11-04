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

import com.google.gson.JsonObject;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;


/**
 * Translator for the Void type, which results in an empty json object.
 * If the value sent in is anything other than null then a 
 * TranslationException will occur.
 * @author jmolnar
 *
 */
public class VoidToJsonObjectTranslator implements Translator {
	/**
	 * Empty default constructor.
	 */
	public VoidToJsonObjectTranslator( ) {
	}

	/**
	 * Translates the received object into a json primitive.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject != null ) {
			throw new TranslationException( String.format( "An object of type '{%s} is attempting to be used as a Void.", anObject.getClass().getName() ) );
		} else {
			returnValue = new JsonObject( );			
		}
		return returnValue;	
	}
}
