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
package com.talvish.tales.serialization.json.translators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.TypeFormatAdapter;

/**
 * A translator that looks at the type to be translated and picks an appropriate translator.
 * The translator doesn't support more than one class where the only difference is generic
 * type parameters since the information is not available during the serialization process.
 * Fixing this may be impossible or at least very difficult to do in Java.
 * @author jmolnar
 *
 */
public class PolymorphicObjectToJsonObjectTranslator implements Translator {
	private final Map<Class<?>, TypeFormatAdapter> typeAdapters = new HashMap<>( 2 );

	/**
	 * Constructor taking the needed adapters.
	 */
	public PolymorphicObjectToJsonObjectTranslator( List<TypeFormatAdapter> theTypeAdapters ) {
		Preconditions.checkNotNull( theTypeAdapters );
		Preconditions.checkArgument( theTypeAdapters.size( ) > 0, "Need at least one value type adapter." );

		for( TypeFormatAdapter typeAdapter : theTypeAdapters ) {
			Preconditions.checkArgument( !typeAdapters.containsKey( typeAdapter.getType().getUnderlyingClass()), String.format( "Attempting to add type adapter '%s' more than once (differences in generic type parameters are not sufficient).", typeAdapter.getType( ).getUnderlyingClass().getName()));
			typeAdapters.put( typeAdapter.getType().getUnderlyingClass(), typeAdapter );
		}
	}

	/**
	 * Translates the received object into the appropriate json representation.
	 * If the object is of the wrong type or the translator used doesn't produce
	 * JsonElements's then a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = JsonNull.INSTANCE;
		} else {
			TypeFormatAdapter typeAdapter = typeAdapters.get( anObject.getClass( ) );
			if( typeAdapter == null ) {
				throw new TranslationException( String.format( "An object of type '%s' was attempting to be converted to a json object, but this object isn't supported", anObject.getClass( ).getName( ) ));
			} else {
				try {
					JsonObject jsonEntry = new JsonObject( );
					
					jsonEntry.addProperty( "value_type", typeAdapter.getName() );
					jsonEntry.add( "value", ( JsonElement )typeAdapter.getToFormatTranslator().translate( anObject ) );
	
					returnValue = jsonEntry;
	
				} catch( ClassCastException e ) {
					throw new TranslationException( e );
				}
			}
		}
		return returnValue;	
	}
}
