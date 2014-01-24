package com.tales.contracts.services.http;

import javax.servlet.Servlet;

/**
 * This is a contract that is exposed as a direct servlet.
 * In this case the bound object is the same as the bound servlet.
 * @author jmolnar
 *
 */
public class HttpServletContract extends HttpContract {

	/**
	 * The constructor for the servlet contract.
	 * @param theName the name of the contract
	 * @param theDescription the optional description of the contract
	 * @param theVersions the versions supported by the contract
	 * @param theBoundServlet the servlet being bound
	 * @param theBoundPath the path being bound to
	 */
	public HttpServletContract( String theName, String theDescription, String[] theVersions, Servlet theBoundServlet, String theBoundPath ) {
		super( theName, theDescription, theVersions, theBoundServlet, theBoundServlet, theBoundPath );
	}

}
