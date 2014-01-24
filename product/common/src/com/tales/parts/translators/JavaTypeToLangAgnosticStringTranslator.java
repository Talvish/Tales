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

import org.joda.time.DateTime;
import org.joda.time.Period;


/**
 * Translator that converts a class into language agnostic string name.
 * @author jmolnar
 *
 */
public class JavaTypeToLangAgnosticStringTranslator extends NullTranslatorBase implements Translator {
	private final HashMap<Class<?>, String> baseTypeMap = new HashMap<Class<?>, String>();
	
	/**
	 * Constructor that instructs a null object to be set to null string.
	 */
	public JavaTypeToLangAgnosticStringTranslator( ) {
		this( null );
	}

	/**
	 * Constructor that takes the string value to return when a null object is received. 
	 * @param theNullValue the null value to use
	 */
	public JavaTypeToLangAgnosticStringTranslator(String theNullValue) {
		super(theNullValue);

		baseTypeMap.put( int.class, "int32" );
		baseTypeMap.put( Integer.class, "int32" );
		baseTypeMap.put( long.class, "int64" );
		baseTypeMap.put( Long.class, "int64" );
		baseTypeMap.put( float.class, "float32" );
		baseTypeMap.put( Float.class, "float32" );
		baseTypeMap.put( double.class, "float64" );
		baseTypeMap.put( Double.class, "float64" );

		baseTypeMap.put( boolean.class, "boolean" );
		baseTypeMap.put( Boolean.class, "boolean" );

		baseTypeMap.put( String.class, "string" );
		baseTypeMap.put( DateTime.class, "datetime" );
		baseTypeMap.put( Period.class, "period" );
	}
	
	/**
	 * Translates the received object into a string.
	 * If the object to translate isn't null but is of the wrong 
	 * type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				Class<?> value = ( Class<?> )anObject;
				returnValue = baseTypeMap.get( value );
				if( returnValue == null ) {
					if( value.isEnum( ) ) {
						returnValue = "enum";
					} else {
						returnValue = "unknown";
					}
				}
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}