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

package com.talvish.tales.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.talvish.tales.parts.ArgumentParser;
import com.talvish.tales.system.configuration.ConfigurationException;
import com.talvish.tales.system.configuration.ConfigurationManager;
import com.talvish.tales.system.configuration.HierarchicalFileSource;
import com.talvish.tales.system.configuration.MapSource;
import com.talvish.tales.system.configuration.PropertyFileSource;

/**
 * This is a simple helper class that is designed to minimize the 
 * amount of boiler plate code needed to start-up a service. If this
 * class is used as the main entry point for starting a service it
 * will look at the command-line and potential settings file for
 * a service type and if found it will instantiate the service
 * and get it running.
 * @author jmolnar
 *
 */
public final class ServiceHost {
	private static final Logger logger = LoggerFactory.getLogger( ServiceHost.class );

	/**
	 * Helper method that will create a ConfigurationManager that contains
	 * a command-line parser and a properties file parser pointed to by
	 * the command-line argument 'settings.file'. 
	 * @param theArgs the command line arguments
	 * @return the ConfigurationManager
	 */
	public static ConfigurationManager instantiateConfiguration( String[] theArgs ) {
    	logger.info( "Service host is setting up the configuration manager." );

    	// first thing we need is for configuration handling 
		ConfigurationManager configurationManager = new ConfigurationManager( );
		// which will start with command line source
		configurationManager.addSource( new MapSource( "command-line", ArgumentParser.parse( theArgs ) ) );
		// and then we check to see if we have file handling being requested
		String filename = configurationManager.getStringValue( "settings.file", null ); 
		// and if we do, we add a file handling source
		if( !Strings.isNullOrEmpty( filename ) ) {
			String profile = configurationManager.getStringValue( "settings.profile", null );
			String block = configurationManager.getStringValue( "settings.block", null );
			
			if( !Strings.isNullOrEmpty( profile ) && !Strings.isNullOrEmpty( block ) ) {
				configurationManager.addSource( new HierarchicalFileSource( profile,  block, filename ) );
			} else if( Strings.isNullOrEmpty( profile ) && Strings.isNullOrEmpty( block ) ) {
				configurationManager.addSource( new PropertyFileSource( filename ) );
			} else {
				throw new ConfigurationException( String.format( "Only some configuration setup settings are available (profile:'%s', block: '%s', source: '%s').", profile, block, filename ) );
			}
		}
		
		return configurationManager;
	}
	

	/**
	 * Helper method that will use a class loader to load a class that must extend
	 * Service and whose type name is based on the configuration setting 'service.type'.
	 * @param theManager the configuration manager to use
	 * @return the class representing the service
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends Service> loadServiceClass( ConfigurationManager theManager ) {
		// now we see what we have in the way of which service to start        
		ClassLoader classLoader = null;
        String serviceType = null;
    	Class<? extends Service> serviceClass = null;
    	
    	// TODO: need to fix that we have two forms of configuration ...
		
    	try {
        	// need a class loader to create the class
	        classLoader = ServiceHost.class.getClassLoader();
        	// we need to get the service type
            serviceType = theManager.getStringValue( ConfigurationConstants.SERVICE_TYPE );

        	logger.info( "Service host is attempting to load the service '{}'.", serviceType )
        	;
            // now we load the type
        	serviceClass = ( Class<? extends Service> )classLoader.loadClass( serviceType );
        	
        } catch( ClassNotFoundException e ) {
        	throw new ConfigurationException( String.format( "Failed to load service class since the class '%s' could not be found.", serviceType ), e );
        } catch( ClassCastException e ) {
        	throw new ConfigurationException( String.format( "Failed to load service class since the class '%s' does not extend Service.", serviceType ), e );
        } catch( SecurityException e ) {
        	throw new ConfigurationException( String.format( "Failed to load service class '%s' due to a security exception.", serviceType ), e );
        } catch( IllegalArgumentException e ) {
        	throw new ConfigurationException( String.format( "Failed to load service class '%s' due to an exception.", serviceType ), e );
        }
    	return serviceClass;
	}
	
	/**
	 * A helper method that takes a class that extends Service and instantiates it.
	 * The method looks for a default (parameterless) constructor.
	 * This and the loadServiceClass are separated to allow for custom hosts to 
	 * instantiate services in a different manner than described below.
	 * @param theServiceClass the class of the service to instantiate
	 * @return the instantiated service
	 */
	public static Service instantiateService( Class<? extends Service> theServiceClass ) {
		// now we see what we have in the way of which service to start        
    	Constructor<?> serviceConstructor = null;
    	Service serviceInstance = null;
    	
    	try {
        	logger.info( "Service host is attempting to create the service '{}'.", theServiceClass.getName( ) );
        	
        	// and then get the constructor we expected
        	serviceConstructor = theServiceClass.getConstructor( );
        	// create the interface
        	serviceInstance = ( Service )serviceConstructor.newInstance( );
        	
        	
        } catch( ClassCastException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate service since class '%s' does not extend Service.", theServiceClass.getName( ) ), e );
        } catch( NoSuchMethodException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate service since class '%s' is missing a default (parameterless) constructor.", theServiceClass.getName( ) ), e );
        } catch( IllegalAccessException | SecurityException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate service using class '%s' due to a security exception.", theServiceClass.getName( ) ), e );
        } catch( IllegalArgumentException | InstantiationException | InvocationTargetException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate service using class '%s' due to an exception.", theServiceClass.getName( ) ), e );
        }
    	return serviceInstance;
	}

	
	/**
	 * The standard Java main entry point, which in this case will
	 * instantiate and run the service and then wait for it to be 
	 * shutdown.
	 * @param theArgs the arguments containing the start-up or override parameters
	 * @throws Exception any potential uncaught exception
	 */
    public static void main( String[ ] theArgs ) throws Exception {
    	logger.info( "Service host is initializing." );
    	
    	ConfigurationManager configurationManager = instantiateConfiguration( theArgs );
    	Class<? extends Service > serviceClass = loadServiceClass( configurationManager );
    	Service serviceInstance = instantiateService( serviceClass );
    	
    	// now we have the service create 
    	// so let's get it running and wait
    	// for it to finish
    	serviceInstance.start( configurationManager );
    	serviceInstance.run( );
    	serviceInstance.stop( );
	}
}
