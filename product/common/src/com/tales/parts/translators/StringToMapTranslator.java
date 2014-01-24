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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

public class StringToMapTranslator extends StringToObjectTranslatorBase implements Translator {
	private static final String ELEMENT = "(?:(?:[^,\\\\\\[\\]]|(?:\\\\.))*)";
	private static final String MAP_REGEX = String.format("(?:\\s*\\[)(%1$s)(?:,)(%1$s)(?:\\]\\s*)", ELEMENT );
	private static final Pattern MAP_PATTERN = Pattern.compile( MAP_REGEX );
	
	private static final String REPLACE_REGEX = "(\\\\)(.)";
	private static final Pattern REPLACE_PATTERN = Pattern.compile( REPLACE_REGEX );
	

	private final Translator keyTranslator;
	private final Translator valueTranslator;
	
	@SuppressWarnings("rawtypes") // TODO: this works but considered, need to find a better way to handle the generics
	public StringToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator ) {
		this( theKeyTranslator, theValueTranslator, true, new HashMap( 0 ), null );
	}
	
	public StringToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator, boolean shouldTrim, Object theEmptyValue, Object theNullValue ) {
		super(shouldTrim, theEmptyValue, theNullValue);
		Preconditions.checkNotNull( theKeyTranslator, "theKeyTranslator" );
		Preconditions.checkNotNull( theValueTranslator, "theValueTranslator" );
		
		keyTranslator = theKeyTranslator;
		valueTranslator = theValueTranslator;
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
					Matcher matcher = MAP_PATTERN.matcher( stringValue );
					HashMap map = new HashMap( );
					
					String key;
					String value;
					int lastMatchLocation = 0;
					
					while( matcher.find( ) ) {
						key = matcher.group( 1 );
						value = matcher.group( 2 );

						if( matcher.start() != lastMatchLocation ) {
							throw new TranslationException( String.format( "Around offset '%2$d' string '%1$s' does not match the map pattern.", stringValue, lastMatchLocation ) );
						} else {
							lastMatchLocation = matcher.end( );
							key = unescape( key );
							value = unescape( value );
							
							map.put( keyTranslator.translate( key ), valueTranslator.translate( value ) );
						}
					}
					
					if( lastMatchLocation != stringValue.length( ) ) {
						throw new TranslationException( String.format( "Around offset '%2$d' string '%1$s' does not match the map pattern.", stringValue, lastMatchLocation ) );
					} else {
						returnValue = map;
					}
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
