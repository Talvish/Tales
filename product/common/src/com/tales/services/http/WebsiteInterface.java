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
package com.tales.services.http;

import java.util.Map;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.servlet.DefaultServlet;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.services.http.HttpContract;
import com.tales.contracts.services.http.HttpServletContract;
import com.tales.services.Service;

/**
 * This is a form of interface for managing a website.
 * @author Joseph Molnar
 *
 */
public class WebsiteInterface extends HttpInterfaceBase {

	/**
	 * Constructor taking parameters needed to start a website.
	 * @param theName the name given to the interface
	 * @param theService the service the interface will be bound to
	 * @param theResourceBase the location on disk to map where to find files from
	 * @param theService the service the interface will be bound to
	 */
	public WebsiteInterface( String theName, String theResourceBase, Service theService ) {
		super(theName, theService);
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theResourceBase ), String.format( "Website interface '%s'  needs a resource base.", theName ) );

		setupInterface( theName, theResourceBase, null );
	}

	/**
	 * Constructor taking parameters needed to start a website.
	 * @param theName the name given to the interface
	 * @param theEndpoints the endpoints exposed by this interface
	 * @param theResourceBase the location on disk to map where to find files from
	 * @param theInitParameters an optional set of parameters to configure the jsp servlet with
	 * @param theService the service the interface will be bound to
	 */
	public WebsiteInterface( String theName, String theResourceBase, Map<String,String> theInitParameters, Service theService ) {
		super(theName, theService);
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theResourceBase ), String.format( "Website interface '%s'  needs a resource base.", theName ) );

		setupInterface( theName, theResourceBase, theInitParameters );
	}
	
	/**
	 * Constructor helper method for setting up the default and jsp servlets.
	 * @param theResourceBase the location on disk to map where to find files from
	 * @param theCompilerPath the path to find the java compiler for compiling jsp files
	 */
	private final void setupInterface( String theName, String theResourceBase, Map<String,String> theInitParameters ) {
		// information on parameters can be found here: http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
		// setup our resources and base class loader
		this.getServletContext().setResourceBase( theResourceBase );
		this.getServletContext().setClassLoader( Thread.currentThread().getContextClassLoader());

		// first we setup the default contract/servlet for handling regular static images and files
        HttpContract defaultContract = new HttpServletContract( theName + "_default", "The default servlet for handling non-JSP files", new String[] { "20130201" }, new DefaultServlet(), "/" );
    	this.getContractManager( ).register( defaultContract );
		ContractServletHolder defaultHolder = new LaxContractServletHolder( defaultContract, this );
		this.getServletContext().addServlet( defaultHolder, "/" );

		// next we setup the jsp contract/servlet for handling jsp files
        HttpContract jspContract = new HttpServletContract( theName + "_jsp", "The jsp servlet for handling JSP files", new String[] { "20130201" }, new JspServlet(), "/" );
    	this.getContractManager( ).register( jspContract );
		ContractServletHolder jspHolder = new LaxContractServletHolder( jspContract, this );
		this.getServletContext().addServlet( jspHolder, "*.jsp" );

		// we need to set up the init parameters, and will
		// treat the class path specially, since we need it
		boolean foundClasspath = false;
		
		if( theInitParameters != null ) {
			for( String name: theInitParameters.keySet( ) ) {
				jspHolder.setInitParameter( name, theInitParameters.get( name ) );
				if( String.CASE_INSENSITIVE_ORDER.compare( name, "classpath" ) == 0 ) {
					foundClasspath = true;
				}
			}
		}
		// if we don't have a class path set then we need to set it
		if( !foundClasspath ) {
			jspHolder.setInitParameter( "classpath", this.getServletContext( ).getClassPath( ) );
		}
	}
}
