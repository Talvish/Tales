// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.communication.HttpEndpoint;
import com.tales.contracts.services.ServiceContract;
import com.tales.contracts.services.ContractManager;
import com.tales.contracts.services.http.HttpContract;
import com.tales.contracts.services.http.HttpServletContract;
import com.tales.serialization.Readability;
import com.tales.services.Interface;
import com.tales.services.Service;
import com.tales.services.ConfigurationConstants;
import com.tales.services.OperationContext.Details;
import com.tales.services.http.servlets.DefaultServlet;
import com.tales.system.ExecutionLifecycleException;
import com.tales.system.ExecutionLifecycleListener;
import com.tales.system.ExecutionLifecycleListeners;
import com.tales.system.ExecutionLifecycleState;
import com.tales.system.configuration.ConfigurationException;
import com.tales.system.status.MonitorableStatusValue;
import com.tales.system.status.RatedLong;
import com.tales.system.status.StatusBlock;
import com.tales.system.status.StatusManager;

/**
 * This class represents a host/port that servlets can be bound to. 
 * @author jmolnar
 *
 */
public abstract class HttpInterfaceBase implements Interface {
	/**
	 * Stored information regarding the status of the interface.
	 * @author jmolnar
	 *
	 */
	public class Status {
		private DateTime startTime				= null;
		private DateTime stopTime				= null;
		private DateTime suspendTime			= null;
		private AtomicLong suspends				= new AtomicLong( 0 );
		private RatedLong suspendRate			= new RatedLong( );
		private AtomicLong badUrls				= new AtomicLong( 0 );
		private RatedLong badUrlRate			= new RatedLong( );
		
		/**
		 * Default empty constructor.
		 */
		public Status( ) {
		}

		/**
		 * Records the interface starting.
		 */
		public void recordStart( ) {
			startTime = new DateTime( DateTimeZone.UTC );
			stopTime = null;
		}

		/**
		 * Records the interface stopping.
		 */
		public void recordStop( ) {
			Preconditions.checkState( startTime != null, "Cannot record a start when a stop hasn't happend." );
			stopTime = new DateTime( DateTimeZone.UTC );
		}
		
		/**
		 * Records the interface being suspended.
		 */
		public void recordSuspended( ) {
			Preconditions.checkState( suspendTime == null, "Cannot record a suspend when a suspend has already happened." );
			suspendTime = new DateTime( DateTimeZone.UTC );
			suspends.incrementAndGet();
			suspendRate.increment();
		}
		
		/**
		 * Records the interface resuming.
		 */
		public void recordResumed( ) {
			Preconditions.checkState( suspendTime != null, "Cannot record a suspend when a suspend hasn't already happened." );
			suspendTime = null;
		}
		
		/**
		 * Returns that a bad URL has occurred.
		 */
		public void recordBadUrl( ) {
			badUrls.incrementAndGet();
			badUrlRate.increment();
		}

		/**
		 * Returns the current execution state of the interface.
		 * @return the execution state
		 */
		@MonitorableStatusValue( name = "state", description = "The current execution state of the interface." )
		public ExecutionLifecycleState getState( ) {
			return HttpInterfaceBase.this.lifecycleState;
		}
	
		/**
		 * Returns the start time that was recorded.
		 * @return the start time
		 */
		@MonitorableStatusValue( name = "start_running_datetime", description = "The date and time the interface started running." )
		public DateTime getStartTime( ) {
			return this.startTime;
		}
		
		/**
		 * Calculates the length of the time the interface has been running.
		 * @return the running time, or Period. ZERO if not currently running
		 */
		@MonitorableStatusValue( name = "elapsed_running_time", description = "The amount of time the interface has been running." )
		public Period calculateRunningTime( ) {
			if( stopTime == null  ) {
				return new Period( startTime, new DateTime( DateTimeZone.UTC ), PeriodType.standard( ) );
			} else {
				return Period.ZERO;
			}
		}
		/**
		 * Returns the stop time that was recorded.
		 * @return the stop time
		 */
		public DateTime getStopTime( ) {
			return this.stopTime;
		}
		
		/**
		 * Returns the time the interface was suspended, if currently suspended.
		 * @return the suspended time, or null if not currently suspended
		 */
		@MonitorableStatusValue( name = "start_suspend_datetime", description = "The date and time the interface was suspend." )
		public DateTime getSuspendTime( ) {
			return this.suspendTime;
		}
		
