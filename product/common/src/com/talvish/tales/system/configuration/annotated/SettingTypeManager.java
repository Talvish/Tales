// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.system.configuration.annotated;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.naming.NopValidator;
import com.talvish.tales.parts.naming.SegmentedLowercaseValidator;
import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.FieldSite;
import com.talvish.tales.serialization.SerializationTypeManager;
import com.talvish.tales.system.configuration.ConfigurationException;
import com.talvish.tales.system.configuration.ConfigurationManager;

/**
 * Manager class that creates type descriptors representing classes and fields that 
 * are used for configuration settings.
 * @author jmolnar
 *
 */
public class SettingTypeManager extends SerializationTypeManager< SettingType, SettingField> {
	public static final String CONTRACT_TYPE_NAME_VALIDATOR = "tales.contracts.config_type_name";
	public static final String CONTRACT_MEMBER_PARAMETERIZED_NAME_VALIDATOR = "tales.contracts.config_member_parameterized_name";
	public static final String CONTRACT_MEMBER_NAME_VALIDATOR = "tales.contracts.config_member_name";
	
	static {
		if( !NameManager.hasValidator( SettingTypeManager.CONTRACT_TYPE_NAME_VALIDATOR ) ) {
			NameManager.setValidator( SettingTypeManager.CONTRACT_TYPE_NAME_VALIDATOR, new NopValidator( ) ); // we don't care about validation for this
		}
		if( !NameManager.hasValidator( SettingTypeManager.CONTRACT_MEMBER_PARAMETERIZED_NAME_VALIDATOR ) ) {
			NameManager.setValidator( SettingTypeManager.CONTRACT_MEMBER_PARAMETERIZED_NAME_VALIDATOR, new SegmentedLowercaseParameterizedValidator( ) );
		}
		if( !NameManager.hasValidator( SettingTypeManager.CONTRACT_MEMBER_NAME_VALIDATOR ) ) {
			NameManager.setValidator( SettingTypeManager.CONTRACT_MEMBER_NAME_VALIDATOR, new SegmentedLowercaseValidator( ) );
		}
	}
	
	private Map<Class<?>,Method> managerRequiredMethods = new HashMap<>( );
	private Map<Class<?>,Method> managerNotRequiredMethods = new HashMap<>( );
	
	private Method mapRequiredMethod;
	private Method mapNotRequiredMethod;
	
	private Method listRequiredMethod;
	private Method listNotRequiredMethod;


    public SettingTypeManager( ) {
    	this( null, null, null );
    }
    
    // TODO: consider not sending field related items to parent
    public SettingTypeManager( NameValidator theTypeNameValidator, NameValidator theMemberNameValidator, NameValidator theMemberParameterizedNameValidator ) {
    	super(
    			Settings.class,
    			theTypeNameValidator == null ? NameManager.getValidator( SettingTypeManager.CONTRACT_TYPE_NAME_VALIDATOR ) : theTypeNameValidator,
    			Setting.class,
    			theMemberNameValidator == null ? NameManager.getValidator( SettingTypeManager.CONTRACT_MEMBER_PARAMETERIZED_NAME_VALIDATOR ) : theMemberNameValidator );
    	setupConfigurationMethods( );
    }

	@Override
	protected SettingType instantiateTypeDescriptor(
			String theTypeName,
			JavaType theType, 
			Method theDeserializationHook,
			boolean supportsValid, 
			SettingType theBaseTypeDescriptor) {
		return new SettingType ( 
				theTypeName, 
				theType, 
				theDeserializationHook, 
				supportsValid, 
				theBaseTypeDescriptor );
	}
	
