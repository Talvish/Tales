// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.auth.accesscontrol;

import java.lang.reflect.Method;

/**
 * This is a helper interface used by developers looking to create
 * their own claim verifiers that have corresponding annotations. 
 * The intention is that implementations of this method will analyze 
 * the method looking for annotations and then register what it 
 * finds onto the verifier. As a single method interface, the 
 * intention is to use Java lambdas to define the implementation.
 * @author jmolnar
 *
 */
public interface AnnotationHandler {
	/**
	 * Implementations will analyze the method looking for annotations
	 * and then register verifiers in the token verifier.
	 * @param theMethod the method to analyze for annotations
	 * @param theDescriptor the method descriptor that will be registered into
	 * @param theManager the manager that the handler was registered into
	 */
	void analyze( Method theMethod, MethodAccessDescriptor theDescriptor, @SuppressWarnings("rawtypes") AccessControlManager theManager );
}
