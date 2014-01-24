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
package com.tales.serialization.json;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;

import com.tales.parts.reflection.FieldDescriptor;
import com.tales.parts.sites.TranslatedDataSite;
import com.tales.parts.translators.TranslationException;

/**
 * This class represents a member of a class that will be
 * returned as a JSON formatted object.
 * @author jmolnar
 *
 */
public class JsonMemberMap {
	
	private final JsonTypeMap containingType;
	private final TranslatedDataSite dataSite;
	private final FieldDescriptor<?,?> reflectedField;

	/**
	 * The constructor taking field and the translator to translate to/from the field.
	 * @param theReflectedField the field
	 * @param theDataSite the site that translates the data
	 */
	public JsonMemberMap( FieldDescriptor<?,?> theReflectedField, TranslatedDataSite theDataSite, JsonTypeMap theContainingType ) {
		Preconditions.checkNotNull( theReflectedField );
		Preconditions.checkNotNull( theDataSite );
		Preconditions.checkNotNull( theContainingType );
		
		dataSite = theDataSite;
		reflectedField = theReflectedField;
		containingType = theContainingType;
	}
	
	/**
	 * The type that contains this member.
	 * @return the type that contains this member.
	 */
	public JsonTypeMap getContainingType( ) {
		return containingType;
	}
	
	/**
	 * The field.
	 * @return the field
	 */
	public FieldDescriptor<?,?> getReflectedField( ) {
		return reflectedField;
	}
	
	/**
	 * The item that translates data to/from field
	 * @return the item that translate data 
	 */
	public TranslatedDataSite getDataSite( ) {
		return dataSite;
	}
	
	/**
	 * Gets data for the field from the instance passed in. 
	 * @param theInstance the parent object that contains the field
	 * @return the Json representation result of what is in the field
	 */
	public JsonElement getData( Object theInstance ) {
		try {
			return ( JsonElement)dataSite.getData( theInstance );
		} catch( ClassCastException e ) {
			throw new TranslationException( e );
		}
	}
	
	/**
	 * Sets the data on the field for the instance passed in.
	 * @param theInstance the parent object that contains the field
	 * @param theValue the Json value, which will be translated, to set the field to
	 */
	public void setData( Object theInstance, JsonElement theValue ) {
		dataSite.setData( theInstance, theValue ); 
	}
}
