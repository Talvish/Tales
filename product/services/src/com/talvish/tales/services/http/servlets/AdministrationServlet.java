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
package com.talvish.tales.services.http.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.google.common.base.Preconditions;
import com.talvish.tales.services.Service;
import com.talvish.tales.services.http.AttributeConstants;

@SuppressWarnings("serial")
public abstract class AdministrationServlet extends HttpServlet {
    private Service service;

    /**
     * Override for initialization to ensure we have a service.
     */
    @Override
	public void init() throws ServletException {
		super.init();
    	service=  ( Service )this.getServletContext().getAttribute( AttributeConstants.SERVICE_SERVLET_CONTEXT );
    	Preconditions.checkState( getService() != null, "Must have a service to use administrative servlets." );
	 }

	/**
	 * Returns the HttpService this servlet helps administer.
	 * @return the service
	 */
	protected Service getService() {
		return service;
	}
}

