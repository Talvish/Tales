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
package com.talvish.tales.services.http;

import java.util.concurrent.Executor;

import javax.servlet.Servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.services.http.HttpContract;
import com.talvish.tales.contracts.services.http.HttpResourceContract;
import com.talvish.tales.contracts.services.http.HttpServletContract;
import com.talvish.tales.contracts.services.http.ResourceContract;
import com.talvish.tales.contracts.services.http.ResourceFacility;
import com.talvish.tales.contracts.services.http.ResourceType;
import com.talvish.tales.contracts.services.http.ServletContract;
import com.talvish.tales.services.Service;
import com.talvish.tales.services.http.servlets.ResourceQueryServlet;
import com.talvish.tales.system.ExecutionLifecycleState;
import com.talvish.tales.system.ExecutorManager;

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
     * @param theBoundPath the path to bind to
     */
    public void bind( Servlet theServlet, String theBoundPath ) {
    	Preconditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind to servlet to interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
    	
    	Preconditions.checkNotNull( theServlet, "must provide a servlet" );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theBoundPath ), "need a path to bind to" );
    	Preconditions.checkArgument( theBoundPath.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theBoundPath );

    	// make sure we have a contract defined
        ServletContract contractAnnotation = theServlet.getClass().getAnnotation( ServletContract.class );
        Preconditions.checkState( contractAnnotation != null, "The object of type '%s' does not have a service contract defined.", theServlet.getClass().getName() );
        Preconditions.checkState( !Strings.isNullOrEmpty( contractAnnotation.name() ), "must have a contract name" );
		Preconditions.checkState( contractAnnotation.versions() != null && contractAnnotation.versions().length > 0, "must have at least one version" );

    	logger.info( "Binding servlet '{}' on interface '{}' to http path '{}'.", contractAnnotation.name(), this.getName(), theBoundPath );

    	// create and save the contract we can use it in the class below and for validation
        HttpContract contract = new HttpServletContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ),theServlet, theBoundPath  );

        // we save the contract for later validation
    	this.getContractManager( ).register( contract );
    	// and properly bind the servlet to the context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, theServlet, this ), theBoundPath );
	}
    

    /**
     * Binds a resource object to a root path on this interface and will use 
     * the default configuration.
     * @param theResource the resource being bound
     * @param theBoundPath the root path being bound to
     */
    public void bind( Object theResource, String theBoundPath ) {
    	// parameter validation is below
    	_bind( theResource, theBoundPath, null );
    }

    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theBoundPath the root path being bound to
     * @param theConfiguration the configuration to use as part of the resource binding
     */
    public void bind( Object theResource, String theBoundPath, ResourceConfiguration theConfiguration ) {
    	// parameter validation is below
    	_bind( theResource, theBoundPath, theConfiguration );
    }
    
    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theBoundPath the root path being bound to
     * @param theConfiguration the configuration to use for the resource
     */
    private void _bind( Object theResource, String theBoundPath, ResourceConfiguration theConfiguration ) {
    	Preconditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind a resource to interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
    	Preconditions.checkNotNull( theResource, "must provide a resource object ");
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theBoundPath ), "need a path to bind to" );
    	Preconditions.checkArgument( theBoundPath.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theBoundPath );

    	// make sure we have a contract defined
    	ResourceContract contractAnnotation = theResource.getClass().getAnnotation( ResourceContract.class );
        Preconditions.checkState( contractAnnotation != null, "The object of type '%s' does not have a service contract defined.", theResource.getClass().getName() );
        Preconditions.checkState( !Strings.isNullOrEmpty( contractAnnotation.name() ), "must have a contract name" );
		Preconditions.checkState( contractAnnotation.versions() != null && contractAnnotation.versions().length > 0, "must have at least one version" );
		
		if( theConfiguration == null ) {
			theConfiguration = ResourceConfiguration.DEFAULT_CONFIGURATION;
		}

    	// create a path that will ensure all methods on the object can run
    	String path = theBoundPath; 
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
    	
    	// we now need to setup the resource servlet which means getting the thread pool necessary 
    	// make sure we have the executor and the timeout
    	String executorName = theConfiguration.getThreadPoolName() == null ? ThreadingConstants.DEFAULT_THREAD_POOL : theConfiguration.getThreadPoolName( );
    	long executionTimeout = theConfiguration.getExecutionTimeout() == null ? ThreadingConstants.DEFAULT_KEEP_ALIVE_TIME :  theConfiguration.getExecutionTimeout();

    	// now get the executor to use
    	ExecutorManager executorManager = this.getService().getExecutorManager();
    	Executor executor = executorManager.getExecutor( executorName );
    	Preconditions.checkState( executor != null, "Unable to bind resource '%s' since the executor '%s' has not been registered.", resourceType.getName( ), theConfiguration.getThreadPoolName( ) );

    	StringBuilder configurationBuilder = new StringBuilder();
//    	configurationBuilder.append( "\n\tThread Prefix: " );
//    	configurationBuilder.append( prefix );
//    	configurationBuilder.append( "\n\tThread Priority: " );
//    	configurationBuilder.append( priority );
//    	configurationBuilder.append( "\n\tCore Threads: " );
//    	configurationBuilder.append( coreThreads );
//    	configurationBuilder.append( "\n\tMax Threads: " );
//    	configurationBuilder.append( maxThreads );
//    	configurationBuilder.append( "\n\tKeep Alive Time: " );
//    	configurationBuilder.append( keepAliveTime );
//    	configurationBuilder.append( "\n\tPrestart Core: " );
//    	configurationBuilder.append( prestartCore );
    	if( theConfiguration.getExecutionTimeout( ) == null || theConfiguration == ResourceConfiguration.DEFAULT_CONFIGURATION ) {
        	configurationBuilder.append( "\n\tThread Pool Name (default): " );
    	} else {
        	configurationBuilder.append( "\n\tThread Pool Name: " );
    	}
    	configurationBuilder.append( executorName );
    	
    	if( theConfiguration.getExecutionTimeout( ) == null || theConfiguration == ResourceConfiguration.DEFAULT_CONFIGURATION ) {
    		configurationBuilder.append( "\n\tExecution Time-out (default): " );
    	} else {
    		configurationBuilder.append( "\n\tExecution Time-out: " );
    	}
    	configurationBuilder.append( executionTimeout );

    	// so now we need to create the servlet we will run within
    	ResourceQueryServlet servlet = new ResourceQueryServlet( 
    			theResource, 
    			resourceType, 
    			resourceFacility, 
    			executor, 
    			executionTimeout );
    	
    	logger.info( "Binding resource '{}' on interface '{}' to http path '{}' using configuration: {}", contractAnnotation.name(), this.getName(), fullPath, configurationBuilder.toString( ) );
    	
    	// create the resource contract representing this
    	HttpContract contract = new HttpResourceContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ), theResource, fullPath, resourceType );
    	// register for later validation
    	this.getContractManager( ).register( contract );
    	// and now properly save the servlet to a context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, servlet, this ), path );
    }
}