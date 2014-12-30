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
package com.talvish.tales.auth.capabilities;

import com.talvish.tales.parts.translators.NullTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

/**
 * Translator that converts Capabilities into something that can be placed into a string claim
 * so it can be used by something like the json web token based Token Manager.
 * The family does not need to be know in this case because it is contained within the 
 * Capabilities class itself, and ultimately isn't stored on the value side of the claim
 * anyhow.
 * @author jmolnar
 *
 */
public class TokenCapabilityToStringTranslator extends NullTranslatorBase implements Translator {
	/**
	 * Constructor that instructs a null object to be set to null string.
	 */
	public TokenCapabilityToStringTranslator( ) {
		this( null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received. 
	 * @param theNullValue the null value to use
	 */
	public TokenCapabilityToStringTranslator(String theNullValue) {
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
				returnValue = ( ( Capabilities )anObject ).getCapabilityString( );
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
