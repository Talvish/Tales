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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.talvish.tales.parts.translators.StringToObjectTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

/**
 * This translator is meant to be used with the something like the json web token based
 * Token Manger. It will help translate claim string into Capabilities. This means
 * it needs to also know the family associated with the capabilities so it can store it.
 * @author jmolnar
 *
 */
public class StringToTokenCapabilityTranslator extends StringToObjectTranslatorBase implements Translator {
	private final String family;

	public StringToTokenCapabilityTranslator( String theFamily ) {
		this( theFamily, true, null, null );
	}
	public StringToTokenCapabilityTranslator( String theFamily, boolean shouldTrim, Object theEmptyValue, Object theNullValue) {
		super(shouldTrim, theEmptyValue, theNullValue);
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need a family" );
		
		family = theFamily;
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
					returnValue = new Capabilities( family, stringValue );
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