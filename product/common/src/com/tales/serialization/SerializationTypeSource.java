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
package com.tales.serialization;

import java.util.Collection;

import com.tales.parts.reflection.FieldDescriptor;
import com.tales.parts.reflection.TypeDescriptor;

/**
 * This is a helper class to get type information and field information for classes that a 
 * developer would like to serialize. 
 * @author jmolnar
 *
 */
public interface SerializationTypeSource<T extends TypeDescriptor<T, F>, F extends FieldDescriptor<T, F>> {
	/**
	 * Gets a reflected type from a given class. The returned
	 * type represents a definition for serialization.
	 * @param theType the type to get serialization information on
	 * @return the serializable form of the type
	 */
	T getSerializedType( Class<?> theType );
	/**
	 * Gets, from the previously requested serializable type,
	 * the set of fields to serialize.
	 * @param theType the type to get serializable fields for
	 * @return the collection of serializable fields.
	 */
	Collection<F> getSerializedFields( T theType );
}
