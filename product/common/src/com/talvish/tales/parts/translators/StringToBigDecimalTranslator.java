// ***************************************************************************
// *  Copyright 2013 Joseph Molnar
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
package com.talvish.tales.parts.translators;

import java.math.BigDecimal;

/**
 * Translator that takes a string and changes it into BigDecimal.
 * @author jmolnar
 *
 */
public class StringToBigDecimalTranslator extends StringToObjectTranslatorBase implements Translator {

	/**
	 * Default constructor that will have set the translator to have
	 * trim as true and null for empty and null values.
	 */
	public StringToBigDecimalTranslator( ) {
		this( true, null, null );
	}
	
	/**
	 * Constructor for explicitly setting translation values.
	 * @param shouldTrim indicate if the string should be trimmed before translation
	 * @param theEmptyValue the value to use if the empty string is received
	 * @param theNullValue the value to use if null is received
	 */
	public StringToBigDecimalTranslator( boolean shouldTrim, Object theEmptyValue, Object theNullValue) {
		super(shouldTrim, theEmptyValue, theNullValue);
	}

	/**
	 * Translates the value of the object from a String to a BigDecimal.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				String stringValue = ( String )anObject;
				
				if( this.trim ) {
					stringValue = stringValue.trim();
				}
				if( stringValue.equals("") ) {
					returnValue = this.emptyValue;
				} else {
					returnValue = new BigDecimal( stringValue );
				}
			} catch( NumberFormatException e ) {
				throw new TranslationException( String.format( "Unable to translate '%s' into a big decimal.", anObject ), e);
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
