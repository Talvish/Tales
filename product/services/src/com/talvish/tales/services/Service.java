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
package com.talvish.tales.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContractManager;
import com.talvish.tales.contracts.data.DataContractTypeSource;
import com.talvish.tales.contracts.services.http.ResourceFacility;
import com.talvish.tales.parts.naming.LowerCaseEntityNameValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.serialization.Readability;
import com.talvish.tales.serialization.json.JsonTranslationFacility;
import com.talvish.tales.services.http.ConnectorConfiguration;
import com.talvish.tales.services.http.ConnectorConfigurationManager;
import com.talvish.tales.services.http.HttpInterface;
import com.talvish.tales.services.http.HttpInterfaceBase;
import com.talvish.tales.services.http.ThreadingConstants;
import com.talvish.tales.services.http.servlets.AlertsServlet;
import com.talvish.tales.services.http.servlets.ConfigurationServlet;
import com.talvish.tales.services.http.servlets.ContractsServlet;
import com.talvish.tales.services.http.servlets.ControlServlet;
import com.talvish.tales.services.http.servlets.StatusServlet;
import com.talvish.tales.system.ConfigurableThreadFactory;
import com.talvish.tales.system.ExecutionLifecycleListener;
import com.talvish.tales.system.ExecutionLifecycleListeners;
import com.talvish.tales.system.ExecutionLifecycleState;
import com.talvish.tales.system.ExecutorManager;
import com.talvish.tales.system.Facility;
import com.talvish.tales.system.FacilityManager;
import com.talvish.tales.system.SimpleFacilityManager;
import com.talvish.tales.system.configuration.ConfigurationException;
import com.talvish.tales.system.configuration.ConfigurationManager;
import com.talvish.tales.system.status.MonitorableStatusValue;
import com.talvish.tales.system.status.RatedLong;
import com.talvish.tales.system.status.StatusManager;

/**
 * This is a base class for all services. It provides basic abilities 
 * managing contract information, build, health information, etc.
 * @author jmolnar
 *
 */
/*
 * TODO: 
 * 	- filter system parameters out and properly replaces items they say they override 
 * 	- make it so logging auto-adds the request id (even when in an engine)
 * 	- make it so calls going out send the proper headers (re-using operation contexts would be nice, if you could bind on thread pools)
 *  - support stream responses (though not as important as fixed length responses)
 *  - consider an interface servlet
 *  - have the info-header (query param?) used  to indicate what information to show or not show
 *    which could impact the data in logs and in administrative contracts
 *  - there can be administrative servlets for specific services as well (like changing watermark settings in Facebook queuing)
 */
public abstract class Service implements Runnable {
	public class Status {
		// NOTE: consider status for memory usage, thread count, cpu usage, if available
		// http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/MemoryUsage.html
		// http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/ThreadMXBean.html

		private DateTime startTime				= null;
		private DateTime stopTime				= null;
		
		private AtomicLong unhandledExceptions	= new AtomicLong( 0 );
		private RatedLong unhandledExceptionRate = new RatedLong( );

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
		 * Returns the current execution state of the service.
		 * @return the execution state
		 */
		@MonitorableStatusValue( name = "state", description = "The current execution state of the service." )
		public ExecutionLifecycleState getState( ) {
			return Service.this.lifecycleState;
		}
		
		/**
		 * Records an unhandled exception occurring/
		 */
		public void recordUnhandledException( ) {
			unhandledExceptions.incrementAndGet();
			unhandledExceptionRate.increment();
		}

		/**
		 * Returns the number of unhandled exceptions that have occurred.
		 * since the service was operational.
		 * @return the number of unhandled exceptions
		 */
		@MonitorableStatusValue( name = "unhandled_errors", description = "The total number of unhandled errors since the service was started." )
		public long getUnhandledExceptions( ) {
			return unhandledExceptions.get();
		}
		
		/**
		 * Returns the rate of the number of unhandled exceptions that are occurring
		 * @return the current rate of unhandled exceptions
		 */
		@MonitorableStatusValue( name = "unhandled_error_rate", description = "The rate, in seconds, of the number of unhandled errors as measured over 10 seconds." )
		public double getUnhandledExceptionRate( ) {
			return unhandledExceptionRate.calculateRate();
		}
		
		/**
		 * Returns the start time that was recorded.
		 * @return the start time
		 */
		@MonitorableStatusValue( name = "start_running_datetime", description = "The date and time the service started running." )
		public DateTime getStartTime( ) {
			return this.startTime;
		}
		
