//***************************************************************************
//*  Copyright 2016 Joseph Molnar
//*
//*  Licensed under the Apache License, Version 2.0 (the "License");
//*  you may not use this file except in compliance with the License.
//*  You may obtain a copy of the License at
//*
//*      http://www.apache.org/licenses/LICENSE-2.0
//*
//*  Unless required by applicable law or agreed to in writing, software
//*  distributed under the License is distributed on an "AS IS" BASIS,
//*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//*  See the License for the specific language governing permissions and
//*  limitations under the License.
//***************************************************************************
package com.talvish.tales.serialization.json.translators;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import com.talvish.tales.parts.translators.NullTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

/**
* A translator that takes a JsonElement and translates it into the desired type.
* @author jmolnar
*
*/
public class JsonElementToDoubleTranslator extends NullTranslatorBase implements Translator {
	/**
	 * Constructor taking the value to use if a null.
	 * @param theNullValue the null value to return if the value translating is null
	 */
	public JsonElementToDoubleTranslator( Object theNullValue ) {
		super( theNullValue );
	}

	/**
	 * Translates json element into the desired value type.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		if( anObject == null || anObject.equals( JsonNull.INSTANCE )) {
			returnValue = this.nullValue;
		} else {
			try {
				returnValue = ( ( JsonElement )anObject ).getAsDouble();
			} catch( ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}