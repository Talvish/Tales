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
package com.tales.services;

import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.naming.NameValidator;
import com.tales.parts.naming.NopNameValidator;
import com.tales.parts.naming.SegmentedLowercaseEntityNameValidator;

/**
 * A utility class used to validate names read in from external sources or exposed externally.
 * @author jmolnar
 *
 */
public final class NameManager {
	private static final NameValidator resourceMethodParameterNameValidator = new LowerCaseEntityNameValidator();;
	
	private static final NameValidator statusBlockNameValidator = resourceMethodParameterNameValidator;
	private static final NameValidator statusValueNameValidator = resourceMethodParameterNameValidator; // TODO: we could force the name to include 'rate', 'count', 'average', 'state', etc.

	private static final NameValidator serviceNameValidator = resourceMethodParameterNameValidator;
	
	private static final NameValidator contractNameValidator = new SegmentedLowercaseEntityNameValidator( );
	
	private static final NameValidator configurationNameValidator = contractNameValidator;
	
	private static final NameValidator nopNameValidator = new NopNameValidator();
	
	private static boolean enabled = true;

	
	/**
	 * Indicates if the name manager is enabled.
	 * @return true if enabled, false otherwise
	 */
	public static boolean isEnabled( ) {
		return enabled;
	}
	
//	/**
//	 * Sets whether or not the name manager is enabled.
//	 * @param shouldEnable true to enable, false to disable
//	 */
//	public static void setEnabled( boolean shouldEnable ) {
//		enabled = shouldEnable;
//	}
	
	/**
	 * The name validator used for the service names.
	 * @return the validator
	 */
	public static NameValidator getServiceNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return serviceNameValidator;
		}
	}

	/**
	 * The name validator used for the contract names.
	 * @return the validator
	 */
	public static NameValidator getContractNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return contractNameValidator;
		}
	}

	/**
	 * The name validator used for the configuration names.
	 * @return the validator
	 */
	public static NameValidator getConfigurationNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return configurationNameValidator;
		}
	}

	/**
	 * The name validator used for the status names, include block names and value names..
	 * @return the validator
	 */
	public static NameValidator getStatusBlockNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return statusBlockNameValidator;
		}
	}

	/**
	 * The name validator used for the status value names.
	 * @return the validator
	 */
	public static NameValidator getStatusValueNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return statusValueNameValidator;
		}
	}

	
	/**
	 * The name validator used for post body/query string parameter names names.
	 * @return the validator
	 */
	public static NameValidator getResourceMethodNameValidator( ) {
		if( !enabled ) {
			return nopNameValidator;
		} else {
			return resourceMethodParameterNameValidator;
		}
	}
}
