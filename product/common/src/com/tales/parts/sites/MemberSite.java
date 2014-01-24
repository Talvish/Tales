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

import java.lang.reflect.Type;

/**
 * {@link DataSite} extension that targets members of a class.
 * @author jmolnar
 *
 */
public interface MemberSite extends DataSite {
	/**
	 * The class that contains the member this site targets
	 * @return the class containing the member
	 */
	Class<?> getContainingType( );

	/**
	 * The generic type of the member data site, which is helpful
	 * for discovering generic parameters used by the data site.
	 * @return the generic type of the member site.
	 */
	Type getGenericType( );
	
	/**
	 * The name of the member this site targets.
	 * @return the name of the member
	 */
	String getName( );
}
