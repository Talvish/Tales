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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.base.Preconditions;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.storage.CompoundIdField;
import com.tales.storage.CompoundIdType;

public class KeyToBytesTranslator implements Translator {
	private final CompoundIdType compoundIdType;
	private final List<Translator> translators;
	
	public KeyToBytesTranslator( CompoundIdType theIdType, List<Translator> theTranslators ) {
		Preconditions.checkNotNull( theIdType, "need an id type");
		Preconditions.checkNotNull( theTranslators, "need translators" );
		
		compoundIdType = theIdType;
		translators = Collections.unmodifiableList( new ArrayList<Translator>( theTranslators ) );
	}

	@Override
	public Object translate(Object anObject) {
		Preconditions.checkNotNull( anObject, String.format("Attempt to translate a null key for type '%s'", compoundIdType.getName( ) ) );
		try {
			// TODO: this is not great at all. 
			//       ideally this allocates one array and adds all to it
			//       which implies knowing byte sizes, etc. I think we 
			//       want special translators for this
			int index = 0;
			byte[] returnBytes = null;
			for( CompoundIdField field : compoundIdType.getFields( ) ) {
				byte[] fieldBytes = ( byte[] )translators.get( index ).translate( field.getData( anObject ) );

				// make sure it isn't null
				if( fieldBytes == null) {
					throw new TranslationException( String.format( "Compound id field '%s.'%s' must not have a null byte value.", field.getContainingType().getName(), field.getName() ) );
				}
				if( returnBytes == null ) {
					returnBytes = fieldBytes; 
				} else {
					returnBytes = Bytes.add( returnBytes, fieldBytes );
				}
				index += 1;
			}
			return returnBytes;
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
}
