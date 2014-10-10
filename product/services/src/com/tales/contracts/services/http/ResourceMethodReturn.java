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
package com.tales.contracts.services.http;

import com.google.common.base.Preconditions;
import com.tales.parts.reflection.JavaType;
import com.tales.parts.translators.Translator;

/**
 * This class represents the return type of an exposed method.
 * @author jmolnar
 *
 */
public class ResourceMethodReturn {
	// TODO: support translators in both directions AND getting/setting data (both directions)
	private final ResourceMethod resourceMethod; 
	private final JavaType type;
	private final boolean isVoid;
	private final boolean isResultWrapper;
	private final Translator valueTranslator;
	
	/**
	 * Constructor used when the return is a void.
	 * @param theType the type of the return values
	 * @param theGenericType the generic type for the return value, which can be used for getting generic information
	 * @param theResultTranslator the translator that will be used to translate
	 * @param theMethod the method this is a return value
	 */
	ResourceMethodReturn( JavaType theType, ResourceMethod theMethod ) {
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkNotNull( theMethod, "need a method" );
		
		type = theType;
		isVoid = true;
		valueTranslator = null;
		resourceMethod = theMethod;
		isResultWrapper = false;
	}

	/**
	 * Constructor used when a type is being returned, though 
	 * there is no indication if it is a complex or simple type.
	 * @param theType the type of the return values
	 * @param theGenericType the generic type for the return value, which can be used for getting generic information
	 * @param resultWrapper if true, indicates that when the data comes back, it has a ResourceResult wrapper
	 * @param theValueTranslator the translator that will be used to translate the result values
	 * @param theMethod the method this is a return value
	 */
	ResourceMethodReturn( JavaType theType, boolean resultWrapper, Translator theValueTranslator, ResourceMethod theMethod ) {
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkNotNull( theValueTranslator, "need a translator" );
		Preconditions.checkNotNull( theMethod, "need a method" );
		
		type = theType;
		isResultWrapper = resultWrapper;
		isVoid = false;
		valueTranslator = theValueTranslator;
		resourceMethod = theMethod;
	}

	/**
	 * The method this return object is a return object for.
	 * @return the method this return object is a return object for.
	 */
	public ResourceMethod getResourceMethod( ) {
		return resourceMethod;
	}

	/**
	 * Indicates if the result is a void type.
	 * @return true if the void, false otherwise
	 */
	public boolean isVoid( ) {
		return isVoid;
	}
	
	/**
	 * Indicate the response is a special type where the developer wants to have 
	 * more direct control of what to do.
	 * @return true if the special type, false otherwise
	 */
	public boolean isResourceResponse( ) {
		return isResultWrapper;
	}
	
	/**
	 * The return type of the method.
	 * @return the return type
	 */
	public JavaType getType( ) {
		return this.type;
	}
	
	/**
	 * This is called to translator results, using the assigned translator,
	 * when the method executes.
	 * @param theObject to translate
	 * @return the translated object
	 */
	public Object translate( Object theObject ) {
		return valueTranslator.translate( theObject );
	}
}