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
package com.tales.storage.hbase.translators;

import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.base.Preconditions;
import com.tales.parts.translators.NullTranslatorBase;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;

/**
 * This translator converts bytes into it's enum value.
 * @author jmolnar
 *
 */
public class BytesToEnumTranslator extends NullTranslatorBase implements Translator {
	private final Class<?> enumClass;

	public BytesToEnumTranslator( Class<?> theEnumClass ) {
		this( theEnumClass, null );
	}

	public BytesToEnumTranslator( Class<?> theEnumClass, Object theNullValue ) {
		super(theNullValue);
		Preconditions.checkNotNull( theEnumClass, "need an enum class" );
		Preconditions.checkArgument( theEnumClass.isEnum(), String.format( "'%s' needs to be an enum class", theEnumClass.getSimpleName( ) ) );
		enumClass= theEnumClass;
	}

	/**
	 * Translates a string into an enum value.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		String stringValue = null;
		
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				byte[] value = ( byte[] )anObject;
				if( value.length == 0) {
					returnValue = this.nullValue;
				} else {
					stringValue = Bytes.toString( value );				
					returnValue = Enum.valueOf( ( Class<Enum> )enumClass, stringValue );
				}
			} catch( IllegalArgumentException e ) {
				throw new TranslationException( String.format( "Unable to translate '%s' into an enum of type '%s'.", stringValue, enumClass.getSimpleName() ), e);
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}