		/**
		 * Calculates the length of the time the interface has been suspended.
		 * @return the length of time suspended, or Period.ZERO if not suspended.
		 */
		@MonitorableStatusValue( name = "elapsed_suspend_time", description = "The amount of time the interface has been suspended." )
		public Period calculateSuspendTime( ) {
			if( suspendTime != null  ) {
				return new Period( suspendTime, new DateTime( DateTimeZone.UTC ), PeriodType.standard( ) );
			} else {
				return Period.ZERO;
			}
		}

		/**
		 * Returns the number of times the interface has been suspended.
		 * @return the total number of suspends
		 */
		@MonitorableStatusValue( name = "suspends", description = "The total number of times the interface has been suspended since the interface was started." )
		public long getSuspends( ) {
			return this.suspends.get( );
		}

		/**
		 * Returns the rate of suspends on the interface.
		 * @return the suspend ate
		 */
		@MonitorableStatusValue( name = "suspend_rate", description = "The rate, in seconds, of the number of suspends as measured over 10 seconds." )
		public double getSuspendRate( ) {
			return this.suspendRate.calculateRate();
		}

		/**
		 * Returns the number of bad url requests on the interface.
		 * @return the number of bad url requests
		 */
		@MonitorableStatusValue( name = "bad_urls", description = "The total number of times the interface has processed bad url requests since the interface was started." )
		public long getBadUrls( ) {
			return this.badUrls.get();
		}
		
		/**
		 * Returns the rate of the number of bad url requests on the interface.
		 * @return the rate of the number of bad url requests
		 */
		@MonitorableStatusValue( name = "bad_url_rate", description = "The rate, in seconds, of the number of bad urls processed as measured over 10 seconds." )
		public double getBadUrlRate( ) {
			return this.badUrlRate.calculateRate();
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger( HttpInterfaceBase.class );

	private final SslContextFactory sslFactory;
	
	private final Service service;
	private final String name;
	private final Collection<HttpEndpoint> endpoints; 
	private final HttpServletServer server;
	private final ServletContextHandler servletContext;
	private final ContractManager contractManager;

	private final Status status = new Status( );
	private final StatusManager statusManager = new StatusManager( );
	
	private ExecutionLifecycleListeners  listeners 	= new ExecutionLifecycleListeners( );
	private ExecutionLifecycleState lifecycleState	= ExecutionLifecycleState.CREATED;

	// TODO: add a constructor that takes the parameters manually instead of loaded from the configuration
	
	/**
	 * Constructor taking the items needed for the interface to start.
	 * @param theName the name given to the interface
	 * @param theService the service the interface will be bound to
	 */
	public HttpInterfaceBase( String theName, Service theService ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "must have a name" );
		Preconditions.checkNotNull( theService, "need a service" );
		
		service = theService;
		name = theName;
		server = new HttpServletServer( this );
		
		// so we need to see if we have SSL settings for this interface
		String sslKeyStoreConfigName = String.format( ConfigurationConstants.HTTP_INTERFACE_SSL_KEY_STORE, theName );
		String sslCertAliasConfigName = String.format( ConfigurationConstants.HTTP_INTERFACE_SSL_CERT_ALIAS, theName );
		// if we have a key store defined on the  service and if so create the ssl factory
		if( service.getConfigurationManager().contains( sslKeyStoreConfigName ) ) {
			String keyStoreName = service.getConfigurationManager().getStringValue( sslKeyStoreConfigName ) ;
			String certAlias = service.getConfigurationManager().getStringValue( sslCertAliasConfigName, "" );
			try {
				KeyStore keyStore = service.getKeyStoreManager().getKeyStore( keyStoreName );
				
				if( keyStore == null ) {
					throw new ConfigurationException( String.format( "Interface '%s' is attempting to use a non-existent key store called '%s'.", theName, keyStoreName ) );
				} else {
					sslFactory = new SslContextFactory();
					sslFactory.setKeyStore( keyStore );
					// if we have the cert alias available, then we use

					
					if( !Strings.isNullOrEmpty( certAlias ) ) {
						if( !keyStore.containsAlias( certAlias ) ) {
							throw new ConfigurationException( String.format( "Interface '%s' is attempting to use a non-existent certificate alias '%s' on key store '%s'.", theName, certAlias, keyStoreName ) );
						} else {
							sslFactory.setCertAlias( certAlias );
						}
					}
					// oddly we need to grab the key again, even though the store is open
					// I'm not very happy with having to do this, but Jetty needs the password again
					sslFactory.setKeyStorePassword( service.getConfigurationManager().getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_PASSWORD_FORMAT, keyStoreName ) ) );
				}
			} catch( KeyStoreException e ) {
				throw new IllegalStateException( String.format( "Interface '%s' is using an invalid key store called '%s'.", theName, keyStoreName ) );
			}
		} else {
			sslFactory = null;
		}

