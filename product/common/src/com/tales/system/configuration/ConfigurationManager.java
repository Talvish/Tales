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
package com.tales.system.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.joda.time.DateTime;


import com.google.common.base.Preconditions;
import com.tales.system.Facility;

/**
 * A configuration system that allows managing more than one source
 * to get configuration information from.
 * @author jmolnar
 *
 */
public class ConfigurationManager implements Facility {
	private final ReentrantReadWriteLock settingsLock = new ReentrantReadWriteLock( );
	private final Lock settingsReadLock = settingsLock.readLock();
	private final Lock settingsWriteLock = settingsLock.writeLock();

	private final HashMap<String,LoadedSetting> loadedSettings = new HashMap<String, LoadedSetting>( );
	private ArrayList<ConfigurationSource> sources;
	
	/**
	 * Constructor taking nothing.
	 */
	public ConfigurationManager( ) {
		sources = new ArrayList<ConfigurationSource>( 0 );
	}

	/**
	 * Constructor taking the set of sources to use. 
	 * The sources will be used in order of the list.
	 * @param theSources
	 */
	public ConfigurationManager( ConfigurationSource ... theSources ) {
		Preconditions.checkNotNull( theSources );
		
		sources = new ArrayList<ConfigurationSource>( theSources.length + 1 );
		Collections.addAll( sources, theSources );
	}

	/**
	 * Returns the sources that are part of this configuration manager.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Collection<ConfigurationSource> getSources( ) {
		return Collections.unmodifiableCollection( ( Collection< ConfigurationSource > )this.sources.clone( ) );
	}

	/**
	 * Adds a new source to the manager.
	 * @param aSource the source to add
	 */
	public synchronized void addSource( ConfigurationSource aSource ) {
		Preconditions.checkNotNull( aSource, "must pass a source" );
		
		// we copy since we don't want to impact anyone else who many be currently using the sources
		// yes this adds to heap usage but we expect the source count to be small and calls to 
		// add sources to be minimal
		ArrayList<ConfigurationSource> newSources = new ArrayList<ConfigurationSource>( this.sources );
		newSources.add( aSource );
		sources = newSources; // replace the list
	}
	
	/**
	 * Called to find out if a source contains the requested value.
	 * @param theName the name of the value to check for
	 * @return true if found, false otherwise
	 */
	public boolean contains( String theName ) {
		boolean returnValue = false;
		
		for( ConfigurationSource source : sources ) {
			returnValue = source.contains( theName );
			if( returnValue ) {
				break;
			}
		}		
		return returnValue;
	}
	