	/**
	 * Verifies that the name given is correct where any parameter has been replaced.
	 * @param theName the full proper name to verify
	 * @return true if it is valid, false otherwise
	 */
	public boolean isValidFieldName( String theName ) {
		return NameManager.getValidator( SettingTypeManager.CONTRACT_MEMBER_NAME_VALIDATOR ).isValid( theName );
	}

    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theField the field we are to look at 
     * @param theDeclaringType the type the field is a member of
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	@Override
	protected SettingField generateField( Field theField, SettingType theDeclaringType, Object aDeclaringInstance ) {
		SettingField fieldDescriptor = null;
        // if we have serialization declaration we will save it
        if( theField.isAnnotationPresent( Setting.class ) ) {
        	// first we see if have the setting annotation on the field
            Setting fieldAnnotation = theField.getAnnotation( Setting.class );

            Class<?> fieldClass = theField.getType();
			Type fieldGenericType = theField.getGenericType();
			FieldSite fieldSite = new FieldSite( theDeclaringType.getType( ).getType(), theField ); // we use this constructor to ensure we get fields that use type parameters
			
			boolean fieldRequired = fieldAnnotation.required();
			Object fieldDefaultValue = fieldSite.getData( aDeclaringInstance );
			
	        // make sure the field is accessible
	        theField.setAccessible( true ); 
	        // get the proper names for the field
	        String fieldName = Strings.isNullOrEmpty( fieldAnnotation.name( ) ) ? theField.getName( ) : fieldAnnotation.name( );
	        
	        // and verify the name of the field is valid
	        if( !memberNameValidator.isValid( fieldName ) ) {
        		throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", theDeclaringType.getType().getName(), fieldSite.getName(), fieldName, memberNameValidator.getClass().getSimpleName() ) );
	        }

			if( Map.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) {
				// first check to see if it is a map and if it is something else, throw an exception
				if( !Map.class.equals( fieldClass ) ) {
					throw new ConfigurationException( String.format( "Field '%s.%s' is of type '%s', which is a kind of map, but it must be a standard Java Map class.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), fieldClass.getSimpleName( ) ) );
				} else {
					// first we deal with the keys
					JavaType declaredKeyType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
		            // next we deal with the values
		    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 1 ] );

		    		Method configMethod;
		    		
		    		if( fieldRequired ) {
		    			configMethod = mapRequiredMethod;
		    		} else {
		    			configMethod = mapNotRequiredMethod;
		    		}

		    		if( !this.isSupportedSimpleType( declaredKeyType.getUnderlyingClass( ) ) ) {
						throw new ConfigurationException( String.format( "Field '%s.%s' has an unsupported map key type of '%s'.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), declaredKeyType.getUnderlyingClass( ).getSimpleName( ) ) );
		    		} else if( !this.isSupportedSimpleType( declaredValueType.getUnderlyingClass( ) ) ) {
						throw new ConfigurationException( String.format( "Field '%s.%s' has an unsupported map value type of '%s'.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), declaredValueType.getUnderlyingClass( ).getSimpleName( ) ) );
		    		} else {
			            fieldDescriptor = new SettingField( 
			            		fieldName, 
			            		new ValueType<>( declaredKeyType, null ), 
			            		new ValueType<>( declaredValueType, null ), 
			            		fieldSite, 
			            		theDeclaringType, 
			            		theDeclaringType,
			            		fieldRequired,
			            		fieldDefaultValue,
			            		configMethod );
		    		}
				}
			
			} else if( ( Collection.class.isAssignableFrom( fieldClass ) && ( fieldGenericType instanceof ParameterizedType ) ) ) {
				// check to see if it is a collection or a list and if not, then thrown an exception
				if( !Collection.class.equals( fieldClass ) && !List.class.equals( fieldClass ) ) {
					throw new ConfigurationException( String.format( "Field '%s.%s' is of type '%s', which is a kind of collection/list, but it must be a standard Java Collection or List class.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), fieldClass.getSimpleName( ) ) );
				} else {
					// if we have a collection (e.g list, set, collection itself, etc)
					// we need to get the type information for the collection element
		    		JavaType declaredValueType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );

		    		Method configMethod;
		    		
		    		if( fieldRequired ) {
		    			configMethod = listRequiredMethod;
		    		} else {
		    			configMethod = listNotRequiredMethod;
		    		}
		    		
		    		if( !this.isSupportedSimpleType( declaredValueType.getUnderlyingClass( ) ) ) {
						throw new ConfigurationException( String.format( "Field '%s.%s' has an unsupported list value type of '%s'.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), declaredValueType.getUnderlyingClass( ).getSimpleName( ) ) );
		    		} else {
			            // now create the field object we need
			            fieldDescriptor = new SettingField( 
			            		fieldName, 
			            		FieldDescriptor.FieldValueType.COLLECTION, 
			            		new ValueType<>( declaredValueType, null ), 
			            		fieldSite, 
			            		theDeclaringType, 
			            		theDeclaringType,
			            		fieldRequired,
			            		fieldDefaultValue,
			            		configMethod );		    			
		    		}
				}

	    	} else if( fieldClass.isArray( ) ) {
				throw new ConfigurationException( String.format( "Field '%s.%s' is of type '%s' but arrays are not supported for configuration.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), fieldClass.getSimpleName( ) ) );

	    	} else {
	    		// and we need to figure out if we can manage the types and which config manager method to use
	    		Method configMethod = getSimpleConfigMethod( fieldClass, fieldRequired );

	    		// we have either a simple type, primitive type, enum or non-collection complex type
	    		fieldDescriptor = new SettingField( 
	            		fieldName, 
	            		FieldDescriptor.FieldValueType.OBJECT, 
	            		new ValueType<>( fieldSite.getType(), null ), 
	            		fieldSite, 
	            		theDeclaringType, 
	            		theDeclaringType,
	            		fieldRequired,
	            		fieldDefaultValue,
	            		configMethod); 
	    	}
			
    	} else if( theField.isAnnotationPresent( SettingCollection.class ) ) {
        	// now we see if have the collection annotation on the field
    		SettingCollection fieldAnnotation = theField.getAnnotation( SettingCollection.class );

            Class<?> fieldClass = theField.getType();
			Type fieldGenericType = theField.getGenericType();
			FieldSite fieldSite = new FieldSite( theDeclaringType.getType( ).getType(), theField ); // we use this constructor to ensure we get fields that use type parameters			
			boolean fieldRequired = fieldAnnotation.required();

			// make sure the field is accessible
	        theField.setAccessible( true ); 
	        // get the proper names for the field
	        String fieldName = Strings.isNullOrEmpty( fieldAnnotation.name( ) ) ? theField.getName( ) : fieldAnnotation.name( );

	        // and verify the name of the field is valid
	        if( !memberNameValidator.isValid( fieldName ) ) {
        		throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", theDeclaringType.getType().getName(), fieldSite.getName(), fieldName, memberNameValidator.getClass().getSimpleName() ) );
	        }
			
			// check the type
			if( !RegisteredCollection.class.equals( fieldClass ) ) {
				throw new ConfigurationException( String.format( "Field '%s.%s' is of type '%s', which is not supported as a SettingCollection.", theDeclaringType.getType( ).getUnderlyingClass().getSimpleName(), theField.getName(), fieldClass.getSimpleName( ) ) );
			} else {
				// now we pull out the class parameter 
				JavaType declaredSettingType = new JavaType( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
				SettingType settingType = this.generateType( declaredSettingType ); // we dont' seen in an instance because this is a different type
				
				List<String> defaults = fieldAnnotation.defaults() == null ? new ArrayList<>( ) : Arrays.asList( fieldAnnotation.defaults( ) );

	    		Method configMethod;
	    		
	    		if( fieldRequired ) {
	    			configMethod = listRequiredMethod;
	    		} else {
	    			configMethod = listNotRequiredMethod;
	    		}
				
	    		fieldDescriptor = new SettingField( 
	            		fieldName, 
	            		FieldDescriptor.FieldValueType.COLLECTION, 
	            		new ValueType<>( declaredSettingType, settingType ), 
	            		fieldSite, 
	            		theDeclaringType, 
	            		theDeclaringType,
	            		fieldRequired,
	            		defaults,	
	            		configMethod );				
			}
    	}
        return fieldDescriptor;
	}
	
	/**
	 * Helpers method to indicate if the type is one of the simple types
	 * that the configuration system supports.
	 * @param theClass the class to check for
	 * @return true if support, false otherwise
	 */
	private boolean isSupportedSimpleType( Class<?> theClass ) {
		return managerRequiredMethods.containsKey( theClass );
	}
	
	/**
	 * Returns the method off the ConfigurationManager to use for 
	 * getting a setting for a particular type.
	 * @param theClass the type of the setting
	 * @param isRequired true indicates the setting is expected in the configuration source, false means a default will be used
	 * @return the Method from the ConfigurationManager
	 */
	private Method getSimpleConfigMethod( Class<?> theClass, boolean isRequired ) {
		if( isRequired ) {
			return managerRequiredMethods.get( theClass );
		} else {
			return managerNotRequiredMethods.get( theClass );
		}
	}
	
	/**
	 * Constructor helper method that setups the mapping of types to the method
	 * used to get that type of setting from the configuration manager.
	 */
	private void setupConfigurationMethods( ) {
		try {
			// so the setup for the manager is making sure we can get the right methods to call
			Class<?> managerType = ConfigurationManager.class;
		
			// first the simple types
			
			managerRequiredMethods.put( String.class, managerType.getMethod( "getStringValue", String.class ) );
			managerNotRequiredMethods.put( String.class, managerType.getMethod( "getStringValue", String.class, String.class ) );

			managerRequiredMethods.put( Integer.class, managerType.getMethod( "getIntegerValue", String.class ) );
			managerNotRequiredMethods.put( Integer.class, managerType.getMethod( "getIntegerValue", String.class, Integer.class ) );
			managerRequiredMethods.put( int.class, managerType.getMethod( "getIntegerValue", String.class ) );
			managerNotRequiredMethods.put( int.class, managerType.getMethod( "getIntegerValue", String.class, Integer.class ) );

			managerRequiredMethods.put( Long.class, managerType.getMethod( "getLongValue", String.class ) );
			managerNotRequiredMethods.put( Long.class, managerType.getMethod( "getLongValue", String.class, Long.class ) );
			managerRequiredMethods.put( long.class, managerType.getMethod( "getLongValue", String.class ) );
			managerNotRequiredMethods.put( long.class, managerType.getMethod( "getLongValue", String.class, Long.class ) );

			managerRequiredMethods.put( Float.class, managerType.getMethod( "getFloatValue", String.class ) );
			managerNotRequiredMethods.put( Float.class, managerType.getMethod( "getFloatValue", String.class, Float.class ) );
			managerRequiredMethods.put( float.class, managerType.getMethod( "getFloatValue", String.class ) );
			managerNotRequiredMethods.put( float.class, managerType.getMethod( "getFloatValue", String.class, Float.class ) );

			managerRequiredMethods.put( Double.class, managerType.getMethod( "getDoubleValue", String.class ) );
			managerNotRequiredMethods.put( Double.class, managerType.getMethod( "getDoubleValue", String.class, Double.class ) );
			managerRequiredMethods.put( double.class, managerType.getMethod( "getDoubleValue", String.class ) );
			managerNotRequiredMethods.put( double.class, managerType.getMethod( "getDoubleValue", String.class, Double.class ) );

			managerRequiredMethods.put( Boolean.class, managerType.getMethod( "getBooleanValue", String.class ) );
			managerNotRequiredMethods.put( Boolean.class, managerType.getMethod( "getBooleanValue", String.class, Boolean.class ) );
			managerRequiredMethods.put( boolean.class, managerType.getMethod( "getBooleanValue", String.class ) );
			managerNotRequiredMethods.put( boolean.class, managerType.getMethod( "getBooleanValue", String.class, Boolean.class ) );

			managerRequiredMethods.put( DateTime.class, managerType.getMethod( "getDateTimeValue", String.class ) );
			managerNotRequiredMethods.put( DateTime.class, managerType.getMethod( "getDateTimeValue", String.class, DateTime.class ) );

			
			// now the more list/map types 
			listRequiredMethod = managerType.getMethod( "getListValue", String.class, Class.class );
			listNotRequiredMethod= managerType.getMethod( "getListValue", String.class, Class.class, List.class );
    		
			mapRequiredMethod = managerType.getMethod( "getMapValue", String.class, Class.class, Class.class );
    		mapNotRequiredMethod= managerType.getMethod( "getMapValue", String.class, Class.class, Class.class, Map.class );

		} catch( NoSuchMethodException | SecurityException e ) {
			throw new IllegalStateException( "Unable to setup the type-to-configuration-manager-method' map due to a problem", e );
		}

	}
}
