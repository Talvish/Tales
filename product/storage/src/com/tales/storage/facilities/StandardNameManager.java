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
package com.tales.storage.facilities;

import com.google.common.base.Preconditions;
import com.tales.parts.ValidationException;
import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.naming.NameValidator;

public class StandardNameManager implements NameManager {
	private final NameValidator tableNameValidator;
	private final String tableNamePrefix;
	
//	private final NameValidator familyNameValidator;
//	private final NameValidator columnNameValidator;
	
	public StandardNameManager( ) {
		this( null, new LowerCaseEntityNameValidator( ) );
	}
	
	public StandardNameManager( String theTableNamePrefix, NameValidator theValidator ) {
		Preconditions.checkNotNull( theValidator, "need a validator" );
		
		tableNameValidator = theValidator;
		tableNamePrefix = theTableNamePrefix; // this could be null
	}

	@Override
	public String confirmTableName(String theName) {
		if( !tableNameValidator.isValid(theName)) {
			throw new ValidationException( String.format( "Table name '%s' does not conform to validator '%s'", theName, tableNameValidator.getClass().getSimpleName( ) ) );
		}
		return theName;
	}
	
	public String ensureTableName( String theName ) {
		return tableNamePrefix + theName;
	}
//
//	@Override
//	public String confirmFamilyName(String theName) {
//		if( !tableNameValidator.isValid(theName)) {
//			throw new ValidationException( String.format( "Family name '%s' does not conform to validator '%s'", theName, familyNameValidator.getClass().getSimpleName( ) ) );
//		}
//		return theName;
//	}
//
//	@Override
//	public String confirmColumnName(String theName) {
//		if( !tableNameValidator.isValid(theName)) {
//			throw new ValidationException( String.format( "Column name '%s' does not conform to validator '%s'", theName, columnNameValidator.getClass().getSimpleName( ) ) );
//		}
//		return theName;
//	}
}
