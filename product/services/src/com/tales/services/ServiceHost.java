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

package com.tales.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.parts.ArgumentParser;
import com.tales.system.configuration.ConfigurationException;
import com.tales.system.configuration.ConfigurationManager;
import com.tales.system.configuration.MapSource;
import com.tales.system.configuration.PropertySource;

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
	/**
	 * The standard Java main entry point, which in this case will
	 * instantiate and run the service and then wait for it to be 
	 * shutdown.
	 * @param theArgs the arguments containing the start-up or override parameters
	 * @throws Exception any potential uncaught exception
	 */
    public static void main( String[ ] theArgs ) throws Exception {
		// first thing we need is for configuration handling 
		ConfigurationManager configurationManager = new ConfigurationManager( );
		// which will start with command line source
		configurationManager.addSource( new MapSource( "command-line", ArgumentParser.parse( theArgs ) ) );
		// and then we check to see if we have file handling being requested
		String filename = configurationManager.getStringValue( "settings.file", null ); 
		// and if we do, we add a file handling source
		if( !Strings.isNullOrEmpty( filename ) ) {
			configurationManager.addSource( new PropertySource( filename ) );
		}

		// now we see what we have in the way of which service to start        
		ClassLoader classLoader = null;
        String serviceType = null;
    	Class<?> serviceClass = null;
    	Constructor<?> serviceConstructor = null;
    	Service serviceInstance = null;
		
    	try {
        	// need a class loader to create the class
	        classLoader = ServiceHost.class.getClassLoader();
        	// we need to get the service type
            serviceType = configurationManager.getStringValue( ConfigurationConstants.SERVICE_TYPE );
        	// now we load the type
        	serviceClass = classLoader.loadClass( serviceType );
        	Preconditions.checkState( Service.class.isAssignableFrom( serviceClass ), "Failed to setup service since class '%s' does not extend Service.", serviceType );
        	
        	// and then get the constructor we expected
        	serviceConstructor = serviceClass.getConstructor( );
        	// create the interface
        	serviceInstance = ( Service )serviceConstructor.newInstance( );
        	
        } catch( ClassNotFoundException e ) {
        	throw new ConfigurationException( String.format( "Failed to setup service since class '%s' could not be found.", serviceType ), e );
        } catch( NoSuchMethodException e ) {
        	throw new ConfigurationException( String.format( "Failed to setup service since class '%s' is missing a default (parameter-less) constructor.", serviceType ), e );
        } catch( IllegalAccessException | SecurityException e ) {
        	throw new ConfigurationException( String.format( "Failed to setup service using class '%s' due to a security exception.", serviceType ), e );
        } catch( IllegalArgumentException | InstantiationException | InvocationTargetException e ) {
        	throw new ConfigurationException( String.format( "Failed to setup service using class '%s' due to an exception.", serviceType ), e );
        }

    	// now we have the service create 
    	// so let's get it running and wait
    	// for it to finish
    	serviceInstance.start( theArgs );
    	serviceInstance.run( );
    	serviceInstance.stop( );
	}
}
