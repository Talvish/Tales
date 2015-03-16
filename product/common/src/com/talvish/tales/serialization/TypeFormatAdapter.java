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
package com.talvish.tales.serialization;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.translators.Translator;

/**
 * A class that hold references to simple and complex types and how to translate the type
 * both to and from a particular format. This is useful for persistence or transport.
 * Examples: translating to/from json, or translator to expected database formats, etc.
 * @author Joseph Molnar
 *
 */
public class TypeFormatAdapter {
	private final JavaType type;
	private final String name;

	private final Translator toFormatTranslator;
	private final Translator fromFormatTranslator;

	public TypeFormatAdapter( JavaType theType, String theName, Translator theFromFormatTranslator, Translator theToFormatTranslator ) {
		Preconditions.checkNotNull( theType, "type is required" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "name is required" );
		Preconditions.checkNotNull( theToFormatTranslator, "to format translator is required" );
		Preconditions.checkNotNull( theFromFormatTranslator, "from format translator is required" );

		type = theType;
		name = theName;
		toFormatTranslator = theToFormatTranslator;
		fromFormatTranslator = theFromFormatTranslator;
	}
	
	/**
	 * The type this class is being used for.
	 */
	public JavaType getType( ){ 
		return this.type;
	}
	
	/**
	 * The serialization specific name.
	 */
	public String getName( ) {
		return this.name;
	}
	
	/**
	 * The translator to turn data into the specific serialization format.
	 */
	public Translator getToFormatTranslator( ) {
		return this.toFormatTranslator;
	}

	/**
	 * The translator to turn data from the serialization specific format to the type.
	 */
	public Translator getFromFormatTranslator( ) {
		return this.fromFormatTranslator;
	}
	
	public Object translateToFormat( Object aTypeInstance) {
		return this.toFormatTranslator.translate( aTypeInstance );
	}

	public Object translateFromFormat( Object aFormattedInstance ) {
		return this.fromFormatTranslator.translate( aFormattedInstance );
	}
}