		ConnectorConfiguration connectorConfiguration = null;

    	// load up the the connector configuration
    	String connectorName = String.format( ConfigurationConstants.HTTP_INTERFACE_CONNECTOR, this.getName( ) );
		if( service.getConfigurationManager().contains( connectorName ) ) {
	    	ConnectorConfigurationManager connectorConfigurationManager = this.service.getFacility( ConnectorConfigurationManager.class );
	    	String connectorConfigurationName = service.getConfigurationManager().getStringValue( connectorName );
		    connectorConfiguration = connectorConfigurationManager.getConfiguration( connectorConfigurationName );
	    	if( connectorConfiguration == null ) {
	    		throw new IllegalArgumentException( String.format( "Could not find connector configuration '%s' for interface '%s'.", connectorConfigurationName, this.name ) );
	    	} else {
	    		logger.info( "Interface '{}' is using connector configuration '{}'.", this.name, connectorConfigurationName );
	    	}
		} else {
			connectorConfiguration = new ConnectorConfiguration( );
    		logger.info( "Interface '{}' is using default connector configuration.", this.name );
		}

		// load up the endpoints from the configuration and set them on the service
		List<String> endPoints = service.getConfigurationManager( ).getListValue( String.format( ConfigurationConstants.HTTP_INTERFACE_ENDPOINTS, theName), String.class );
		Preconditions.checkState( endPoints != null && endPoints.size() > 0, String.format( "HttpInterface '%s' does not have any endpoints defined.",  theName ) );

		int count = 0;
		HttpEndpoint endpoint;
		ArrayList<HttpEndpoint> modifiableEndpoints = new ArrayList<HttpEndpoint>( endPoints.size() );
		
		for( String stringEndpoint : endPoints ) {
			endpoint = new HttpEndpoint( stringEndpoint, false );
			if( endpoint.getScheme().equals( "https") ) {
				if( sslFactory == null ) {
					throw new IllegalArgumentException( String.format( "The http interface '%s' is attempting to use SSL on endpoint '%s', but SSL is not configured for this interface.", this.name, endpoint.toString( ) ) );
				}
				addSecureConnector( String.format( "%s%03d", theName, count ), endpoint, connectorConfiguration  );
			} else {
				addNonSecureConnector( String.format( "%s%03d", theName, count ), endpoint, connectorConfiguration  );
			}
			modifiableEndpoints.add( endpoint );
			count += 1;
		}
		endpoints = Collections.unmodifiableCollection( modifiableEndpoints );

		// we need to setup the overall context
		servletContext = new ServletContextHandler( server, "/", false, false ); // set the context on the root; no sessions, security

		// now we set the max form content size based on the connector definition
		if( connectorConfiguration != null && connectorConfiguration.getMaxFormContentSize() != null ) {
			servletContext.setMaxFormContentSize( connectorConfiguration.getMaxFormContentSize( ) );
			server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", connectorConfiguration.getMaxFormContentSize());
			logger.info( "Interface '{}' is set to use the max form content size of '{}'.", this.name, servletContext.getMaxFormContentSize() );
			
		} else {
    		logger.info( "Interface '{}' is set to use the default max form content size of '{}'.", this.name, servletContext.getMaxFormContentSize( ) );
		}
		
		// save these for servlets to access
		servletContext.setAttribute( AttributeConstants.INTERFACE_SERVLET_CONTEXT, this );
		servletContext.setAttribute( AttributeConstants.SERVICE_SERVLET_CONTEXT, service );

		// make sure we have a contract manager
		contractManager = new ContractManager();
		
