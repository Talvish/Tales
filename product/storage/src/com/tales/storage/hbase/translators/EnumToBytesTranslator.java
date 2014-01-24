// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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

public class EnumToBytesTranslator extends NullTranslatorBase implements Translator {
	private final Class<?> enumClass;

	public EnumToBytesTranslator( Class<?> theEnumClass ) {
		this( theEnumClass, null );
	}

	public EnumToBytesTranslator( Class<?> theEnumClass, Object theNullValue ) {
		super(theNullValue);
		Preconditions.checkNotNull( theEnumClass, "need an enum class" );
		Preconditions.checkArgument( theEnumClass.isEnum(), String.format( "'%s' needs to be an enum class", theEnumClass.getSimpleName( ) ) );
		enumClass= theEnumClass;
	}
	
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				returnValue = Bytes.toBytes( enumClass.cast( anObject ).toString( ) );
			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}
}
