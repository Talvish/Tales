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
package com.tales.client.http;

import java.lang.reflect.Type;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.translators.Translator;
import com.tales.system.Conditions;

/**
 * This class represents any form of parameter that can exist, be it 
 * path, header, cookie, query, etc, on a request.
 * @author jmolnar
 *
 */
public class ResourceMethodParameter {
	private final String name;
	private final int index; // this is used to help get items quickly
	private final Class<?> type;
	private final Type genericType;
	private final Translator translator;
	
	protected ResourceMethodParameter( String theName, int theIndex, Class<?> theType, Type theGenericType, Translator theTranslator ) {
		Conditions.checkParameter( !Strings.isNullOrEmpty( theName ),  "theName" );
		Preconditions.checkArgument( theIndex >= 0, "parameter '%s' is using an invalid index value of '%s'", theName, theIndex );
		Conditions.checkParameterNull( theTranslator,  "theTranslator" );
		Conditions.checkParameterNull( theType, "theType" );
		
		name = theName;
		type = theType;
		genericType = theGenericType;
		translator = theTranslator;
		index = theIndex;
	}
	
	/**
	 * The name of the parameter.
	 * @return the name of the parameter
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The path parameters this represents the index of the parameter in the path.
	 * For other parameter types it represents the order that parameters were added
	 * @return the index or order of the parameter
	 */
	public int getIndex( ) {
		return index;
	}
	
	/**
	 * The type of the parameter.
	 * @return the type of the parameter
	 */
	public Class<?> getType( ) {
		return type;
	}
	
	/**
	 * The generic type, if applicable, for a parameter.
	 * @return the generic type, though may be null if not set
	 */
	public Type getGenericType( ) {
		return genericType;
	}
	
	/**
	 * The translator to use to convert the in-memory value of the 
	 * parameter to the transport/network version of the parameter.
	 * @return the translator to use
	 */
	public Translator getTranslator( ) { 
		return translator;
	}
}
