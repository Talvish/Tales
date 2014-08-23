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
package com.tales.services.http;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.services.http.HttpContract;
import com.tales.contracts.services.http.HttpResourceContract;
import com.tales.contracts.services.http.HttpServletContract;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceFacility;
import com.tales.contracts.services.http.ResourceType;
import com.tales.contracts.services.http.ServletContract;
import com.tales.services.KeySource;
import com.tales.services.Service;
import com.tales.services.http.servlets.ResourceServlet;

/**
 * This class represents a host/port that servlets can be bound to. 
 * @author jmolnar
 *
 */
public class HttpInterface extends HttpInterfaceBase {
	private static final Logger logger = LoggerFactory.getLogger( HttpInterfaceBase.class );
	
	/**
	 * Constructor taking the items needed for the interface to start.
	 * @param theName the name given to the interface
	 * @param theEndpoints the endpoints exposed by this interface
	 * @param theService the service the interface will be bound to
	 */
	public HttpInterface(String theName, Service theService) {
		super(theName, theService);
	}

	/**
     * Binds a servlet on a path to this particular interface.
     * @param theServlet the servlet being bound
     * @param theRoot the path to bind to
     */
    public void bind( Servlet theServlet, String theRoot ) {
    	Preconditions.checkNotNull( theServlet, "must provide a servlet" );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theRoot ), "need a path to bind to" );
    	Preconditions.checkArgument( theRoot.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theRoot );

    	// make sure we have a contract defined
        ServletContract contractAnnotation = theServlet.getClass().getAnnotation( ServletContract.class );
        Preconditions.checkState( contractAnnotation != null, "The object of type '%s' does not have a service contract defined.", theServlet.getClass().getName() );
        Preconditions.checkState( !Strings.isNullOrEmpty( contractAnnotation.name() ), "must have a contract name" );
		Preconditions.checkState( contractAnnotation.versions() != null && contractAnnotation.versions().length > 0, "must have at least one version" );
    	
    	// create and save the contract we can use it in the class below and for validation
        HttpContract contract = new HttpServletContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ),theServlet, theRoot  );

        // we save the contract for later validation
    	this.getContractManager( ).register( contract );
    	// and properly bind the servlet to the context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, this ), theRoot );
	}
    

    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theRoot the root path being bound to
     */
    public void bind( Object theResource, String theRoot ) {
    	// parameter validation is below
    	_bind( theResource, theRoot, null );
    }

    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theRoot the root path being bound to
     */
    public void bind( Object theResource, String theRoot, KeySource<HttpServletRequest> theKeySource ) {
    	// additional parameter validation is below
    	Preconditions.checkNotNull( theKeySource, "the key source cannot be null" );

    	_bind( theResource, theRoot, theKeySource );
    }
    
    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theRoot the root path being bound to
     */
    private void _bind( Object theResource, String theRoot, KeySource<HttpServletRequest> theKeySource ) {
    	Preconditions.checkNotNull( theResource, "must provide a resource object ");
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theRoot ), "need a path to bind to" );
    	Preconditions.checkArgument( theRoot.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theRoot );

    	// make sure we have a contract defined
    	ResourceContract contractAnnotation = theResource.getClass().getAnnotation( ResourceContract.class );
        Preconditions.checkState( contractAnnotation != null, "The object of type '%s' does not have a service contract defined.", theResource.getClass().getName() );
        Preconditions.checkState( !Strings.isNullOrEmpty( contractAnnotation.name() ), "must have a contract name" );
		Preconditions.checkState( contractAnnotation.versions() != null && contractAnnotation.versions().length > 0, "must have at least one version" );

    	// create a path that will ensure all methods on the object can run
    	String path = theRoot; 
    	if( path.endsWith( "/") ) {
    		path = path + "*";
    	} else if( !path.endsWith( "*" ) ) {
    		path = path + "/*";
    	} 

    	String fullPath = this.getServletContext().getContextPath();
    	if( fullPath.equals( "/" ) ) {
    		fullPath = "";
    	} else if( fullPath.endsWith( "/") ) {
    		fullPath.substring( 0,  fullPath.length() - 2 );
    	}
    	fullPath = fullPath + path;
		
    	// pull out the information from the object about what methods are being exposed
		ResourceFacility resourceFacility = this.getService( ).getFacility( ResourceFacility.class );
    	ResourceType resourceType = resourceFacility.generateResource( theResource, fullPath );
    	// we need to create the servlet we will run within
    	ResourceServlet servlet = new ResourceServlet( theResource, resourceType, resourceFacility, theKeySource );
    	
    	logger.info( "Binding resource '{}' on interface '{}' to full path '{}'.", contractAnnotation.name(), this.getName(), fullPath );
    	
    	// create the resource contract representing this
    	HttpContract contract = new HttpResourceContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ), theResource, servlet, fullPath, resourceType );
    	// register for later validation
    	this.getContractManager( ).register( contract );
    	// and now properly save the servlet to a context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, this ), path );
    }
}