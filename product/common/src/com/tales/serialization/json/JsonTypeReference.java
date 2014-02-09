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
package com.tales.serialization.json;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.translators.Translator;

// TODO: consider storing this at the serialization level since it doesn't have to be specific to json.
/**
 * A class that hold references to simple and complex types and how to translate them
 * for persistence or transport.
 * @author Joseph Molnar
 *
 */
public class JsonTypeReference {
	private final Class<?> type;
	private final String name;

	private final Translator toJsonTranslator;
	private final Translator fromJsonTranslator;
	
	public JsonTypeReference( Class<?> theType, String theName, Translator theFromJsonTranslator, Translator theToJsonTranslator ) {
		Preconditions.checkNotNull( theType, "type is required" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "name is required" );
		Preconditions.checkNotNull( theToJsonTranslator, "to json translator is required" );
		Preconditions.checkNotNull( theFromJsonTranslator, "from json translator is required" );

		type = theType;
		name = theName;
		toJsonTranslator = theToJsonTranslator;
		fromJsonTranslator = theFromJsonTranslator;
	}
	
	/**
	 * The type this class is being used for.
	 */
	public Class<?> getType(){
		return this.type;
	}
	
	/**
	 * The serialization specific name.
	 */
	public String getName( ) {
		return this.name;
	}
	
	/**
	 * The translator to turn data into the specific serialization form.
	 */
	public Translator getToJsonTranslator( ) {
		return this.toJsonTranslator;
	}

	/**
	 * The translator to deal with from the serialization specific form.
	 */
	public Translator getFromJsonTranslator( ) {
		return this.fromJsonTranslator;
	}
}
