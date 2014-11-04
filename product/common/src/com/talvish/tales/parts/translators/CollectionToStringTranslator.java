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
package com.talvish.tales.parts.translators;

import java.util.Collection;

import com.google.common.base.Preconditions;

public class CollectionToStringTranslator extends NullTranslatorBase implements	Translator {
	private String delimiter;
	private Translator translator;
	
	public CollectionToStringTranslator( Translator theTranslator ) {
		this( ",", theTranslator, null );
	}
	
	public CollectionToStringTranslator( String theDelimiter, Translator theTranslator, Object theNullValue ) {
		super(theNullValue);
		Preconditions.checkNotNull(theTranslator, "theTranslator ");
		
		delimiter = theDelimiter;
		translator = theTranslator;
	}

	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null ) {
			returnValue = this.nullValue;
		} else {
			try {
				Collection<?> values = ( Collection<?> )anObject;

				StringBuffer buffer = new StringBuffer( );
				int index = 0;
				int listSize = values.size( );

				for( Object value : values ) {
				    buffer.append( translator.translate( value ) );
				    if( index < listSize - 1 ) {
				        buffer.append( delimiter );
				    }
				    index += 1;
				}
				returnValue = buffer.toString();

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			}
		}
		return returnValue;	
	}

}


