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
package com.talvish.tales.serialization.json.translators;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.talvish.tales.parts.translators.NullTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

public class JsonElementToMapTranslator extends NullTranslatorBase implements Translator {
	private final Translator keyTranslator;
	private final Translator valueTranslator;
	
	public JsonElementToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator ) {
		this( theKeyTranslator, theValueTranslator, null );
	}
	
	public JsonElementToMapTranslator( Translator theKeyTranslator, Translator theValueTranslator, Object theNullValue ) {
		super(theNullValue);
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
				JsonElement element = ( JsonElement )anObject;
				
				if( element.isJsonObject( ) ) {
					HashMap map = new HashMap( );

					JsonObject object = ( JsonObject )element;
					Set<Entry<String,JsonElement>> set = object.entrySet();
					
					for( Entry<String, JsonElement> entry : set ) {
						map.put( keyTranslator.translate( entry.getKey( ) ), valueTranslator.translate( entry.getValue( ) ) );
					}
					returnValue = map;
				} else {
					throw new TranslationException( String.format( "String '%s' is not a json object that can be used for a map.", element.toString() ) );
				}
			} catch( JsonParseException | ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}

