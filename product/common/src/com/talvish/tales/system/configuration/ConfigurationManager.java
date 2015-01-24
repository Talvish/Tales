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
package com.talvish.tales.system.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.system.Facility;
import com.talvish.tales.system.configuration.annotated.RegisteredCollection;
import com.talvish.tales.system.configuration.annotated.SettingField;
import com.talvish.tales.system.configuration.annotated.SettingType;
import com.talvish.tales.system.configuration.annotated.SettingTypeManager;

/**
 * A configuration system that allows managing more than one source
 * to get configuration information from.
 * @author jmolnar
 *
 */
public class ConfigurationManager implements Facility {
	private static final Logger logger = LoggerFactory.getLogger( ConfigurationManager.class );

	// the setting type manager is used to help map the class onto settings
	private final SettingTypeManager settingTypeManager = new SettingTypeManager( );

	private final ReentrantReadWriteLock settingsLock = new ReentrantReadWriteLock( );
	private final Lock settingsReadLock = settingsLock.readLock();
	private final Lock settingsWriteLock = settingsLock.writeLock();

	private final HashMap<String,LoadedSetting> loadedSettings = new HashMap<String, LoadedSetting>( );
	private ArrayList<ConfigurationSource> sources;
	
	/**
	 * Constructor taking nothing.
	 */
	public ConfigurationManager( ) {
		logger.info( "Configuration manager is initalizing." );
		sources = new ArrayList<ConfigurationSource>( 0 );		
	}

	/**
	 * Constructor taking the set of sources to use. 
	 * The sources will be used in order of the list.
	 * @param theSources
	 */
	public ConfigurationManager( ConfigurationSource ... theSources ) {
		logger.info( "Configuration manager is initalizing." );
		Preconditions.checkNotNull( theSources );
		
		sources = new ArrayList<ConfigurationSource>( theSources.length + 1 );
		sources.forEach(  source->logger.info( "Adding source '{}' (of type '{}') to configuration manager.", source.getName(), source.getClass().getName( ) ) );
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
		logger.info( "Adding source '{}' (of type '{}') to configuration manager.", aSource.getName(), aSource.getClass().getName( ) );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}
	
