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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;

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
import com.tales.services.Service;
import com.tales.services.http.servlets.ResourceServlet;
import com.tales.system.ConfigurableThreadFactory;
import com.tales.system.ExecutionLifecycleState;
import com.tales.system.configuration.ConfigurationManager;

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
    	Preconditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind to servlet to interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
    	
    	Preconditions.checkNotNull( theServlet, "must provide a servlet" );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theRoot ), "need a path to bind to" );
    	Preconditions.checkArgument( theRoot.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theRoot );

    	// make sure we have a contract defined
        ServletContract contractAnnotation = theServlet.getClass().getAnnotation( ServletContract.class );
        Preconditions.checkState( contractAnnotation != null, "The object of type '%s' does not have a service contract defined.", theServlet.getClass().getName() );
        Preconditions.checkState( !Strings.isNullOrEmpty( contractAnnotation.name() ), "must have a contract name" );
		Preconditions.checkState( contractAnnotation.versions() != null && contractAnnotation.versions().length > 0, "must have at least one version" );

    	logger.info( "Binding servlet '{}' on interface '{}' to http path '{}'.", contractAnnotation.name(), this.getName(), theRoot );

    	// create and save the contract we can use it in the class below and for validation
        HttpContract contract = new HttpServletContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ),theServlet, theRoot  );

        // we save the contract for later validation
    	this.getContractManager( ).register( contract );
    	// and properly bind the servlet to the context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, theServlet, this ), theRoot );
	}
    

    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theRoot the root path being bound to
     */
    public void bind( Object theResource, String theRoot ) {
    	// parameter validation is below
    	_bind( theResource, theRoot );
    }

    /**
     * Binds a resource object to a root path on this interface.
     * @param theResource the resource being bound
     * @param theRoot the root path being bound to
     */
    private void _bind( Object theResource, String theRoot ) {
    	Preconditions.checkState( this.getState() == ExecutionLifecycleState.CREATED || this.getState() == ExecutionLifecycleState.STARTING, "Cannot bind a resource to interface '%s' while it is in the '%s' state.", this.getName(), this.getState( ) );
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
    	
    	// we now need to setup the resource servlet which means getting the 
    	// thread pool necessary 
    	ConfigurationManager configurationManager = this.getService().getConfigurationManager();
    	
    	// TODO: this should all be configurable and potentially
    	//       sharable across all resources, you can imagine
    	//       some form of thread pool manager that
    	//       resources can opt to use
    	int processors = Runtime.getRuntime().availableProcessors( );
    	int coreThreads = configurationManager.getIntegerValue( ThreadPoolConstants.RESOURCE_POOL_CORE_SIZE, processors );
    	int maxThreads = configurationManager.getIntegerValue( ThreadPoolConstants.RESOURCE_POOL_MAX_SIZE, coreThreads * 4 );
    	long keepAliveTime = configurationManager.getLongValue( ThreadPoolConstants.RESOURCE_POOL_KEEP_ALIVE_TIME, 60000l );
    	boolean prestartCore = configurationManager.getBooleanValue( ThreadPoolConstants.RESOURCE_POOL_PRESTART_CORE, false );

    	String prefix = configurationManager.getStringValue( ThreadPoolConstants.RESOURCE_POOL_THREAD_NAME, "rtp" );
    	int priority = configurationManager.getIntegerValue( ThreadPoolConstants.RESOURCE_POOL_THREAD_PRIORITY, Thread.NORM_PRIORITY );

    	long executionTimeout = configurationManager.getLongValue( ThreadPoolConstants.RESOURCE_EXECUTION_TIMEOUT, 10000l );
    	
    	ThreadPoolExecutor executor = new ThreadPoolExecutor(
    			coreThreads, 
    			maxThreads, 
    			keepAliveTime,
                TimeUnit.MILLISECONDS, 
                new ArrayBlockingQueue<Runnable>( maxThreads ),
                new ConfigurableThreadFactory( prefix, priority, false ) );
    	
    	if( prestartCore ) {
    		executor.prestartAllCoreThreads();
    	}
    	StringBuilder executorBuilder = new StringBuilder();
    	executorBuilder.append( "Resource '" );
    	executorBuilder.append( resourceType.getName( ) );
    	executorBuilder.append( "' on interface '" );
    	executorBuilder.append( this.getName( ) );
    	executorBuilder.append( "' is using non-blocking execution configuration:" );
    	executorBuilder.append( "\n\tThread Prefix: " );
    	executorBuilder.append( prefix );
    	executorBuilder.append( "\n\tThread Priority: " );
    	executorBuilder.append( priority );
    	executorBuilder.append( "\n\tExecution Time-out: " );
    	executorBuilder.append( executionTimeout );
    	executorBuilder.append( "\n\tCore Threads: " );
    	executorBuilder.append( coreThreads );
    	executorBuilder.append( "\n\tMax Threads: " );
    	executorBuilder.append( maxThreads );
    	executorBuilder.append( "\n\tKeep Alive Time: " );
    	executorBuilder.append( keepAliveTime );
    	executorBuilder.append( "\n\tPrestart Core: " );
    	executorBuilder.append( prestartCore );
    	logger.info( executorBuilder.toString( ) );

    	// also need an execution timeout
    	
    	// TODO: reconsider the default for blocking/non-blocking
    	
		// TODO: Looking to add async and manged load:
		//       https://webtide.com/servlet-3-1-async-io-and-jetty/
		//       http://www.eclipse.org/jetty/documentation/current/high-load.html

    	
    	// we need to create the servlet we will run within
    	ResourceServlet servlet = new ResourceServlet( theResource, resourceType, resourceFacility, executor, executionTimeout );
    	
    	logger.info( "Binding resource '{}' on interface '{}' to http path '{}'.", contractAnnotation.name(), this.getName(), fullPath );
    	
    	// create the resource contract representing this
    	HttpContract contract = new HttpResourceContract( contractAnnotation.name( ), contractAnnotation.description( ),contractAnnotation.versions( ), theResource, fullPath, resourceType );
    	// register for later validation
    	this.getContractManager( ).register( contract );
    	// and now properly save the servlet to a context
    	this.getServletContext( ).addServlet( new StrictContractServletHolder( contract, servlet, this ), path );
    }
}