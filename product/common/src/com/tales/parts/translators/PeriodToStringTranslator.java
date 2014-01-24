// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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

package com.tales.parts.translators;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;

/**
 * Translator that converts a Period into a string.
 * @author jmolnar
 *
 */
public class PeriodToStringTranslator extends NullTranslatorBase implements Translator {
	private final PeriodFormatter formatter;
	
	/**
	 * Constructor that instructs a null object to be set to null string with no explicit formatter.
	 */
	public PeriodToStringTranslator( ) {
		this( null, null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received 
	 * as well as an optional formatter. 
	 * @param theNullValue the null value to use
	 * @param theFormatter the optional formatter to use to create the string
	 */
	public PeriodToStringTranslator(String theNullValue, PeriodFormatter theFormatter ) {
		super(theNullValue);
		formatter = theFormatter;
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
				Period value = ( Period )anObject;
				
				if( formatter == null ) {
					returnValue = value.toString();
				} else {
					returnValue = value.toString( formatter );
				}
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
