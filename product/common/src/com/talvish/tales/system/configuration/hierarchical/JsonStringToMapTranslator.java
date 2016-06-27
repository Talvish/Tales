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
package com.talvish.tales.system.configuration.hierarchical;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.talvish.tales.parts.translators.StringToObjectTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

public class JsonStringToMapTranslator extends StringToObjectTranslatorBase implements Translator {
	private static final JsonParser parser = new JsonParser( );
	private final Translator keyTranslator;
	private final Translator valueTranslator;
	
	@SuppressWarnings("rawtypes") // TODO: this works but considered, need to find a better way to handle the generics
	public JsonStringToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator ) {
		this( theKeyTranslator, theValueTranslator, true, new HashMap( 0 ), null );
	}
	
	public JsonStringToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator, boolean shouldTrim, Object theEmptyValue, Object theNullValue ) {
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
					JsonElement element = parser.parse( stringValue );
					
					if( element.isJsonObject( ) ) {
						HashMap map = new HashMap( );

						JsonObject object = ( JsonObject )element;
						Set<Entry<String,JsonElement>> set = object.entrySet();
						
						for( Entry<String, JsonElement> entry : set ) {
							if( entry.getValue().isJsonPrimitive( ) ) {
								map.put( keyTranslator.translate( entry.getKey( ) ), valueTranslator.translate( entry.getValue().getAsString( ) ) );
							} else {
								throw new TranslationException( String.format( "Key '%s' is attempting to use non-primitive value '%s'.", entry.getKey( ), entry.getValue().toString( ) ) );
							}
						}
						returnValue = map;
					} else {
						throw new TranslationException( String.format( "String '%s' is not a json object.", stringValue ) );
					}
				}
			} catch( JsonParseException | ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}

