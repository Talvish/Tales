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

import java.util.Map;

import javax.servlet.Servlet;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.services.http.HttpContract;
import com.talvish.tales.contracts.services.http.HttpServletContract;
import com.talvish.tales.services.Service;
import com.talvish.tales.system.ExecutionLifecycleState;


/**
 * This is a form of interface for managing a website.
 * @author Joseph Molnar
 *
 */
public class WebsiteInterface extends HttpInterface {

	private static final Logger logger = LoggerFactory.getLogger( WebsiteInterface.class );
	private boolean didBind = false;

	/**
	 * Constructor taking parameters needed to start a website.
	 * @param theName the name given to the interface
	 * @param theService the service the interface will be bound to
	 */
	public WebsiteInterface( String theName, Service theService ) {
		super(theName, theService);
	}

//	public void bind( String theAreaName, String theDiskBase, String theWebBase ) {
//		bind( theAreaName, theDiskBase, theWebBase, null );
//	}
//	
//	public void bind( String theAreaName, String theDiskBase, String theWebBase, Map<String,String> theInitParameters ) {
//	// TODO: this isn't working because of how Jetty does binding, the constructor for the ServletContextHandler resets the Server's context handling,
//	//       instead of having it handle more than one (which it can in some capacity if the Server was able to have more than one) so it isn't possible
//	//		 to have more than one root, at least not in this way
//		Conditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind to an interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
//		Conditions.checkParameter( !Strings.isNullOrEmpty( theAreaName ), "Need to specify an name for binding a web location into interface '%s'.", this.getName( ) );
//		Conditions.checkParameter( !Strings.isNullOrEmpty( theDiskBase ), "Need to specify a disk base location for binding '%s' into interface '%s'.", theAreaName, this.getName( ) );
//		Conditions.checkParameter( !Strings.isNullOrEmpty( theWebBase ), "Need to specify a web base for binding '%s' into interface '%s'.", theAreaName, this.getName( ) );
//		
//		ServletContextHandler servletContext = null;
//				
//		// TODO: need to give the full http path
//		logger.info( "Binding web area '{}' on interface '{}' from path '{}' to http path '{}'.", theAreaName, this.getName(), theDiskBase, theWebBase );
//		
//		// we need to create a new context
//		// we need to setup the overall context
//		servletContext = new ServletContextHandler( this.getServer( ), theWebBase, false, false ); // set the context on the root; no sessions, security
//
//		// now we set the max form content size based on the connector definition from the main servlet
//		servletContext.setMaxFormContentSize( this.getServletContext().getMaxFormContentSize() );
//		
//		// then we set the resource base, which is where we serve from disk and we need a class loader set up
//		servletContext.setResourceBase( theDiskBase );
//		servletContext.setClassLoader( Thread.currentThread().getContextClassLoader());
//
//		// next we setup the default contract/servlet for handling regular static images and files
//        HttpContract defaultContract = new HttpServletContract( theAreaName + "_web_default", "The servlet for handling non-JSP files", new String[] { "20130201" }, new DefaultServlet(), theWebBase );
//    	this.getContractManager( ).register( defaultContract );
//		ContractServletHolder defaultHolder = new LaxContractServletHolder( defaultContract, this );
//		servletContext.addServlet( defaultHolder, theWebBase );
//
//		// next we setup the jsp contract/servlet for handling jsp files
//        HttpContract jspContract = new HttpServletContract( theAreaName + "_web_jsp", "The jsp servlet for handling JSP files", new String[] { "20130201" }, new JspServlet(), theWebBase );
//    	this.getContractManager( ).register( jspContract );
//		ContractServletHolder jspHolder = new LaxContractServletHolder( jspContract, this );
//		servletContext.addServlet( jspHolder, "*.jsp" );
//
//		
//		// we need to set up the init parameters, and will
//		// treat the class path specially, since we need it
//		boolean foundClasspath = false;
//		
//		if( theInitParameters != null ) {
//			for( String name: theInitParameters.keySet( ) ) {
//				jspHolder.setInitParameter( name, theInitParameters.get( name ) );
//				if( String.CASE_INSENSITIVE_ORDER.compare( name, "classpath" ) == 0 ) {
//					foundClasspath = true;
//				}
//			}
//		}
//		// if we don't have a class path set then we need to set it
//		if( !foundClasspath ) {
//			jspHolder.setInitParameter( "classpath", servletContext.getClassPath( ) );
//		}
//		
//		// save these for servlets to access
//		servletContext.setAttribute( AttributeConstants.INTERFACE_SERVLET_CONTEXT, this );
//		servletContext.setAttribute( AttributeConstants.SERVICE_SERVLET_CONTEXT, this.getService() );
//	}
	
	/**
	 * A bind method for setting up the web location to use for JSP servlets and static files.
	 * Only one bind can be performed at this time.
	 * @param theDiskBase the location on disk, relative to the starting directory, where to find files from
	 * @param theWebBase the location in the URL, relative to the service location, where the system will service from
	 */
	public void bind( String theDiskBase, Map<String,String> theInitParameters ) {
		Preconditions.checkState( !didBind, "Cannot bind a web location to interface '%s' because a location was already bound.", this.getName() );
    	Preconditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind a web location to interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theDiskBase ), String.format( "Website interface '%s' needs a disk location.", getName( ) ) );
		
		// information on parameters can be found here: http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
		// setup our resources and base class loader
		this.getServletContext().setResourceBase( theDiskBase );
		this.getServletContext().setClassLoader( Thread.currentThread().getContextClassLoader());

		logger.info( "Binding web location on interface '{}' from path '{}' to http path '{}'.", this.getName(), theDiskBase, this.getServletContext().getContextPath( ) );
		
		String webBase = "/";
		
		// first we setup the default contract/servlet for handling regular static images and files
		Servlet defaultServlet = new DefaultServlet();
        HttpContract defaultContract = new HttpServletContract( getName( ) + "_web_default", "The default servlet for handling non-JSP files", new String[] { "20130201" }, defaultServlet, webBase );
    	this.getContractManager( ).register( defaultContract );
		ContractServletHolder defaultHolder = new LaxContractServletHolder( defaultContract, defaultServlet, this );
		this.getServletContext().addServlet( defaultHolder, "/" );
		
    	
		// next we setup the jsp contract/servlet for handling jsp files
		JspServlet jspServlet = new JspServlet();
        HttpContract jspContract = new HttpServletContract( getName( ) + "_web_jsp", "The jsp servlet for handling JSP files", new String[] { "20130201" }, jspServlet, webBase );
    	this.getContractManager( ).register( jspContract );
		ContractServletHolder jspHolder = new LaxContractServletHolder( jspContract, jspServlet, this );
		this.getServletContext().addServlet( jspHolder, "*.jsp" ); // TODO: this should be relative somehow


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
