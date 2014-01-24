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
package com.tales.parts.sites;

/**
 * This is a simple interface that abstracts getting and setting values 
 * on a particular object.
 * @author jmolnar
 *
 */
public interface DataSite {
	/**
	 * The type of the data in this data site.
	 * @return class representing the type of data
	 */
	Class<?> getType( );

	/**
	 * Indicates if this particular data is read-only.
	 * @return
	 */
	boolean isReadOnly( );
	
	/**
	 * Gets data from a source object.
	 * @param theSource the source object to get data from.
	 * @return the value from the source
	 */
	Object getData( Object theSource );
	/**
	 * Sets data on a sink object.
	 * @param theSink the object to set a value on
	 * @param theValue the value to set on the sink
	 */
	void setData( Object theSink, Object theValue );
}
