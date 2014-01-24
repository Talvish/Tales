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

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

public class StringToListTranslator extends StringToObjectTranslatorBase implements Translator {
	private static final String REPLACE_REGEX = "(\\\\)(.)";
	private static final Pattern REPLACE_PATTERN = Pattern.compile( REPLACE_REGEX );

	private final char delimiter;
	private Translator elementTranslator = null;
	
	@SuppressWarnings("rawtypes") // TODO: this works but considered, need to find a better way to handle the generics
	public StringToListTranslator( Translator theElementTranslator ) {
		this( ',', theElementTranslator, true, new ArrayList( 0 ), null );
	}
	
	public StringToListTranslator( char theDelimiter, Translator theElementTranslator, boolean shouldTrim, Object theEmptyValue, Object theNullValue ) {
		super(shouldTrim, theEmptyValue, theNullValue);
		Preconditions.checkNotNull( theElementTranslator, "theElementTranslator" );
		
		delimiter = theDelimiter;
		elementTranslator = theElementTranslator;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // TODO: this works but considered, need to find a better way to handle the generics
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
				if( stringValue.equals( "" ) ) {
					returnValue = this.emptyValue;
				} else {
					ArrayList list = new ArrayList( ) ;

					boolean previousSlash = false;
					int startElement = 0;
					int endElement = 0;
					char currentChar;

					for( ; endElement < stringValue.length(); endElement += 1 ) {
						currentChar = stringValue.charAt( endElement );
						if( currentChar == this.delimiter ) {
							if( !previousSlash ) {
								list.add( elementTranslator.translate( unescape( stringValue.substring( startElement, endElement ) ) ) );
								startElement = endElement + 1;
							}
							previousSlash = false;
						} else if( currentChar == '\\' ) {
							previousSlash = !previousSlash;
						} else {
							previousSlash = false;
						}
					}

					if( startElement < endElement ) {
						list.add( elementTranslator.translate( unescape( stringValue.substring( startElement, endElement ) ) ) );
					} else if( startElement == endElement ) {
						list.add( elementTranslator.translate( "" ) );
					}

					returnValue = list;
				}
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}

	/**
	 * Unescapes the string by removing slash characters.
	 * @param theString the string to unescape.
	 * @return the unescaped string
	 */
	private String unescape( String theString ) {
		String returnValue = null;
		
		if( theString != null ) {
			returnValue = REPLACE_PATTERN.matcher( theString ).replaceAll( "$2" );
		}
		
		return returnValue;
	}

}