	/**
	 * Gets a String value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the String value
	 */
	public String getStringValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, String.class, false ); // does validation, will except if value not there
		try {
			return ( String )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets a String value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the String value
	 */
	public String getStringValue( String theName, String theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, String.class, true ); // does validation, will not except if value not there
		try {
			return ( String )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an Integer value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the Integer value
	 */
	public Integer getIntegerValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, Integer.class, false ); // does validation, will except if not there
		try {
			return ( Integer )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets an Integer value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the Integer value
	 */
	public Integer getIntegerValue( String theName, Integer theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, Integer.class, true ); // does validation, will not except if value not there
		try {
			return ( Integer )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an Long value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the Long value
	 */
	public Long getLongValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, Long.class, false ); // does validation, will except if value not there
		try {
			return ( Long )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets an Long value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the Long value
	 */
	public Long getLongValue( String theName, Long theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, Long.class, true ); // does validation, will not except if value not there
		try {
			return ( Long )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an Double value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the Double value
	 */
	public Double getDoubleValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, Double.class, false ); // does validation, will except if value not there
		try {
			return ( Double )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets an Double value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the Double value
	 */
	public Double getDoubleValue( String theName, Double theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, Double.class, true ); // does validation, will not except if value not there
		try {
			return ( Double )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an Boolean value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the Boolean value
	 */
	public Boolean getBooleanValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, Boolean.class, false ); // does validation, will except if value not there
		try {
			return ( Boolean )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets an Boolean value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the Boolean value
	 */
	public Boolean getBooleanValue( String theName, Boolean theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, Boolean.class, true ); // does validation, will not except if value not there
		try {
			return ( Boolean )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an DateTime value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the DateTime value
	 */
	public DateTime getDateTimeValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, DateTime.class, false ); // does validation, will except if value not there
		try {
			return ( DateTime )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
	
	/**
	 * Gets an DateTime value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the DateTime value
	 */
	public DateTime getDateTimeValue( String theName, DateTime theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, DateTime.class, true ); // does validation, will not except if value not there
		try {
			return ( DateTime )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets a List of a particular type from configuration. 
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theElementType the type of the element in the list
	 * @return the list
	 */
 	@SuppressWarnings("unchecked")
	public <T> List<T> getListValue( String theName, Class<T> theElementType ) {
 		LoadedSetting setting = getList( theName, null, theElementType, false ); // does validation, will except if value not there
		try {
			return ( List<T> )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}
 	
	/**
	 * Gets a Map of the specified types.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @return the list
	 */
 	@SuppressWarnings("unchecked")
	public <K,V> Map<K,V> getMapValue( String theName, Class<K> theKeyType, Class<V> theValueType ) {
 		LoadedSetting setting = getMap( theName, null, theKeyType, theValueType, false ); // does validation, will except if value not there
		try {
			return ( Map<K,V> )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type." ), e );
		}
	}

	/**
	 * Gets configuration setting information for a particular name.
	 * @param theName the name of the configuration to get setting information for
	 * @return the loaded setting if available, otherwise null
	 */
	public LoadedSetting getLoadedSetting( String theName ) {
		settingsReadLock.lock();
		try {
			return this.loadedSettings.get( theName );
		} finally {
			settingsReadLock.unlock( );
		}
	}
	
	/**
	 * This method returns all the current loaded settings.
	 * This makes a copy of the list to handle multi-threaded
	 * access, but care must be taken to not continually call
	 * and create lots of temporary objects.
	 * @return All loaded settings
	 */
	@SuppressWarnings("unchecked")
	public Map<String, LoadedSetting> getAllLoadedSettings() {
		settingsReadLock.lock();
		try {
			return Collections.unmodifiableMap( ( Map<String, LoadedSetting> )this.loadedSettings.clone() );
		} finally {
			settingsReadLock.unlock( );
		}
	}
	
	/**
	 * Internal helper method to get the string value and indicate 
	 * we are using the value. If we have gotten the value before
	 * then the local cached copy is used.
	 * @param theName The name of the config value to get
	 * @param theDefault the default value to use if the name cannot be found
	 * @param theType the type of the item being retrieved, as requested by the original caller
	 * @param useDefault indicates if the default should be used, or an exception thrown if the name cannot be found
	 * @return The loaded setting value.
	 */
	private <T> LoadedSetting getValue( String theName, T theDefault, Class<T> theType, boolean useDefault ) {
		LoadedSetting setting = null;

		settingsReadLock.lock();
		try {
			// look to see if we requested this before, if so
			// use it again ... yes this could be the wrong 
			// type BUT the cast will fail in the above methods 
			// and throw a config exception
			setting = loadedSettings.get( theName ); 
		} finally {
			settingsReadLock.unlock( );
		}
		if( setting != null ) {
			// we got it before, so indicate we are using
			// it again, and get the value to return
			setting.indicateRequested(); 
		} else {
			settingsWriteLock.lock( );
			try {
				Collection<ConfigurationSource> sources = this.sources; // this makes ensures we aren't touching a list that gets modified/replaces
				// this is first time requested, so we
				// will need to create the configuration setting
				for( ConfigurationSource source : sources ) {
					setting = source.getValue( theName, theType );
					if( setting != null ) {
						break;
					}
				}
				if( setting == null ) {
					if( useDefault ) {
						setting = new LoadedSetting( theName, theDefault, theDefault == null ? "" : theDefault.toString(), "default-value" );
					} else {
						throw new ConfigurationException( String.format( "Could not find a value for '%s'.", theName ) );
					}
				}
				// make sure we save, regardless of how it got made
				this.loadedSettings.put( theName, setting );
			} finally {
				settingsWriteLock.unlock( );
			}
		}
	
		return setting;
	}

	/**
	 * Internal helper method to get the list and indicate 
	 * we are using the list. If we have gotten the list before
	 * then the local cached copy is used.
	 * @param theName The name of the config value to get
	 * @param theDefault the default value to use if the name cannot be found
	 * @param theType the type of the item being retrieved, as requested by the original caller
	 * @param useDefault indicates if the default should be used, or an exception thrown if the name cannot be found
	 * @return The loaded setting for the list
	 */
	private <T> LoadedSetting getList( String theName, List<T> theDefault, Class<T> theElementType, boolean useDefault ) {
		LoadedSetting setting = null;
		
		settingsReadLock.lock();
		try {
			// look to see if we requested this before, if so
			// use it again ... yes this could be the wrong 
			// type BUT the cast will fail in the above methods 
			// and throw a config exception
			setting = loadedSettings.get( theName ); 
		} finally {
			settingsReadLock.unlock( );
		}
		if( setting != null ) {
			// we got it before, so indicate we are using
			// it again, and get the value to return
			setting.indicateRequested(); 
		} else {
			settingsWriteLock.lock( );
			try {
				Collection<ConfigurationSource> sources = this.sources; // this makes ensures we aren't touching a list that gets modified/replaces

				// this is first time requested, so we
				// will need to create the configuration setting
				for( ConfigurationSource source : sources ) {
					setting = source.getList( theName, theElementType );
					if( setting != null ) {
						break;
					}
				}
				if( setting == null ) {
					if( useDefault ) {
						setting = new LoadedSetting( theName, theDefault, theDefault == null ? "" : theDefault.toString(), "default-value" );
					} else {
						throw new ConfigurationException( String.format( "Could not find a value for '%s'.", theName ) );
					}
				}
				// make sure we save, regardless of how it got made
				this.loadedSettings.put( theName, setting );
			} finally {
				settingsWriteLock.unlock( );
			}
		}
		return setting;
	}
	
	/**
	 * Gets a Map of the specified types.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @return the loaded setting for the map
	 */
 	private <K,V> LoadedSetting getMap( String theName, Map<K,V> theDefault, Class<K> theKeyType, Class<V> theValueType, boolean useDefault ) {
		LoadedSetting setting = null;
		
		settingsReadLock.lock();
		try {
			// look to see if we requested this before, if so
			// use it again ... yes this could be the wrong 
			// type BUT the cast will fail in the above methods 
			// and throw a config exception
			setting = loadedSettings.get( theName ); 
		} finally {
			settingsReadLock.unlock( );
		}
		if( setting != null ) {
			// we got it before, so indicate we are using
			// it again, and get the value to return
			setting.indicateRequested(); 
		} else {
			settingsWriteLock.lock( );
			try {
				Collection<ConfigurationSource> sources = this.sources; // this makes ensures we aren't touching a list that gets modified/replaces

				// this is first time requested, so we
				// will need to create the configuration setting
				for( ConfigurationSource source : sources ) {
					setting = source.getMap( theName, theKeyType, theValueType );
					if( setting != null ) {
						break;
					}
				}
				if( setting == null ) {
					if( useDefault ) {
						setting = new LoadedSetting( theName, theDefault, theDefault == null ? "" : theDefault.toString(), "default-value" );
					} else {
						throw new ConfigurationException( String.format( "Could not find a value for '%s'.", theName ) );
					}
				}
				// make sure we save, regardless of how it got made
				this.loadedSettings.put( theName, setting );
			} finally {
				settingsWriteLock.unlock( );
			}
		}
		return setting;
	}
}
