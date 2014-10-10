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

import com.google.common.base.Preconditions;
import com.tales.parts.reflection.JavaType;

/**
 * This class represents the return type of a resource method.
 * @author jmolnar
 *
 */
public class ResourceMethodReturn{
	private final JavaType type;

	/**
	 * Constructor to create the return type information of a resource method.
	 * @param theType the class that represents the type.
	 */
	public ResourceMethodReturn( JavaType theType ) {
		Preconditions.checkNotNull( theType, "need a type for the reutrn type" );
		type = theType;
	}
	
	/**
	 * The type the resource method returns.
	 * @return the type the resource method returns
	 */
	public JavaType getType( ) {
		return type;
	}
}