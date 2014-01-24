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
package com.tales.storage.hbase;

import com.google.common.base.Preconditions;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.storage.StorageType;

/**
 * This class represents the key to the table.
 * @author jmolnar
 *
 */
public class HBaseKeyMap {
	private final Translator keyTranslator;
	private final HBaseTableMap containingTable;

	/**
	 * The constructor taking the value to bytes translator and the table the key is for
	 * @param theKeyTranslator the translator for the table
	 * @param theContainingTable the table the key is for
	 */
	public HBaseKeyMap( Translator theKeyTranslator, HBaseTableMap theContainingTable ) {
		Preconditions.checkNotNull( theKeyTranslator, "need a key translator" );
		Preconditions.checkNotNull( theContainingTable, "need the containing table" );
		
		keyTranslator = theKeyTranslator;
		containingTable = theContainingTable;
	}

	/**
	 * The translator used to convert from the key to bytes.
	 * @return
	 */
	public Translator getKeyTranslator( ) {
		return keyTranslator;
	}
	
	/**
	 * The table this key is for.
	 * @return the table the key is for
	 */
	public HBaseTableMap getContainingTable( ) {
		return containingTable;
	}
	
	/**
	 * Gets data for the key from the instance passed in. 
	 * @param theInstance the object that contains the key
	 * @return the byte representation result of what is in the key
	 */
	public byte[] getData( Object theInstance ) {
		try {
			StorageType rootType = containingTable.getStorageType();
			Object id = rootType.getIdInstance(theInstance);
			return ( byte[] )keyTranslator.translate( id );
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
}
