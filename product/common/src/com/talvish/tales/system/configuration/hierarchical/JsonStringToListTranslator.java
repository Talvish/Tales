//***************************************************************************
//*  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.system.configuration.hierarchical;

import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.talvish.tales.parts.translators.StringToObjectTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

public class JsonStringToListTranslator extends StringToObjectTranslatorBase implements Translator {
	private static final JsonParser parser = new JsonParser( );
	private Translator elementTranslator = null;
	
	@SuppressWarnings("rawtypes") // TODO: this works but considered, need to find a better way to handle the generics
	public JsonStringToListTranslator ( Translator theElementTranslator ) {
		this( theElementTranslator, true, new ArrayList( 0 ), null );
	}
	
	public JsonStringToListTranslator ( Translator theElementTranslator, boolean shouldTrim, Object theEmptyValue, Object theNullValue ) {
		super(shouldTrim, theEmptyValue, theNullValue);
		Preconditions.checkNotNull( theElementTranslator, "theElementTranslator" );
		
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
					JsonElement element = parser.parse( stringValue );
					
					if( element.isJsonArray() ) {
						// if we have an array, we take all the values, translate and put into the array list
						ArrayList list = new ArrayList( ) ;

						JsonArray array = ( JsonArray )element;
						
						for( JsonElement entry: array ) {
							if( entry.isJsonPrimitive( ) ) {
								list.add( elementTranslator.translate( entry.getAsString( ) ) );
							} else {
								throw new TranslationException( String.format( "Attempting to use non-primitive array entry '%s'.", entry.toString( ) ) );
							}
						}
						returnValue = list;
					
					// TODO: support handling a single item array that doesn't require array syntax
						
					// attempted to allow special casing for single fields but
					// JsonElementToStringToChain will strip quotes around strings 
					// before passing them off, which is important for other types
					// of translations, but causes a problem here since the above
					// parse call will fail since it is expecting quotes around 
					// strings ... only way to make work is catch the exception
					// and then try 
						
//					} else if( element.isJsonPrimitive( ) ) {
//						// if we have a primitive, single entry, we attempt to translate it as a single value for the list
//						ArrayList list = new ArrayList( ) ;
//						
//						list.add( elementTranslator.translate( element.getAsString( ) ) );
//
//						returnValue = list;

					} else {
						throw new TranslationException( String.format( "String '%s' is not a json array.", stringValue ) );
					}
				}
			} catch( JsonParseException | ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
