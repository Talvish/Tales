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
package com.talvish.tales.services.http.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.talvish.tales.communication.Status;
import com.talvish.tales.contracts.services.http.ServletContract;
import com.talvish.tales.services.http.AttributeConstants;
import com.talvish.tales.services.http.FailureSubcodes;
import com.talvish.tales.services.http.HttpInterfaceBase;
import com.talvish.tales.services.http.ResponseHelper;


/**
 * Default servlet handling which simply fails with a 404.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.default", versions="20111005")
@SuppressWarnings("serial")
public class DefaultServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger( ControlServlet.class );

    private HttpInterfaceBase httpInterface;
    
    /**
     * Empty, default constructor.
     */
    public DefaultServlet( ) {
    }
    
    /**
     * Override for initialization to ensure we have a service.
     */
    @Override
	public void init() throws ServletException {
		super.init();
		httpInterface = ( HttpInterfaceBase )this.getServletContext().getAttribute( AttributeConstants.INTERFACE_SERVLET_CONTEXT );
    	Preconditions.checkState( httpInterface != null, "Must be bound to an interface." );
	 }
    
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Implementation of the post method to request closing the service..
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doDelete(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doHead( HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doOptions(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doPut(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Implementation that simply throws a 404.
	 */
	@Override
	protected void doTrace(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Private, shared, implementation that throws the 404 and records the failure.
	 */ 
	private void doCall( HttpServletRequest theRequest, HttpServletResponse theResponse ) {
		String requestPath = theRequest.getRequestURL().toString();
				
		logger.info( "Path '{}' is not mapped to a servlet or resource.", requestPath );
		ResponseHelper.writeFailure(theRequest, theResponse, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path '%s' is not mapped to a servlet or resource.", requestPath ), null );
		httpInterface.recordBadUrl();
	}
}
