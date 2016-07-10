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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.talvish.tales.parts.ArgumentParser;
import com.talvish.tales.system.configuration.ConfigurationException;
import com.talvish.tales.system.configuration.ConfigurationManager;
import com.talvish.tales.system.configuration.ConfigurationSource;
import com.talvish.tales.system.configuration.HierarchicalFileSource;
import com.talvish.tales.system.configuration.MapSource;
import com.talvish.tales.system.configuration.PropertyFileSource;
import com.talvish.tales.system.configuration.SourceConfiguration;

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
			// TODO: it would be nice if the default for this was actually the canonical name of the services
			//       so that the block name wouldn't have to be entered all the time though there is a 
			//		 slight chicken-egg problem given the service class to instantiate comes from config
			String block = configurationManager.getStringValue( "settings.block", null );
			
			if( !Strings.isNullOrEmpty( profile ) && !Strings.isNullOrEmpty( block ) ) {
				configurationManager.addSource( new HierarchicalFileSource( profile,  block, filename ) );
			} else if( Strings.isNullOrEmpty( profile ) && Strings.isNullOrEmpty( block ) ) {
				configurationManager.addSource( new PropertyFileSource( filename ) );
			} else {
				throw new ConfigurationException( String.format( "Cannote quite determine which source type (property file or hieararchical) due to missing settings (profile: '%s', block: '%s', source: '%s').", profile, block, filename ) );
			}
		}
		// and then we see if a we have additional sources requested
		List<String> sourceClassNames = configurationManager.getListValue( "settings.sources", String.class, null );
		if( sourceClassNames != null ) {
			Class<? extends ConfigurationSource> sourceClass;
			ConfigurationSource source;
			for( String sourceClassName : sourceClassNames ) {
				// we try to load the class 
		    	logger.info( "Service host is attempting to load configuration source '{}'.", sourceClassName );
				sourceClass = loadClass( sourceClassName , ConfigurationSource.class );
				// and then create an instance
	        	logger.info( "Service host is attempting to create configuration source '{}'.", sourceClassName );
				source = instantiateSource( sourceClass, configurationManager );
				// which we save in the list of sources
				configurationManager.addSource( source );
			}
		}
		
		return configurationManager;
	}

	/**
	 * Helper method that will use a class loader to load a class.
	 * An exception is thrown if there are any issues.
	 * @param <T>
	 * @param theClassName the class to load
	 * @return the class request
	 */
	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> loadClass( String theClassName, Class<T> theExpectedType ) {
		ClassLoader classLoader = null;
    	Class<? extends T> desiredClass = null;
    	
    	try {
        	// need a class loader to create the class
	        classLoader = ServiceHost.class.getClassLoader();
            // now we load the type
	        desiredClass = ( Class<? extends T> )classLoader.loadClass( theClassName );
        	
        } catch( ClassNotFoundException e ) {
        	throw new ConfigurationException( String.format( "Failed to load class since the class '%s' could not be found.", theClassName ), e );
        } catch( ClassCastException e ) {
        	throw new ConfigurationException( String.format( "Failed to load class since the class '%s' does not extend '{}'.", theClassName, theExpectedType.getName() ), e );
        } catch( SecurityException e ) {
        	throw new ConfigurationException( String.format( "Failed to load class '%s' due to a security exception.", theClassName ), e );
        } catch( IllegalArgumentException e ) {
        	throw new ConfigurationException( String.format( "Failed to load class '%s' due to an exception.", theClassName ), e );
        }
    	return desiredClass;
	}
	
	/**
	 * A helper method that takes a class that extends Service and instantiates it.
	 * The method looks for a default (parameterless) constructor.
	 * This and the loadServiceClass are separated to allow for custom hosts to 
	 * instantiate services in a different manner than described below.
	 * @param theServiceClass the class of the service to instantiate
	 * @return the instantiated service
	 */
	private static Service instantiateService( Class<? extends Service> theServiceClass ) {
		// now we see what we have in the way of which service to start        
    	Constructor<?> serviceConstructor = null;
    	Service serviceInstance = null;
    	
    	try {
        	// we get the constructor we expected
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
	 * A helper method that will instantiate a source for use
	 * in the configuration manager.
	 * @param theSourceClass the class of the source to instantiate
	 * @param theManager the configuration manager, which is used to get configuration for the source
	 * @return the newly instantiated source
	 */
	private static ConfigurationSource instantiateSource( Class<? extends ConfigurationSource> theSourceClass, ConfigurationManager theManager ) {
		// now we see what we have in the way of which service to start        
    	Constructor<?> sourceConstructor = null;
    	ConfigurationSource sourceInstance = null;
    	
    	try {
        	
        	// so first I need to get the annotation of the configuration class to use to get settings
    		SourceConfiguration configurationAnnotation = theSourceClass.getAnnotation( SourceConfiguration.class );
    		// and I need to some validation of the annotation
    		if( configurationAnnotation == null ) {
            	throw new ConfigurationException( String.format( "Failed to instantiate source since class '%s' does not have the SourceConfiguration annotation.", theSourceClass.getName( ) ) );
    		}
    		if( configurationAnnotation.settingsClass() == null ) {
            	throw new ConfigurationException( String.format( "Failed to instantiate source since the SourceConfiguration annotation on class '%s' does not provide a settings class.", theSourceClass.getName( ) ) );
    		}
        	// and then I need to load the settings from the config manager
    		Object settings = theManager.getValues( configurationAnnotation.settingsClass( ) );
        	// and then get the constructor we need to load the source up (it takes it's settings)
        	sourceConstructor = theSourceClass.getDeclaredConstructor( configurationAnnotation.settingsClass( ) );
        	// and then finally we create the instance we want
        	sourceInstance = ( ConfigurationSource )sourceConstructor.newInstance( settings );
        	
        	
        } catch( ClassCastException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate source since class '%s' does not extend ConfigurationSource.", theSourceClass.getName( ) ), e );
        } catch( NoSuchMethodException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate source since class '%s' is missing the constructor taking the parameter of the type associated with the source.", theSourceClass.getName( ) ), e );
        } catch( IllegalAccessException | SecurityException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate source using class '%s' due to a security exception.", theSourceClass.getName( ) ), e );
        } catch( IllegalArgumentException | InstantiationException | InvocationTargetException e ) {
        	throw new ConfigurationException( String.format( "Failed to instantiate source using class '%s' due to an exception.", theSourceClass.getName( ) ), e );
        }
    	return sourceInstance;
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
        String serviceClassName = configurationManager.getStringValue( ConfigurationConstants.SERVICE_TYPE );
    	logger.info( "Service host is attempting to load the service '{}'.", serviceClassName );
    	Class<? extends Service > serviceClass = loadClass( serviceClassName, Service.class );
    	logger.info( "Service host is attempting to create the service '{}'.", serviceClassName );
    	Service serviceInstance = instantiateService( serviceClass );
    	
    	// now we have the service create 
    	// so let's get it running and wait
    	// for it to finish
    	serviceInstance.start( configurationManager );
    	serviceInstance.run( );
    	serviceInstance.stop( );
	}
}