		/**
		 * Calculates the length of the time the interface has been running.
		 * @return the running time, or Period. ZERO if not currently running
		 */
		@MonitorableStatusValue( name = "elapsed_running_time", description = "The amount of time the service has been running." )
		public Period calculateRunningTime( ) {
			if( stopTime == null  ) {
				return new Period( startTime, new DateTime( DateTimeZone.UTC ), PeriodType.standard( ) );
			} else {
				return Period.ZERO;
			}
		}
	}
	
	public static final String SERVICE_NAME_VALIDATOR = "tales.services.service_name";
	
	static {
		if( !NameManager.hasValidator( Service.SERVICE_NAME_VALIDATOR ) ) {
			NameManager.setValidator( Service.SERVICE_NAME_VALIDATOR, new LowerCaseEntityNameValidator( ) );
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger( Service.class );

	private final String canonicalName;
	private final String friendlyName;
	private final String description;
	private final String userAgent;
	
	private final Object shutdownLock  = new Object( );
		
	protected final Status status = new Status( );
	protected final StatusManager statusManager = new StatusManager();
	protected final InterfaceManager interfaceManager = new InterfaceManager( );

	protected final FacilityManager facilityManager = new SimpleFacilityManager( );

	private final ExecutionLifecycleListeners listeners = new ExecutionLifecycleListeners( );
	private ExecutionLifecycleState lifecycleState	= ExecutionLifecycleState.CREATED;
	
	private final PeriodFormatter timeFormatter = new PeriodFormatterBuilder()
    .appendYears()
    .appendSuffix( " year", " years" )
    .appendSeparator( ", ", " and ")
    .appendMonths()
    .appendSuffix( " month", " months" )
    .appendSeparator( ", ", " and ")
    .appendDays()
    .appendSuffix(" day", " days")
    .appendSeparator( ", ", " and ")
    .appendHours()
    .appendSuffix(" hour", " hours")
    .appendSeparator( ", ", " and ")
    .appendMinutes()
    .appendSuffix(" minute", " minutes")
    .appendSeparator( ", ", " and ")
    .appendSeconds()
    .appendSuffix(" second", " seconds")
    .appendSeparator( ", ", " and ")
    .appendMillis()
    .appendSuffix(" millisecond", " milliseconds")
    .toFormatter();
	
	/**
	 * Constructor taking the name of the service.
	 * @param theName the name of the service
	 * @param theFriendlyName a visual name for the service
	 * @param theDescription a description of the service
	 */
	protected Service( String theCanonicalName, String theFriendlyName, String theDescription ) {
		NameValidator nameValidator = NameManager.getValidator( Service.SERVICE_NAME_VALIDATOR );
		
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theCanonicalName ) );	
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFriendlyName ) );
		Preconditions.checkArgument( nameValidator.isValid( theCanonicalName ), String.format( "Canonical service name '%s' does not conform to validator '%s'.", theCanonicalName, nameValidator.getClass().getSimpleName() ) );
		
		canonicalName = theCanonicalName;
		friendlyName = theFriendlyName;
		description = theDescription;
		
		userAgent = prepareUserAgent( canonicalName );
		
		// store the status manager's blocks
		statusManager.register( "service", status );
	}
	
	private final String prepareUserAgent( String theServiceName ) {
		// this is loosely based on RFC 2616 and defacto values for user agents
		//
		// User-Agent       = "User-Agent" ":" 1*( product | comment )
		// product          = token ["/" product-version]
	    // product-version  = token
		// token            = 1*<any CHAR except CTLs or separators>
	    // separators       = "(" | ")" | "<" | ">" | "@"
	    //                  | "," | ";" | ":" | "\" | <">
	    //                  | "/" | "[" | "]" | "?" | "="
	    //                  | "{" | "}" | SP | HT
		//
		// format will be: canonical_service/version ( os/version; arch) tales/version
	    // also need to set the system wide property: System.setProperty("http.agent", ""); 

		StringBuffer buffer = new StringBuffer( );
		String manifestName = getManifestName( );
		
		buffer.append( theServiceName );
		// we try to get the tales version from the manifest
		buffer.append( "/" );
		buffer.append( filterUserAgentValue( getServiceVersion( manifestName ) ) );
		buffer.append( " (" );
		buffer.append( filterUserAgentValue( System.getProperty( "os.name" ) ) );
		buffer.append( "/" );
		buffer.append( filterUserAgentValue( System.getProperty( "os.version" ) ) );
		buffer.append( "; " );
		buffer.append( filterUserAgentValue( System.getProperty( "os.arch" ) ) );
		buffer.append( ") " );
		buffer.append( "tales");
		buffer.append( "/" );
		buffer.append( filterUserAgentValue( getTalesVersion( manifestName ) ) );
		
		return buffer.toString( );
	}
	
	/**
	 * Helper method that gets the string representing the version of the tales framework.
	 * It returns the value associated with the 'Tales-Version' string from the primary 
	 * manifest file.
	 * @param theManifestName the name to use for manifest file resource loading
	 * @return the string for the version or 'unknown' if not found
	 */
	private final String getTalesVersion( String theManifestName ) {
		String value = null;
		
		try {
			Manifest manifest = getClassManifest( Service.class, theManifestName );
			Attributes manifestAttributes = manifest.getMainAttributes( );
			value = manifestAttributes.getValue( "Tales-Version" );
		} catch( Exception e ) {
			// we purposefully absorb
		}
		return Strings.isNullOrEmpty( value ) ? "unknown" : value;
	}

	/**
	 * Helper method that gets the string representing the version of the service.
	 * It returns the value associated with the 'Service-Version' string from the primary 
	 * manifest file.
	 * @param theManifestName the name to use for manifest file resource loading
	 * @return the string for the version or 'unknown' if not found
	 */
	private final String getServiceVersion( String theManifestName ) {
		String value = null;
		
		try {			
			Manifest manifest = new Manifest( Service.class.getResourceAsStream( "/" + theManifestName ) );
			Attributes manifestAttributes = manifest.getMainAttributes( );
			value = manifestAttributes.getValue( "Service-Version" );
		} catch( Exception e ) {
			// we purposefully absorb
		}
		return Strings.isNullOrEmpty( value ) ? "unknown" : value;
	}
	
	/**
	 * Simple helper method that will make sure we get the manifest name 
	 * that will be used to load resources. It ensures there is no 
	 * leading "/".
	 * @return the manifest name to use to get resources
	 */
	private final String getManifestName( ) {
		String manifestName = JarFile.MANIFEST_NAME;
		
		if( manifestName.startsWith( "/" ) ) {
			return manifestName.substring( 1 );
		} else {
			return manifestName;
		}
	}

	/**
	 * Helper method, that given a particular class, file find the actual
	 * manifest for the jar file that the class was part of.
	 * @param theClass the class to find
	 * @param theManifestPath the manifest name to use as the resource to load
	 * @return the manifest or null if not found / available
	 */
	public final Manifest getClassManifest( Class<?> theClass, String theManifestName ) {
		Manifest manifest = null;
		InputStream manifestStream = null;
		
		try {
			// first we need to figure out the jar file that class was found in
			// which is basically using the full package name of the class and converting to a resource path 
			String classPath = theClass.getName( ).replace( ".", "/" ) + ".class";
			// the converting that to a resource URL reference
			URL classUrl = theClass.getClassLoader().getResource( classPath );
			if( classUrl != null ) {
				String classUrlString = classUrl.toString();
				// then we need to strip off some of the resource URL quirks
				if( classUrlString.startsWith( "jar:" ) ) {
					int separatorIndex = classUrlString.lastIndexOf( '!' );
					if( separatorIndex > 0 ) {
						// and finally we then use that reference from the class to put the manifest name
						// as the resource we are looking to get
						String manifestUrlString = classUrlString.substring( 0, separatorIndex + 2 ) + theManifestName;
						URL manifestUrl = new URL( manifestUrlString );
						// and then we have our manifest file to load
						manifestStream = manifestUrl.openStream( );
						manifest = new Manifest( manifestStream ); 
					}
				}
			}
		} catch( Exception e ) {
			// absorb, since doesn't matter
		} finally {
			if( manifestStream != null ) {
				try {
					manifestStream.close();
				} catch( Exception e ) {
					// absorb, since doesn't matter
				}
			}
		
		}
		return manifest;
	}
	
	private static String NON_TOKEN_CHARS  = "[\\(\\)\\<\\>\\@\\,\\;\\:\\\\\\\"\\/\\[\\]\\?\\=\\{\\}\\x00-\\x1f\\x7f]";
	private static Pattern NON_TOKEN_REGEX = Pattern.compile( NON_TOKEN_CHARS );
	
	/**
	 * Takes the value that was given and ensures it is a valid
	 * token value as outlined in RFC 2616.
	 * @param theValue
	 * @return
	 */
	private String filterUserAgentValue( String theValue ) {
		// token            = 1*<any CHAR except CTLs or separators>
	    // separators       = "(" | ")" | "<" | ">" | "@"
	    //                  | "," | ";" | ":" | "\" | <">
	    //                  | "/" | "[" | "]" | "?" | "="
	    //                  | "{" | "}" | SP | HT
	    // CTL              = <any US-ASCII control character
        //                 (octets 0 - 31) and DEL (127)>
		Matcher matcher = NON_TOKEN_REGEX.matcher( theValue );
		return matcher.replaceAll( "" );
	}
	
	/**
	 * Returns the canonical name of the service.
	 * @return the canonical name of the service
	 */
	public String getCanonicalName( ) {
		return this.canonicalName;
	}

	/**
	 * Returns the friendly name of the service.
	 * @return the friendly name of the service
	 */
	public String getFriendlyName( ) {
		return this.friendlyName;
	}

	/**
	 * Returns the description of the service.
	 * @return the description of the service
	 */
	public String getDescription( ) {
		return this.description;
	}

	/**
	 * Returns the user agent being used by the service.
	 * @return the user agent string being used by the service
	 */
	public String getUserAgent( ) {
		return this.userAgent;
	}
	
	/**
	 * Returns the status information for the service.
	 * @return the service specific status information
	 */
	public Status getStatus( ) {
		return this.status;
	}
	
	/**
	 * Returns the status manager used by the service.
	 * @return the status manager
	 */
	public StatusManager getStatusManager( ) {
		return this.statusManager;
	}
	
	/**
	 * Returns the interface manager used by the service.
	 * @return the interface manager
	 */
	public InterfaceManager getInterfaceManager( ) {
		return this.interfaceManager;
	}

	/**
	 * Convenience method to get the admin interface.
	 * @return the admin interface
	 */
	public HttpInterfaceBase getAdminInterface( ) {
		return ( HttpInterfaceBase )this.interfaceManager.getInterface( "admin" );
	}
	
	/**
	 * Returns the configuration manager used by the service.
	 * The configuration manager is used to get retrieve
	 * configuration.
	 * @return the configuration manager used by the service
	 */
	public ConfigurationManager getConfigurationManager( ) {
		return this.getFacility( ConfigurationManager.class );
	}
	
	/**
	 * Returns the key store manager used by the service.
	 * The key store manager is used to get key stores that
	 * are used to facilitate SSL connections and other 
	 * encryption needs.
	 * 
	 * @return the key store manager
	 */
	public KeyStoreManager getKeyStoreManager( ) {
		return this.getFacility( KeyStoreManager.class );
	}
	
	/**
	 * Returns the executor manager used by the service.
	 * This manages thread pools and overall execution
	 * services used by things like resources.
	 * @return the executor manager
	 */
	public ExecutorManager getExecutorManager( ) {
		return this.getFacility( ExecutorManager.class );
	}

	/**
	 * Convenience method for getting the JSON translation facility.
	 * @return the JSON translation facility
	 */
	public JsonTranslationFacility getJsonTranslationFacility( ) {
		return this.facilityManager.getFacility( JsonTranslationFacility.class );
	}

	/**
	 * Convenience method for getting the resource facility.
	 * @return the resource facility
	 */
	public ResourceFacility getResourceFacility( ) {
		return this.facilityManager.getFacility( ResourceFacility.class );
	}
	
	/**
	 * Gets all the facilities supported by the manager.
	 * @return the collection of facilities
	 */
	public Collection<Facility> getFacilities() {
		return this.facilityManager.getFacilities();
	}

	/**
	 * Gets a particular facility.
	 * @param theFacilityType the type of facility to get
	 * @return the facility or null if not available
	 */
	public <F extends Facility> F getFacility(Class<F> theFacilityType) {
		return this.facilityManager.getFacility( theFacilityType );
	}

	/**
	 * Adds a particular facility to the manager. Only one instance 
	 * of a facility is available per type. This is available to subclasses.
	 * @param theFacilityType the type to reference the facility by
	 * @param theFacilityInstance the instance of the facility to add
	 */
	protected <F extends Facility> void addFacility(Class<F> theFacilityType, F theFacilityInstance) {
		this.facilityManager.addFacility(theFacilityType, theFacilityInstance);
	}

	/**
	 * Removes a particular facility from the manager.
	 * This is available to subclasses.
	 * @param theFacilityType the facility to remove, as referenced by the type.
	 * @return true if the facility was found and removed, false otherwise
	 */
	protected <F extends Facility> boolean removeFacility(Class<F> theFacilityType) {
		return this.facilityManager.removeFacility( theFacilityType );
	}
	
	/**
	 * Adds an object interested in getting lifecycle state updates.
	 * @param theListener the listener to add
	 */
	public void addListener( ExecutionLifecycleListener theListener ) {
		listeners.addListener( theListener );
	}
	
	/**
	 * Removes an object that was once interested in getting lifecycle state updates.
	 * @param theListener the listener to remove
	 */
	public void removeListener( ExecutionLifecycleListener theListener ) {
		listeners.removeListener( theListener );
	}
	
	/**
	 * Method called to start up the service. Subclasses 
	 * cannot override this, but should override the 
	 * onStart method.
	 * @param theArgs the arguments passed in from the main method
	 */
	public final void start( ConfigurationManager theConfigurationManager ) {
		try {
			Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED, "Cannot start the service when the status is '%s'.", this.lifecycleState );
			Preconditions.checkNotNull( theConfigurationManager, "A configuration manager must be provided by the service host." );
			
			this.lifecycleState = ExecutionLifecycleState.STARTING;
			logger.info( "Starting service '{}' (of type '{}').", canonicalName, this.getClass().getName( ) );
			listeners.onStarting( this, this.lifecycleState );
			
			// ensure we get uncaught exceptions and log them
			Thread.setDefaultUncaughtExceptionHandler( new UncaughtExceptionHandler() {
				public void uncaughtException(Thread theThread, Throwable theException ) {
					// give a shot to have the unhandled exception looked at
					handleUnhandledException( theThread, theException );
				}
			});
			
			// now we setup a bunch of facilities

			// first, we add the configuration facility, and make sure configuration is setup 
			this.facilityManager.addFacility( ConfigurationManager.class, theConfigurationManager );
			// now let subclasses do any additional configuration setup since it may be 
			// required (or nice for overrides) for facilities about to be added
			onInitializeConfiguration();
			
			// now add the json facility (used by servlets, admin, etc)
			JsonTranslationFacility jsonFacility = new JsonTranslationFacility( new DataContractTypeSource( ) );
			this.facilityManager.addFacility( JsonTranslationFacility.class, jsonFacility);
			
			// add the resource facility (used by our servlets/pieces for admin, but others can as well)
			ResourceFacility resourceFacility = new ResourceFacility( jsonFacility );
			this.facilityManager.addFacility( ResourceFacility.class, resourceFacility );

			// we now load up some re-usable items 
			// commonly used through-out tales including...

			// loading key stores (used for SSL or encryption)
			loadKeyStores( );
			// loading connector settings (for interfaces, particularly http interfaces)
			loadConnectorConfigurations( );
			// thread pools (commonly used for async resource execution)
			loadThreadPools( );
			
			// now we setup one interface that must exist, admin interface
	        HttpInterface adminInterface = new HttpInterface( "admin", this );
	        this.interfaceManager.register( adminInterface );
	        
	        // these are the base admin servlets we need
	        adminInterface.bind( new ControlServlet( ), "/service/control/*");
	        adminInterface.bind( new ConfigurationServlet( ), "/service/configuration");
	        adminInterface.bind( new ContractsServlet( ), "/service/contracts");
	        adminInterface.bind( new StatusServlet( ), "/service/status");
	        adminInterface.bind( new AlertsServlet( ), "/service/alerts");
	        
	        // now we look to see if any interfaces were defined and if so, we create and register them
	        List<String> interfaces = theConfigurationManager.getListValue( ConfigurationConstants.INTERFACES, String.class, null );
	        
	        if( interfaces != null ) {
		        String interfaceType	= null;
		        Class<?> interfaceClass	= null;
		        Constructor<?> interfaceConstructor = null;
		        Interface interfaceInstance;
		        
		        ClassLoader classLoader = Service.class.getClassLoader();
		        
		        for( String interfaceName : interfaces ) {
			        try {
			        	// we need to get the type
			        	interfaceType = theConfigurationManager.getStringValue( String.format( ConfigurationConstants.INTERFACE_TYPE, interfaceName ) );
			        	// now we load the type
			        	interfaceClass = classLoader.loadClass( interfaceType );
			        	Preconditions.checkState( Interface.class.isAssignableFrom( interfaceClass ), "Failed to setup interface '%s' since class '%s' does not implement Interface.", interfaceName, interfaceType );
			        	
			        	// and then get the constructor we expected
			        	interfaceConstructor = interfaceClass.getConstructor( String.class, Service.class );
			        	// create the interface
			        	interfaceInstance = ( Interface )interfaceConstructor.newInstance( interfaceName, this );
			        	// and finally register
			        	this.interfaceManager.register( interfaceInstance );
			        	
			        } catch( ClassNotFoundException e ) {
			        	throw new ConfigurationException( String.format( "Failed to setup interface '%s' since class '%s' could not be found.", interfaceName, interfaceType ), e );
			        } catch( NoSuchMethodException e ) {
			        	throw new ConfigurationException( String.format( "Failed to setup interface '%s' since class '%s' is missing a constructor taking two parameters, a String (for the interface name) and a Service.", interfaceName, interfaceType ), e );
			        } catch( IllegalAccessException | SecurityException e ) {
			        	throw new ConfigurationException( String.format( "Failed to setup interface '%s' using class '%s' due to a security exception.", interfaceName, interfaceType ), e );
			        } catch( IllegalArgumentException | InstantiationException | InvocationTargetException e ) {
			        	throw new ConfigurationException( String.format( "Failed to setup interface '%s' using class '%s' due to an exception.", interfaceName, interfaceType ), e );
			        }
		        }
			}
	        
			// now let subclasses override, we expect
	        // initialization and registration
			onStart( );
			
			// now start the interfaces that were registered
			logger.info( "Starting all interfaces for '{}'.", this.getCanonicalName( ) );
			this.interfaceManager.start();
			status.recordStart();
			this.lifecycleState = ExecutionLifecycleState.STARTED;
			listeners.onStarted( this, this.lifecycleState );
			logger.info( "Started service '{}'.", canonicalName );

		} catch( Exception e ) {
			logger.error( "Forcing service exit during start due to exception.", e );
			System.exit( 1 );
		}
	}

	/**
	 * Private method, creating a set of keystores for use by the service.
	 */
	private void loadKeyStores( ) {
		KeyStoreManager keyStoreManager = new KeyStoreManager();
		if( getConfigurationManager( ).contains( ConfigurationConstants.SECURITY_KEY_STORES ) ) {
			logger.info( "Preparing keystores for '{}'.", this.getCanonicalName( ) );
			KeyStore keyStore = null;
			
			// key stores are loaded based firstly on the list found in the config
			List<String> keyStores = getConfigurationManager( ).getListValue( ConfigurationConstants.SECURITY_KEY_STORES, String.class );
			for( String keyStoreName : keyStores ) {
				keyStore = loadKeyStore( keyStoreName );
				keyStoreManager.register( keyStoreName, keyStore );
			}
		}
		// we register this regardless of having any loaded this
		// allows others to manual register if they so desire
		this.facilityManager.addFacility( KeyStoreManager.class, keyStoreManager );
	}
	
	/**
	 * Private method, creating a key store, which can be used for SSL handling
	 * if the configuration manage has a key store location/password specified.
	 * @return the key store, if settings specify it exists, null otherwise
	 */
	private KeyStore loadKeyStore( String theName ) {

		String keyStorePassword = null;
		String keyStoreLocation = null;
		String keyStoreType = null;
		String keyStoreProvider = null;
		
		KeyStore newKeyStore = null;

		try {
			// get the config values for the key store, the first two are required (assuming they WANT a keystore)
			// but the second two do not have to be provided
			keyStoreLocation 	= getConfigurationManager( ).getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_LOCATION_FORMAT, theName ) );
			keyStorePassword 	= getConfigurationManager( ).getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_PASSWORD_FORMAT, theName ) );
			keyStoreType 		= getConfigurationManager( ).getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_TYPE_FORMAT, theName ), KeyStore.getDefaultType( ) );
			keyStoreProvider	= getConfigurationManager( ).getStringValue( String.format( ConfigurationConstants.SECURITY_KEY_STORE_PROVIDER_FORMAT, theName ), null );
			
			// depending on what data they provide we will attempt to get a key store
			if( keyStoreProvider == null ) {
				newKeyStore = KeyStore.getInstance( keyStoreType );
			} else {
				newKeyStore = KeyStore.getInstance( keyStoreType, keyStoreProvider );
			}
			
			// now we try to load the keystore
		    // get user password and file input stream
		    FileInputStream inputStream = null;
		    try {
		        inputStream = new FileInputStream( keyStoreLocation );
		        newKeyStore.load( inputStream, keyStorePassword.toCharArray() );
			} finally {
		    	if( inputStream != null ) {
		    		inputStream.close( );
		        }
		    }
			
		    return newKeyStore;

		} catch( FileNotFoundException e ) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to unknown key store location '%s'.", theName, keyStoreLocation ), e );
	    } catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to an exception.", theName ), e );
		} catch (CertificateException e) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to an exception.", theName ), e );
		} catch (IOException e) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to an exception.", theName ), e );
		} catch( KeyStoreException e ) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to an exception.", theName ), e );
		} catch( NoSuchProviderException e ) {
			throw new ConfigurationException( String.format( "Could not load key store '%s' due to unknown key provider '%s'.", theName, keyStoreProvider ), e );
		}
	}

	/**
	 * Private method, creating a set of connector configurations, for used by http interfaces.
	 */
	private void loadConnectorConfigurations( ) {
		ConnectorConfigurationManager connectorConfigurationManager = new ConnectorConfigurationManager();
		if( getConfigurationManager( ).contains( ConfigurationConstants.HTTP_CONNECTORS ) ) {
			logger.info( "Preparing connectors for '{}'.", this.getCanonicalName( ) );
			ConnectorConfiguration connectorConfiguration = null;
			
			// configurations are loaded based firstly on the list found in the config 
			List<String> connectorConfigurations = getConfigurationManager( ).getListValue( ConfigurationConstants.HTTP_CONNECTORS, String.class );
			for( String connectorConfigurationName : connectorConfigurations ) {
				connectorConfiguration = loadConnectorConfiguration( connectorConfigurationName );
				connectorConfigurationManager.register( connectorConfiguration );
			}
		}
		// we register this regardless of having any loaded this
		// allows others to manual register if they so desire
		this.facilityManager.addFacility( ConnectorConfigurationManager.class, connectorConfigurationManager );
	}
	
	/**
	 * Private method, creating a connector configuration, which can be used for connector creation
	 * if the configuration for an interface references the specified name
	 * @return the configuration, if settings specify it exists, null otherwise
	 */
	private ConnectorConfiguration loadConnectorConfiguration( String theName ) {
    	return new ConnectorConfiguration( theName, this.getConfigurationManager() );
    }
	
	/**
	 * Private method that will load thread pool definitions from configuration and
	 * then create the thread pools.
	 */
	private void loadThreadPools( ) {
		ExecutorManager executorManager = new ExecutorManager();
		if( getConfigurationManager( ).contains( ConfigurationConstants.THREAD_POOLS ) ) {
			logger.info( "Preparing thread pools for '{}'.", this.getCanonicalName( ) );
			Executor executor = null;
			
			// configurations are loaded based firstly on the list found in the config 
			List<String> threadPools = getConfigurationManager( ).getListValue( ConfigurationConstants.THREAD_POOLS, String.class );
			for( String threadPoolName : threadPools ) {
				executor = loadThreadPool( threadPoolName );
				executorManager.register( threadPoolName, executor );
			}
		}
		// now we see if the standard thread pool has been configured and if
		// not then we add one in so at least one exists in the system
		if( executorManager.getExecutor( ThreadingConstants.DEFAULT_THREAD_POOL ) == null ) {
			int coreThreads = Runtime.getRuntime( ).availableProcessors( ) * ThreadingConstants.DEFAULT_CORE_THREADS_FACTOR;
			int maxThreads = coreThreads * ThreadingConstants.DEFAULT_MAX_THREAD_FACTOR;			
	    	
			ThreadPoolExecutor executor = new ThreadPoolExecutor(
	    			coreThreads, 
	    			maxThreads, 
	    			ThreadingConstants.DEFAULT_KEEP_ALIVE_TIME,
	                TimeUnit.MILLISECONDS, 
	                new ArrayBlockingQueue<Runnable>( maxThreads ),
	                new ConfigurableThreadFactory( ThreadingConstants.DEFAULT_THREAD_POOL, ThreadingConstants.DEFAULT_THREAD_PRIORITY, ThreadingConstants.DEFAULT_IS_DAEMON ) );

	    	// should we prestart the core threads?
	    	if( ThreadingConstants.DEFAULT_PRESTART_CORE ) {
				executor.prestartAllCoreThreads();
			}
	    	executorManager.register( ThreadingConstants.DEFAULT_THREAD_POOL, executor );
		}
		// we register this regardless of having any loaded this
		// allows others to manual register if they so desire
		this.facilityManager.addFacility( ExecutorManager.class, executorManager );
	}
	
	/**
	 * Private method that will load and create the thread pool configuration
	 * for a particular thread pools.
	 * @param theName the name of the thread pool to load and create
	 * @return the created thread pool
	 */
	private Executor loadThreadPool( String theName ) {
		// we get the settings to make the executor, which includes using defaults (except for core threads have to be specified if something is going to be specified)
    	int coreThreads = getConfigurationManager( ).getIntegerValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_CORE_SIZE, theName ) );
    	int maxThreads = getConfigurationManager( ).getIntegerValue(
    			String.format( ConfigurationConstants.THREAD_POOL_MAX_SIZE, theName ), 
    			coreThreads * ThreadingConstants.DEFAULT_MAX_THREAD_FACTOR );
    	long keepAliveTime = getConfigurationManager( ).getLongValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_KEEP_ALIVE_TIME, theName ), 
    			ThreadingConstants.DEFAULT_KEEP_ALIVE_TIME );
    	boolean prestartCore = getConfigurationManager( ).getBooleanValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_PRESTART_CORE, theName ), 
    			ThreadingConstants.DEFAULT_PRESTART_CORE );

    	String prefix = getConfigurationManager( ).getStringValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_THREAD_NAME_PREFIX, theName ), 
    			theName );
    	int priority = getConfigurationManager( ).getIntegerValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_THREAD_PRIORITY, theName ), 
    			ThreadingConstants.DEFAULT_THREAD_PRIORITY );
    	boolean isDaemon = getConfigurationManager( ).getBooleanValue( 
    			String.format( ConfigurationConstants.THREAD_POOL_THREAD_IS_DAEMON, theName ), 
    			ThreadingConstants.DEFAULT_IS_DAEMON );

    	ThreadPoolExecutor executor = new ThreadPoolExecutor(
    			coreThreads, 
    			maxThreads, 
    			keepAliveTime,
                TimeUnit.MILLISECONDS, 
                new ArrayBlockingQueue<Runnable>( maxThreads ),
                new ConfigurableThreadFactory( prefix, priority, isDaemon ) );
    	
    	if( prestartCore ) {
    		executor.prestartAllCoreThreads();
    	}
		return executor;
	}

	/**
	 * Initializes the configuration systems. This
	 * method is meant to be overridden by subclasses.
	 * The configuration manager will already be setup 
	 * with a command-line source that is usable. 
	 * Subclasses are free to add other sources.
	 * This is call prior to onStart.
	 */
	protected void onInitializeConfiguration( ) {
		
	}
	
	/**
	 * Method that can be overridden by subclasses to 
	 * manage the start up process.
	 */
	protected void onStart( ) {
	}
	
	/**
	 * This is the generic method for running which really does nothing 
	 * but wait for a shutdown.
	 */
	public void run( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED, "Cannot run the service when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		listeners.onRunning( this, this.lifecycleState );
		logger.info( "Running service '{}'.", canonicalName );
		synchronized( this.shutdownLock ) {
			try {
				this.shutdownLock.wait();
			} catch (InterruptedException e) {
				// ignore exception, but stops run
				Thread.currentThread().interrupt();
			}
		}
	}
	
	/**
	 * Method called when the service is stopping.
	 * Subclasses cannot override this, but should
	 * override the onStop method.
	 */
	public final void stop( ) {
		try {
			Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED || this.lifecycleState == ExecutionLifecycleState.RUNNING || this.lifecycleState == ExecutionLifecycleState.SUSPENDED, "Cannot stop the service when the status is '%s'.", this.lifecycleState );
			this.lifecycleState = ExecutionLifecycleState.STOPPING;
			logger.info( "Stopping service '{}'.", canonicalName );
			listeners.onStopping( this, this.lifecycleState );

			Period executionPeriod;
			
			// now we shutdown all of the interfaces
			logger.info( "Stopping all interfaces." );
			this.interfaceManager.stop();
			
			// now let subclasses override, if any
			onStop( );
			executionPeriod = status.calculateRunningTime();
			status.recordStop( );
			this.lifecycleState = ExecutionLifecycleState.STOPPED;
			logger.info( "Stopped service '{}' (ran for {}).", canonicalName, executionPeriod.toString( timeFormatter ) );
			listeners.onStopped( this, this.lifecycleState );
	
		} catch( Exception e ) {
			logger.error( "Forcing service exit during stop due to exception.", e );
			System.exit( 1 );		
		}
	}
	
	/**
	 * Method that can be overridden by subclasses to
	 * manage the shutdown process.
	 */
	protected void onStop( ) {
	}
	
	/**
	 * This is used to signal the service to shutdown gracefully.
	 * 
	 */
	final public void signalStop( ) {
		synchronized( this.shutdownLock ) {
			this.shutdownLock.notifyAll();
		}
	}
	
	/**
	 * This is used to signal the service to just plain abort
	 * without graceful shutdown.
	 */
	final public void signalKill( ) {
		System.exit( -1 );
	}
	
	
    /**
     * This is an internal method to manage unhandled exceptions. It simple 
     * logs it happened and then calls an overriddable method for additional processing. 
     * @param theThread the thread the exception occurred in
     * @param theException the exception that occurred
     */
    private void handleUnhandledException( Thread theThread, Throwable theException ) {
		status.recordUnhandledException();
		logger.error( String.format( "Thread '%s' had an uncaught exception.", theThread.getName() ), theException );
		theException.printStackTrace();
		this.onUnhandledException(theThread, theException);
    }
    
    /**
     * This is an method that can be overridden by the subclass to do something 
     * when an unhandled exception occurs
     * @param theThread the thread the exception occurred in
     * @param theException the exception that occurred
     */
    protected void onUnhandledException( Thread theThread, Throwable theException ) {
    }
    
	// need logging support? (or is that inherent)
	// REST
	//   /rest/version/system
	//	 /rest/version/system/errors [alerts and exception, etc]
	//   /rest/version/system/health
	//   /rest/version/system/performance [above three could be tied together]
	//   /rest/version/system/build [build information, include contracts supported and their versions, and binary build information]
	//   get somethign where the average timing is being shown for method calls with low and highs (and when)

 // TODO:
//     - need to figure out how to setup the httpclient to send built-in headers
//       (like UA and 'referrer' tag) .. referrer tag is the calling URL that was made
 // http://www.theserverside.com/discussions/thread.tss?thread_id=21055
 // needs to be maintained someone (storing with the batched element)
}
