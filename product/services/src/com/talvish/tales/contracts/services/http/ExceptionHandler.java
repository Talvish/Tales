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
package com.talvish.tales.contracts.services.http;

/**
 * This interface is used to convert an exception into a well known result.
 * @author jmolnar
 *
 * @param <E> the exception to map
 */
public interface ExceptionHandler< E extends Throwable > {
	/**
	 * Changes an exception into a result.
	 * @param theMethod theMethod attempting to be run
	 * @param theException the exception to map
	 * @return the result to return
	 */
	ResourceMethodResult toResult( ResourceMethod theMethod, E theException );
}
