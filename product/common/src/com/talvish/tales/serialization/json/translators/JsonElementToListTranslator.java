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

import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import com.talvish.tales.parts.translators.NullTranslatorBase;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;

public class JsonElementToListTranslator extends NullTranslatorBase implements Translator {
	private Translator elementTranslator = null;
	
	public JsonElementToListTranslator ( Translator theElementTranslator ) {
		this( theElementTranslator, null );
	}
	
	public JsonElementToListTranslator ( Translator theElementTranslator, Object theNullValue ) {
		super(theNullValue);
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
				JsonElement element = ( JsonElement )anObject;
				if( element.isJsonArray() ) {
					// if we have an array, we take all the values, translate and put into the array list
					ArrayList list = new ArrayList( ) ;

					JsonArray array = ( JsonArray )element;
					
					for( JsonElement entry: array ) {
						list.add( elementTranslator.translate( entry ) );
					}
					returnValue = list;
				
					// TODO: support handling a single item array that doesn't require array syntax
					
				} else {
					throw new TranslationException( String.format( "Attempting to translate '%s' but it is not an array.", element.toString( ) ) );
				}
			} catch( JsonParseException | ClassCastException | IllegalStateException | UnsupportedOperationException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
