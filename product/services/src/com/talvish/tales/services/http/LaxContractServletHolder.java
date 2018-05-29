// ***************************************************************************
// *  Copyright 2013 Joseph Molnar
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
package com.talvish.tales.services.http;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.talvish.tales.contracts.services.http.HttpContract;

/**
 * This is a servlet holder that has a contract
 * associated with a servlet but doesn't enforce
 * version.
 * @author jmolnar
 *
 */
public class LaxContractServletHolder extends ContractServletHolder {
	/**
	 * The constructor taking the contract and the servlet it is associated with.
	 * @param theContract the contract associated with the servlet
	 * @param theServlet the servlet the contract is to be bound to
	 * @param theInterface the interface the servlet is running on
	 */
	public LaxContractServletHolder( HttpContract theContract, Servlet theServlet, HttpInterfaceBase theInterface ) {
		super( theContract, theServlet, theInterface );
	}

	/**
	 * This method does not filter anything.
	 * @param theHttpRequest the request being responded to
	 * @param theHttpResponse the response we can write out to
	 * @param theVersion the version of the contract being requested, which may be null/empty
	 * @return alwayse returns false
	 */
	protected boolean filterContract( HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse, String theVersion ) {
		return false;
	}
}