	/**
	 * Gets a float value from configuration.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @return the Double value
	 */
	public Float getFloatValue( String theName ) {
		LoadedSetting setting = getValue( theName, null, Float.class, false ); // does validation, will except if value not there
		try {
			return ( Float )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}

	/**
	 * Gets an Double value from configuration.
	 * This will return if name isn't value, but except if the value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theDefault the default value to use if not found
	 * @return the Double value
	 */
	public Float getFloatValue( String theName, Float theDefault ) {
		LoadedSetting setting = getValue( theName, theDefault, Float.class, true ); // does validation, will not except if value not there
		try {
			return ( Float )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
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
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}
 	
	/**
	 * Gets a List of a particular type from configuration.
	 * This will use the default if the list is not available
	 * @param theName the name of the configuration value to get
	 * @param theElementType the type of the element in the list
	 * @param theDefault the default list to use
	 * @return the list
	 */
 	@SuppressWarnings("unchecked")
	public <T> List<T> getListValue( String theName, Class<T> theElementType, List<T> theDefault ) {
 		LoadedSetting setting = getList( theName, theDefault, theElementType, true ); // does validation, will except if value not there
		try {
			return ( List<T> )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}
 	
 	/**
	 * Gets a Map of the specified types.
	 * This will except if the name isn't found or value cannot be converted.
	 * @param theName the name of the configuration value to get
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @return the map
	 */
 	@SuppressWarnings("unchecked")
	public <K,V> Map<K,V> getMapValue( String theName, Class<K> theKeyType, Class<V> theValueType ) {
 		LoadedSetting setting = getMap( theName, null, theKeyType, theValueType, false ); // does validation, will except if value not there
		try {
			return ( Map<K,V> )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}
 	
 	/**
	 * Gets a Map of the specified types.
	 * This will use the default if the map is not available
	 * @param theName the name of the configuration value to get
	 * @param theKeyType the type of the key of the map to retrieve
	 * @param theValueType the type of the value of the map to retrieve
	 * @param theDefault the default map to use
	 * @return the map
	 */
 	@SuppressWarnings("unchecked")
	public <K,V> Map<K,V> getMapValue( String theName, Class<K> theKeyType, Class<V> theValueType, Map<K,V> theDefault ) {
 		LoadedSetting setting = getMap( theName, theDefault, theKeyType, theValueType, true ); // does validation, will except if value not there
		try {
			return ( Map<K,V> )setting.getValue( );
		} catch( ClassCastException e ) {
			throw new ConfigurationException( String.format( "The value for setting '%1$s' is not the requested type.", theName ), e );
		}
	}
 	
	/**
	 * This method will load all setting as identified by annotations on the
	 * fields of the specified class.
	 * @param theClass the class that has annotations outlining the settings desired
	 * @return an instance of the class with the settings loaded and set 
	 */
	public <T> T getValues( Class<T> theClass ) {
		Preconditions.checkNotNull( theClass, "Cannot get values for a null class." );
		return getValues( theClass, null, null );
	}

	/**
	 * This method will load all setting as identified by annotations on the fields of the specified class.
	 * A prefix for the setting is given for those settings that require a prefix.
	 * @param theSettingPrefix the prefix to add to settings that have requested a prefix
	 * @param theClass the class that has annotations outlining the settings desired
	 * @return an instance of the class with the settings loaded and set 
	 */
	public <T> T getValues( String theSettingPrefix, Class<T> theClass ) {
		Preconditions.checkNotNull( theClass, "Cannot get values for a null class." );
		return getValues( theClass, theSettingPrefix, null );
	}


	/**
	 * This method will get a collection of settings, where the individual names
	 * for the collections are identified by the setting in the string <code>theName</code>.
	 * The list of names is loaded and then for each name, settings identified by 
	 * by annotations on the specified class will loaded, placed into an instance of the
	 * class and then the class will be placed in the collection to be returned.
	 * @param theName the name of the setting that contains a list of collection names to load
	 * @param theClass the class that has annotations outlining the settings desired
	 * @return the collection of instances of the specified class
	 */
	public <T> RegisteredCollection<T> getCollectionValues( String theName, Class<T> theClass ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Cannot get values with a null or empty setting name." );
		Preconditions.checkNotNull( theClass, "Cannot get values for setting '%s' using a null class.", theName );
		
		List<String> collectionNames = getListValue( theName, String.class );
		return _getValues( theName, theClass, collectionNames, null );
	}
	
	/**
	 * This method will get a collection of settings, where the individual names
	 * for the collections are identified by the setting in the string <code>theName</code>.
	 * The list of names is loaded and then for each name, settings identified by 
	 * by annotations on the specified class will loaded, placed into an instance of the
	 * class and then the class will be placed in the collection to be returned.
	 * @param theName the name of the setting that contains a list of collection names to load
	 * @param theClass the class that has annotations outlining the settings desired
	 * @param theDefaultCollectionNames if the setting doesn't exist, this is the default set of collection names to use
	 * @return the collection of instances of the specified class
	 */
	public <T> RegisteredCollection<T> getCollectionValues( String theName, Class<T> theClass, List<String> theDefaultCollectionNames ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Cannot get values with a null or empty setting name." );
		Preconditions.checkNotNull( theClass, "Cannot get values for setting '%s' using a null class.", theName );
		
		List<String> collectionNames = getListValue( theName, String.class, theDefaultCollectionNames );
		return _getValues( theName, theClass, collectionNames, null );
	}
	
	/**
	 * Internal helper method for loading a collection of settings.
	 * @param theName the name of the setting that was used to get <code>theCollectionNames</code>
	 * @param theClass the class that has annotations outlining the settings desired
	 * @param theCollectionNames the collection names, previously loaded
	 * @return the collection of instances of the specified class
	 */
	private <T> RegisteredCollection<T> _getValues( String theName, Class<T> theClass, List<String> theCollectionNames, SettingField theField ) {
		RegisteredCollection<T> registeredCollection = new RegisteredCollection<>(); 
		// okay so now we have list of collection names, which will be used, most likely, as parameter names
		if( theCollectionNames != null ) {
			for( String collectionName : theCollectionNames ) {
				if( registeredCollection.contains( collectionName ) ) {
					if( theField == null ) {
						throw new ConfigurationException( String.format( "Setting name '%s' is attempting to add the collection '%s' more than once.", theName, collectionName ) );
					} else {
						throw new ConfigurationException( String.format( "Field '%s.%s' via setting name '%s' is attempting to add the collection '%s' more than once.", theField.getContainingType().getName(), theField.getSite().getName( ), theName, collectionName ) );
					}
				} else {
					registeredCollection.register( 
							collectionName, 
							// so we get the values (from ourselves, the configuration manager, for the type that 
							// was created for the registered collection), but we send in the name of collection 
							getValues( theClass, null, collectionName ) );
				}
			}
		}
		return registeredCollection;
	}
	
	/**
	 * This is an internal version of the convenience mechanism that will load all settings
	 * as identified by the annotations on field members in a class. This version takes
	 * an optional parameterized name, which is sent if a SettingCollection annotation
	 * was placed on a collection indicating there is a group of types to load.  
	 * @param theClass the class that has annotations outlining the settings desired
	 * @param theCollectionName the name for the collection of settings, used for generating the string Setting name
	 * @return an instance of the class with the settings loaded and set 
	 */
	@SuppressWarnings( "unchecked" )
	private <T> T getValues( Class<T> theClass, String theSettingPrefix, String theCollectionName ) {
		SettingType typeDescriptor = settingTypeManager.generateType( new JavaType( theClass ) );

		T instance = ( T )typeDescriptor.newInstance();
		Object value;
		
		// we iterate through all fields that were annotated
		// to get the methods needed to load the setting from
		// the configuration source (calls the one of the 
		// above getXXXValue methods)
		for( SettingField field : typeDescriptor.getFields( ) ) {
			
			// we warn if we have a collection name BUT the field doesn't have a 
			// parameterized name since it could be the person putting together
			// the setting collection forgot to put a parameter in the setting name
			// this warning isn't needed for the settings name based field 
			if( !field.containNameParameter() && !Strings.isNullOrEmpty( theCollectionName ) && !field.isSettingsName( ) ) {
				logger.warn( "Field '{}.{}' doesn't have a parameterized name even though it is contained inside a collection named '{}'.", typeDescriptor.getType().getName(), field.getSite().getName( ), theCollectionName );
			}
			
			// the generate name call will throw an exception if the field has
			// a parameterized name, but a collection name wasnt' given for the
			// parameter
			String fieldName = field.generateName( theSettingPrefix, theCollectionName );
			if( !settingTypeManager.isValidSettingName( fieldName ) ) {
				throw new ConfigurationException( String.format( "The field name '%s' on '%s.%s' did not conform the field name validator.", fieldName, typeDescriptor.getType().getName(), field.getSite().getName( ) ) );
			}
			try {
				// depending on the type (object, collection, map) we do different things
				if( field.isSettingsCollection( ) ) {
					// so we can assume it is a list of strings and that we can use those
					// strings to get to particular object types, which we will then load
					// and place into a registered collection
					List<String> collectionNames;
					
					// we directly call the list value here BUT field.getSettingMethod( ) will return the config manager's list methods as well
					if( field.isRequired( ) ) {
						collectionNames = getListValue( fieldName, String.class );
					} else {
						collectionNames = getListValue( fieldName, String.class, ( List<String> )field.getDefaultValue( ) );
					}
					// okay so now we have list of collection names, which will be used, most likely, as parameter names
					value = _getValues( 
								fieldName,
								field.getValueTypes( ).get( 0 ).getType().getUnderlyingClass(),
								collectionNames,
								field );
					
				} else if( field.isSettingsName( ) ) {
					// so we this field is where we place the name for the settings collection
					// it does mean that the collection name cannot be missing
					if( Strings.isNullOrEmpty( theCollectionName ) ) {
						throw new ConfigurationException( String.format( "Field '%s.%s' is marked to hold the settings name however the name isn't available.", typeDescriptor.getType().getName(), field.getSite().getName( ) ) );
					} else {
						value = theCollectionName;
					}
					
					
				} else if( field.isObject( ) ) {
					// we have a simple standard object
					if( field.isRequired( ) ) {
						value = field.getSettingMethod().invoke( this, fieldName );
					} else {
						value = field.getSettingMethod().invoke( this, fieldName, field.getDefaultValue( ) );
					}

				} else if( field.isCollection( ) ) {
					// we have a simple list/collection
					if( field.isRequired( ) ) {
						value = field.getSettingMethod().invoke( this, fieldName, field.getValueTypes().get( 0 ).getType().getUnderlyingClass() );
					} else {
						value = field.getSettingMethod().invoke( this, fieldName, field.getValueTypes().get( 0 ).getType().getUnderlyingClass(), field.getDefaultValue( ) );
					}

				} else if( field.isMap( ) ) {
					// we have a map of some kind
					if( field.isRequired( ) ) {
						value = field.getSettingMethod().invoke( this, fieldName, field.getKeyTypes().get( 0 ).getType().getUnderlyingClass(), field.getValueTypes().get( 0 ).getType().getUnderlyingClass() );
					} else {
						value = field.getSettingMethod().invoke( this, fieldName, field.getKeyTypes().get( 0 ).getType().getUnderlyingClass(), field.getValueTypes().get( 0 ).getType().getUnderlyingClass(), field.getDefaultValue( ) );				
					}
				
				} else {
					throw new ConfigurationException( String.format( "Setting '%s' from '%s.%s' is not marked as an object, collection or map.", fieldName, typeDescriptor.getType().getName(), field.getSite().getName( ) ) );
				}
			} catch( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				throw new ConfigurationException( String.format( "Unable to set data from '%s' onto '%s.%s'.", fieldName, typeDescriptor.getType().getName(), field.getSite().getName( ) ), e );
			}
			// the field data is now loaded, so we c
			// we set the data on the class instance
			field.setData( instance, value );
		}
		
		// we call the deserialization and validation 
		typeDescriptor.callDeserializedHook( instance );
		typeDescriptor.callValidationHook( instance );
		
		return instance;
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
