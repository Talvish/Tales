// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.auth.jwt;

import com.talvish.tales.parts.translators.NullTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

/**
 * Simple translator that converts a token to a String.
 * @author jmolnar
 *
 */
public class TokenToStringTranslator extends NullTranslatorBase implements Translator {
	/**
	 * Constructor that instructs a null object to be set to null string.
	 */
	public TokenToStringTranslator( ) {
		this( null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received. 
	 * @param theNullValue the null value to use
	 */
	public TokenToStringTranslator(String theNullValue) {
		super(theNullValue);
	}

	/**
	 * Translates the received object into a string.
	 * If the object to translate isn't null but is of the wrong 
	 * type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				returnValue = ( ( JsonWebToken )anObject ).getTokenString();
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
