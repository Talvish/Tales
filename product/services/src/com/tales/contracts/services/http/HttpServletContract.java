package com.tales.contracts.services.http;

import javax.servlet.Servlet;

import com.google.common.base.Preconditions;

/**
 * This is a contract that is exposed as a direct servlet.
 * In this case the bound object is the same as the bound servlet.
 * @author jmolnar
 *
 */
public class HttpServletContract extends HttpContract {
	private final Servlet boundServlet;

	/**
	 * The constructor for the servlet contract.
	 * @param theName the name of the contract
	 * @param theDescription the optional description of the contract
	 * @param theVersions the versions supported by the contract
	 * @param theBoundServlet the servlet being bound
	 * @param theBoundPath the path being bound to
	 */
	public HttpServletContract( String theName, String theDescription, String[] theVersions, Servlet theBoundServlet, String theBoundPath ) {
		super( theName, theDescription, theVersions, theBoundServlet, theBoundPath );
		Preconditions.checkNotNull( theBoundServlet, "cannot create servlet contract for '%s' becuase a servlet was not give", theName );
		boundServlet = theBoundServlet;
	}
	
	/**
	 * Returns the servlet this contract is bound to.
	 * @return the bound servlet
	 */
	public Servlet getBoundServlet( ) {
		return boundServlet;
	}
}
