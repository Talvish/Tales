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
package com.talvish.tales.parts.translators;

import java.util.BitSet;

/**
 * Generates a BitSet from a String. 
 * The class expects the string to be in little endian byte ordering.
 * @author jmolnar
 *
 */
public class StringToBitSetTranslator extends StringToObjectTranslatorBase implements Translator {

	public StringToBitSetTranslator( ) {
		this( true, null, null );
	}
	public StringToBitSetTranslator( boolean shouldTrim, Object theEmptyValue, Object theNullValue) {
		super(shouldTrim, theEmptyValue, theNullValue);
	}

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
					returnValue = BitSet.valueOf( DatatypeConverter.parseHexBinary( stringValue ) );
				}
			} catch( IllegalArgumentException e ) {
				throw new TranslationException( String.format( "Unable to translate '%s' into a BitSet.", anObject ), e);
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}