		// get the status blocks setup
		statusManager.register( "http_interface", status );
	}
	
	/**
	 * Gets the name given to the interface.
	 * @return the name of the interface
	 */
	public final String getName( ) {
		return name;
	}
	
	/**
	 * Returns the endpoints exposed by the interface.
	 * @return the endpoints exposed by the interface
	 */
	public final Collection<HttpEndpoint> getEndpoints( ) {
		return this.endpoints;
	}
	
	/**
	 * Returns the servlet context backing this interface.
	 * @return the servlet context
	 */
	protected final ServletContextHandler getServletContext( ) {
		return this.servletContext;
	}
	
	/**
	 * Returns the contract manager used by the interface.
	 * @return the underlying contract manager
	 */
	protected final ContractManager getContractManager( ) {
		return this.contractManager;
	}
	
	/**
	 * Returns the service this interface is part of.
	 * @return the service this interface is part of.
	 */
	public final Service getService( ) {
		return this.service;
	}
	
	/**
	 * Returns the underlying Jetty server managing the servlets
	 * @return the underlying Jetty server
	 */
	protected final HttpServletServer getServer( ) {
		return this.server;
	}
	/**
	 * Returns the status information for the interface.
	 * @return
	 */
	public final Status getStatus( ) {
		return this.status;
	}

	
	
	/**
	 * Returns the set of status blocks for the interface
	 * @return the interface status blocks
	 */
	public final Collection<StatusBlock> getStatusBlocks( ) {
		return this.statusManager.getStatusBlocks();
	}
	
	/**
	 * Returns the contracts bound to the interface.
	 * @return the contracts bound to the interface
	 */
	public final Collection<ServiceContract> getBoundContracts( ) {
		return this.contractManager.getContracts();
	}
	
	/**
	 * Sets the default level used for showing details in responses.
	 * @param theDetails the new default level
	 */
	public void setDefaultResponseDetails( Details theDetails ) {
		this.server.setDefaultResponseDetails( theDetails );
	}

	/**
	 * Sets the default target for readability in responses.
	 * @param theReadability the new default target
	 */
	public void setDefaultResponseReadability( Readability theReadability ) {
		this.server.setDefaultResponseReadability( theReadability );
	}

	/**
	 * Adds an object interested in getting execution state updates.
	 * @param theListener the listener to add
	 */
	public void addListener( ExecutionLifecycleListener theListener ) {
		listeners.addListener( theListener );
	}
	
	/**
	 * Removes an object that was once interested in getting execution state updates.
	 * @param theListener the listener to remove
	 */
	public void removeListener( ExecutionLifecycleListener theListener ) {
		listeners.removeListener( theListener );
	}

	
	/**
	 * Returns the current lifecycle state of the interface.
	 * @return the current lifecycle state
	 */
	public ExecutionLifecycleState getState( ) {
		return this.lifecycleState;
	}
	
	/**
	 * Starts the interface.
	 * @throws Exception
	 */
	public void start( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED, "Cannot start an interface when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STARTING;
		logger.info( "Starting interface '{}' on {}.", this.name, endpointListHelper( ) );
		this.listeners.onStarting( this, this.lifecycleState );
		try {
			// we see if we have a servlet covering the 'defaults' (items not explicitly mapped out)
			ServletMapping mapping = this.getServletContext().getServletHandler().getServletMapping( "/" ); 
			if( mapping == null ) {
				// if we dont' have a default handler, we set one up to handle the 404
		        HttpContract defaultContract = new HttpServletContract( this.name + "_default", "Default, error throwing, servlet.", new String[] { "20130201" }, new DefaultServlet(), "/" );
		    	this.getContractManager( ).register( defaultContract );
				ContractServletHolder defaultHolder = new LaxContractServletHolder( defaultContract, this );
				this.getServletContext().addServlet( defaultHolder, "/" );
				logger.info( "Default servlet handling for interface '{}' under context '{}' is handled by the default Tales servlet.", this.getName(), this.servletContext.getContextPath( ) );
			} else {
				logger.info( "Default servlet handling for interface '{}' under context '{}' is handled by '{}'.", this.getName(), this.servletContext.getContextPath( ), mapping.getServletName() );
			}

			server.start();
		} catch( Exception e ) {
			throw new ExecutionLifecycleException( "Unable to start the underlying server.", e );
		}
		this.lifecycleState = ExecutionLifecycleState.STARTED;
		status.recordStart();
		this.listeners.onStarted( this, this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Stops the interface.
	 * @throws Exception
	 */
	public void stop( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED || this.lifecycleState == ExecutionLifecycleState.RUNNING || this.lifecycleState == ExecutionLifecycleState.SUSPENDED, "Cannot stop an interface when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STOPPING;
		logger.info( "Stopping interface '{}' on {}.", this.name, endpointListHelper( ) );
		this.listeners.onStopping( this, this.lifecycleState );
		try {
			server.stop( );
			server.join( ); // wait for it to stop
		} catch( Exception e ) {
			throw new ExecutionLifecycleException( "Unable to stop or join the underlying server.", e );
		}
		this.lifecycleState = ExecutionLifecycleState.STOPPED;
		status.recordStop();
		this.listeners.onStopped( this, this.lifecycleState );
	}
	
	/**
	 * Simple helper method to list out the endpoints.
	 * @return a string listing the endpoints.
	 */
	private String endpointListHelper( ) {
		StringBuilder listBuilder = new StringBuilder();
		boolean wroteOne = false;
		
		for( HttpEndpoint endpoint : endpoints ) {
			if( wroteOne ) {
				listBuilder.append( ", " );
			}
			listBuilder.append( "'" );
			listBuilder.append( endpoint.toString() );
			listBuilder.append( "'" );
			wroteOne = true;
		}
		return listBuilder.toString( );
	}
	
	/**
	 * This is call to suspend an interface, which means requests to
	 * contracts on this interface will return a Failure.LOCAL_UNAVAILABLE.
	 * This will not pause any operations in progress.
	 */
	public void suspend( ) {
		suspend( null );
	}
	
	/**
	 * This is call to suspend an interface, which means requests to
	 * contracts on this interface will return a Failure.LOCAL_UNAVAILABLE.
	 * This will not pause any operations in progress.
	 * The parameter it takes is the length of time we will report that
	 * the suspend will be running for. This does not mean the suspend
	 * will automatically resume, it is just a notification to callers.
	 * @param theLength the length,in seconds, of how long the delay is expected
	 */
	private void suspend( Integer theLength ) {
		Preconditions.checkState( canSuspend( ), "Cannot suspend an interface when the status is '%s'.", this.lifecycleState );
		Preconditions.checkArgument( theLength == null || theLength >= 0, "The delay length cannot be negative." );
		this.lifecycleState = ExecutionLifecycleState.SUSPENDED;
		//this.suspendLength =  theLength;
		status.recordSuspended();
		this.listeners.onSuspended( this, this.lifecycleState );
	}
	
	/**
	 * This is called to resume a previously suspended interface.
	 */
	public void resume( ) {
		Preconditions.checkState( canResume( ), "Cannot resume an interface when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		status.recordResumed();
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Helper method that indicates if the interface is in a state that will
	 * allow suspending.
	 * @return return true if suspendable, false otherwise
	 */
	public boolean canSuspend( ) {
		return this.lifecycleState == ExecutionLifecycleState.RUNNING;
	}
	
	/**
	 * Helper method that indicates if the interface is suspended so that
	 * it can be resumed.
	 * @return return true if resumable, false otherwise
	 */
	public boolean canResume( ) {
		return this.lifecycleState == ExecutionLifecycleState.SUSPENDED;
	}

	/**
	 * Binds a filter into the interface along the specified path.
	 * @param theFilter the filter being bound
	 * @param theRoot the path the filter is being bound to
	 */
	public void bind( Filter theFilter, String theRoot ) {
    	Preconditions.checkNotNull( theFilter, "must provide a filter" );
    	Preconditions.checkArgument( !Strings.isNullOrEmpty( theRoot ), "need a path to bind to" );
    	Preconditions.checkArgument( theRoot.startsWith( "/" ), "the path '%s' must be a reference from the root (i.e. start with '/')", theRoot );

    	logger.info( "Binding filter '{}' on interface '{}' to http path '{}'.", theFilter.getClass().getSimpleName(), this.getName(), theRoot );

    	String path = theRoot; 
    	if( path.endsWith( "/") ) {
    		path = path + "*";
    	} else if( !path.endsWith( "*" ) ) {
    		path = path + "/*";
    	} 

    	// and properly bind the filter to the context
    	servletContext.addFilter( new FilterHolder( theFilter ), path, EnumSet.allOf( DispatcherType.class ) );
	}

	/**
	 * This method is called to setup the non-secure connectors needed.
	 * @param theConnectorName the name to give the connector
	 * @param theEndpoint the endpoint to bind to
	 */
    private void addNonSecureConnector( String theConnectorName, HttpEndpoint theEndpoint, ConnectorConfiguration theConfiguration ) {
    	// here is how to get setup
    	// http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java

    	// let's setup our jetty http configuration
    	HttpConfiguration httpConfiguration = generateJettyHttpConfiguration( theConfiguration );
    	
    	// now we create our connector
    	ServerConnector connector = new ServerConnector( 
    			this.server, 
    			null, // use the server's executor
    			null, // use the server's scheduler
    			null, // use a default bye pool with default configuration
    			theConfiguration.getAcceptors() == null ? -1 : theConfiguration.getAcceptors( ), 
    			theConfiguration.getSelectors() == null ? -1 : theConfiguration.getSelectors( ),  
    			new HttpConnectionFactory( httpConfiguration ) );
    	
    	if( theConfiguration.getAcceptQueueSize() != null ) {
    		connector.setAcceptQueueSize( theConfiguration.getAcceptQueueSize( ) );
    	}
    	if( theConfiguration.getIdleTimeout( ) != null ) {
    		connector.setIdleTimeout( theConfiguration.getIdleTimeout( ) );
    	}

    	// if we have a host, set it so we bind to a particular interface
    	if( !theEndpoint.getHost( ).equals( "*" ) ) {
    		connector.setHost( theEndpoint.getHost( ) );
    	}
    	// now setup the port and name
    	connector.setPort( theEndpoint.getPort() );
    	connector.setName( theConnectorName );

    	// now we add the connector to the server
    	server.addConnector( connector );

    	// display our configuration for the connector
		displayConnectorConfiguration( connector, theEndpoint, httpConfiguration, theConfiguration );
    }

	/**
	 * This method is called to setup the secure connectors needed.
	 * @param theConnectorName the name to give the connector
	 * @param theEndpoint the end point to bind to
	 */
    private void addSecureConnector( String theConnectorName, HttpEndpoint theEndpoint, ConnectorConfiguration theConfiguration ) {
    	// here is how to get setup
    	// http://wiki.eclipse.org/Jetty/Howto/Configure_SSL (older version)
    	// http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java

    	// let's setup our jetty http configuration
    	HttpConfiguration httpConfiguration = generateJettyHttpConfiguration( theConfiguration );
    	
    	// still need to setup the default security items
    	httpConfiguration.setSecureScheme( "https");
    	httpConfiguration.setSecurePort( theEndpoint.getPort( ) );
    	httpConfiguration.addCustomizer( new SecureRequestCustomizer( ) );

    	// now we create our connector
    	ServerConnector connector = new ServerConnector( 
    			this.server, 
    			null, // use the server's executor
    			null, // use the server's scheduler
    			null, // use a default bye pool with default configuration
    			theConfiguration.getAcceptors() == null ? -1 : theConfiguration.getAcceptors( ), 
    			theConfiguration.getSelectors() == null ? -1 : theConfiguration.getSelectors( ),  
    			new SslConnectionFactory( this.sslFactory,  "http/1.1" ),
    			new HttpConnectionFactory( httpConfiguration ) );
    	
    	if( theConfiguration.getAcceptQueueSize() != null ) {
    		connector.setAcceptQueueSize( theConfiguration.getAcceptQueueSize( ) );
    	}
    	if( theConfiguration.getIdleTimeout( ) != null ) {
    		connector.setIdleTimeout( theConfiguration.getIdleTimeout( ) );
    	}

    	// if we have a host, set it so we bind to a particular interface
    	if( !theEndpoint.getHost( ).equals( "*" ) ) {
    		connector.setHost( theEndpoint.getHost( ) );
    	}
    	// now setup the port and name
    	connector.setPort( theEndpoint.getPort() );
    	connector.setName( theConnectorName );

    	// now we add the connector to the server
    	server.addConnector( connector );

    	// display our configuration for the connector
		displayConnectorConfiguration( connector, theEndpoint, httpConfiguration, theConfiguration );
    }
    
    private HttpConfiguration generateJettyHttpConfiguration( ConnectorConfiguration theConfiguration ) {
    	HttpConfiguration httpConfiguration = new HttpConfiguration();
    	
    	if( theConfiguration.getHeaderCacheSize( ) != null ) {
    		httpConfiguration.setHeaderCacheSize( theConfiguration.getHeaderCacheSize( ) );
    	}
    	if( theConfiguration.getRequestHeaderSize( ) != null ) {
    		httpConfiguration.setRequestHeaderSize( theConfiguration.getRequestHeaderSize( ) );
    	}
    	if( theConfiguration.getResponseHeaderSize( ) != null ) {
    		httpConfiguration.setResponseHeaderSize( theConfiguration.getResponseHeaderSize( ) );
    	}
    	if( theConfiguration.getOutputBufferSize( ) != null ) {
    		httpConfiguration.setOutputBufferSize( theConfiguration.getOutputBufferSize( ) );
    	}
    	httpConfiguration.setSendDateHeader( false ); // not sure what this does exactly
    	httpConfiguration.setSendServerVersion( false );
    	httpConfiguration.setSendXPoweredBy( false );

    	return httpConfiguration;
    }
    

    /**
     * Helper method that sets the connector configuration options on the specific connector.
     * @param theConnector the connector to setup
     * @param theEndpoint the endpoint that was configured
     * @param theConfigurationName the name of the set of configuration values to use to setup
     */
    private void displayConnectorConfiguration( ServerConnector theConnector, HttpEndpoint theEndpoint, HttpConfiguration theHttpConfiguration, ConnectorConfiguration theConfiguration ) {
    	Preconditions.checkNotNull( theConnector, "need a jetty connector to apply settings to" );
    	Preconditions.checkNotNull( theHttpConfiguration, "need jetty http configuration if you are going to apply it" );
    	Preconditions.checkNotNull( theConfiguration, "need configuration if you are going to apply it" );
    	
    	StringBuffer settingBuffer = new StringBuffer();

    	if( theConfiguration.getAcceptors() != null ) {
    		settingBuffer.append( "\n\tAcceptors: " );
    	} else {
    		settingBuffer.append( "\n\tAcceptors (default): " );
    	}
		settingBuffer.append( theConnector.getAcceptors( ) );

		if( theConfiguration.getAcceptQueueSize() != null ) {
    		settingBuffer.append( "\n\tAccept Queue Size: " );
    	} else {
    		settingBuffer.append( "\n\tAccept Queue Size (default): " );
    	}
		settingBuffer.append( theConnector.getAcceptQueueSize( ) );

    	if( theConfiguration.getSelectors() != null ) {
    		settingBuffer.append( "\n\tSelectors: " );
    	} else {
    		settingBuffer.append( "\n\tSelectors (default): " );
    	}
		settingBuffer.append( theConnector.getSelectorManager().getSelectorCount() );

    	if( theConfiguration.getIdleTimeout() != null ) {
    		settingBuffer.append( "\n\tIdle Time: " );
    	} else {
    		settingBuffer.append( "\n\tIdle Time (default): " );
    	}
		settingBuffer.append( theConnector.getIdleTimeout( ) );


    	if( theConfiguration.getHeaderCacheSize() != null ) {
    		settingBuffer.append( "\n\tHeader Cache Size: " );
    	} else {
    		settingBuffer.append( "\n\tHeader Cache Size (default): " );
    	}
		settingBuffer.append( theHttpConfiguration.getHeaderCacheSize( ) );


		if( theConfiguration.getRequestHeaderSize() != null ) {
    		settingBuffer.append( "\n\tRequest Header Size: " );
    	} else {
    		settingBuffer.append( "\n\tRequest Header Size (default): " );
    	}
		settingBuffer.append( theHttpConfiguration.getRequestHeaderSize( ) );

    	if( theConfiguration.getResponseHeaderSize() != null ) {
    		settingBuffer.append( "\n\tResponse Header Size: " );
    	} else {
    		settingBuffer.append( "\n\tResponse Header Size (default): " );
    	}
		settingBuffer.append( theHttpConfiguration.getResponseHeaderSize( ) );

    	if( theConfiguration.getOutputBufferSize() != null ) {
    		settingBuffer.append( "\n\tOutput Buffer Size: " );
    	} else {
    		settingBuffer.append( "\n\tOutput Buffer Size (default): " );
    	}
		settingBuffer.append( theHttpConfiguration.getOutputBufferSize( ) );


		settingBuffer.append( "\n\tReuse Address (default): " );
		settingBuffer.append( theConnector.getReuseAddress( ) );

		settingBuffer.append( "\n\tSocket Linger Time (default): " );
		settingBuffer.append( theConnector.getSoLingerTime( ) );

    	logger.info( "Interface '{}' on endpoint '{}' is using configuration: {}", this.getName(), theEndpoint.toString(), settingBuffer.toString() );
    }
}
