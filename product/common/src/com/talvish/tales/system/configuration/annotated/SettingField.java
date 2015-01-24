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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.reflection.FieldDescriptor;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.MemberSite;
import com.talvish.tales.serialization.SerializationField;
import com.talvish.tales.system.Conditions;

/**
 * This class represents a field that is a setting for configuration.
 * @author jmolnar
 */
public class SettingField extends SerializationField<SettingType, SettingField> {
	private static final Pattern PARAM_PATTERN = Pattern.compile( SegmentedLowercaseParameterizedValidator.PARAM_REGEX );
	private static final Pattern PREFIX_PATTERN = Pattern.compile( SegmentedLowercaseParameterizedValidator.PREFIX_REGEX );
	
	private final boolean required;
	private final Object defaultValue;
	private final Method settingMethod;
	
	private final String nameFormat;
	private final boolean hasNameParameter;
	private final boolean hasPrefixParameter;
	
	
	private final boolean settingsCollection;	// TODO: consider putting an enum for these settings related guys
	private final boolean settingsName;
	
    /**
     * Primary constructor used to create a field that is most things BUT a map.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     * @param isRequired indicates if the the field is required in the underlying source
     * @param theDefaultValue the default value to use, particularly if the required is false
     */
    protected SettingField( 
    		String theName, 
    		FieldDescriptor.FieldValueType theFieldValueType,
    		ValueType<SettingType, SettingField> theObjectType, 
    		MemberSite theFieldSite, 
    		SettingType theDeclaringType, 
    		SettingType theContainingType,
    		boolean isRequired,
    		Object theDefaultValue,
    		Method theSettingMethod ) {
    	super( 
    			theName, 
    			theFieldValueType, 
    			Arrays.asList( theObjectType ), 
    			theFieldSite, 
    			theDeclaringType, 
    			theContainingType );
    	Preconditions.checkNotNull( theSettingMethod, "Setting '%s' was not given a setting method to use.", theName );

    	// keep default values, etc
    	required = isRequired;
    	defaultValue = theDefaultValue;
    	settingMethod = theSettingMethod;
    	
    	// now we want to verify the name in question for 
    	// a parameter and if so mark we have one
    	nameFormat = generateNameFormat( name );
    	hasPrefixParameter = nameFormat.contains( "%1$s" );	// if we have the replacement variable, we know it contains a prefix parameter
    	hasNameParameter = nameFormat.contains( "%2$s" ); 	// if we have the replacement variable, we know it contains a name parameter
    	
    	settingsCollection = this.site.getType().getUnderlyingClass().equals( RegisteredCollection.class );
    	settingsName = false;
    }

    /**
     * Primary constructor used to create a map.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theKeyType the type of the key for the map
     * @param theValueType the type of the value for the map
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     * @param isRequired indicates if the the field is required in the underlying source
     * @param theDefaultValue the default value to use, particularly if the required is false
     */
    protected SettingField( 
    		String theName, 
    		ValueType<SettingType, SettingField> theKeyType, 
    		ValueType<SettingType, SettingField> theValueType, 
    		MemberSite theFieldSite, 
    		SettingType theDeclaringType, 
    		SettingType theContainingType,
    		boolean isRequired,
    		Object theDefaultValue,
    		Method theSettingMethod ) {
    	super( 
    			theName, 
    			Arrays.asList( theKeyType ), 
    			Arrays.asList( theValueType ), 
    			theFieldSite, 
    			theDeclaringType, 
    			theContainingType );
    	Preconditions.checkNotNull( theSettingMethod, "Setting '%s' was not given a setting method to use.", theName );
    	
    	required = isRequired;
    	defaultValue = theDefaultValue;
    	settingMethod = theSettingMethod;

    	// now we want to verify the name in question for 
    	// a parameter and if so mark we have one
    	nameFormat = generateNameFormat( name );
    	hasPrefixParameter = nameFormat.contains( "%1$s" );	// if we have the replacement variable, we know it contains a prefix parameter
    	hasNameParameter = nameFormat.contains( "%2$s" ); 	// if we have the replacement variable, we know it contains a name parameter
    	
    	settingsCollection = this.site.getType().getUnderlyingClass().equals( RegisteredCollection.class );
    	settingsName = false;
    }

    /**
     * This constructor is for making a settings name base field. Meaning a field that represents
     * the name for a group of settings.
     * @param theName the name to give the field
     * @param theFieldType the type information regarding the field
     * @param theFieldSite the site information for modifying the field
     * @param theDeclaringType the contract type this field was declared in
     * @param theContainingType the class that currently contains the field, which, if not the same as theDeclaringType is a subclass
     */
    protected SettingField( 
    		String theName, 
    		FieldDescriptor.FieldValueType theFieldValueType,
    		ValueType<SettingType, SettingField> theObjectType, 
    		MemberSite theFieldSite, 
    		SettingType theDeclaringType, 
    		SettingType theContainingType ) {
    	super( 
    			theName, 
    			theFieldValueType, 
    			Arrays.asList( theObjectType ), 
    			theFieldSite, 
    			theDeclaringType, 
    			theContainingType );

    	required = false; // this is not meaningful for this type
    	defaultValue = null;
    	settingMethod = null;
    	
    	// now we want to verify the name in question for 
    	// a parameter and if so mark we have one
    	nameFormat = generateNameFormat( name ); // we do this BUT not much point, since there isn't a parameter, the name is hard-coded
    	hasPrefixParameter = nameFormat.contains( "%1$s" );	// if we have the replacement variable, we know it contains a prefix parameter
    	hasNameParameter = nameFormat.contains( "%2$s" ); 	// if we have the replacement variable, we know it contains a name parameter
    	
    	settingsCollection = false;
    	settingsName = true;
    }
    
    /**
     * Constructor helper method that creates a name format that can be used to 
     * @param theName the name given to the setting to check for parameters
     * @return the format to use for name generation
     */
    private String generateNameFormat( String theName ) {
    	String generatedName;

    	Matcher matcher;

    	// the code below doesn't do a replace in part because it needs to
    	// look for multiple uses of the pattern and fail if there are any
    	
    	// first we look for the collection name parameter
    	matcher= PARAM_PATTERN.matcher( theName );

    	if( matcher.find() ) {
    		// we need the start-stop index
    		generatedName = theName.substring( 0, matcher.start( ) ) + "%2$s";
    		if( matcher.end( ) < theName.length( ) ) {
    			generatedName += theName.substring( matcher.end( ) );
    		}
        	Conditions.checkConfiguration( !matcher.find( ), "Setting '%s' for '%s.%s' indicates it wants more than one parameter, which isn't allowed.", this.name, this.containingType.getType().getSimpleName(), this.site.getName( ) );
    	} else {
    		generatedName = theName;
    	}
    	
    	// now we go back and reset to the newly generated name (and generated name gets reset)
    	theName = generatedName;
    	
    	// next we look for the prefix
    	matcher= PREFIX_PATTERN.matcher( theName );
    	
    	if( matcher.find() ) {
    		// we need the start-stop index
    		generatedName = theName.substring( 0, matcher.start( ) ) + "%1$s";
    		if( matcher.end( ) < theName.length( ) ) {
    			generatedName += theName.substring( matcher.end( ) );
    		}
        	Conditions.checkConfiguration( !matcher.find( ), "Setting '%s' for '%s.%s' indicates it wants more than one prefix, which isn't allowed.", this.name, this.containingType.getType().getSimpleName(), this.site.getName( ) );
    	} else {
    		generatedName = theName;
    	}

    	return generatedName;
    }

    /**
     * Indicates if the setting name expects a prefix to be given.
     * @return true if a prefix is expected to be given, false otherwise
     */
    public boolean containsPrefixParameter( ) {
    	return hasPrefixParameter;
    }
    
    /**
     * Indicates if a collection name is expected to be given so a proper setting name can be generated. 
     * @return true if a collection name is expected, false otherwise
     */
    public boolean containNameParameter( ) {
    	return hasNameParameter;
    }
    
    /**
     * Generates the setting name to use for a field.
     * The name is not verified as the proper format by this method, the caller must do that.
     * The 
     * @param theCollectionName the name for the collection of settings, this can be null if a name isn't expected
     * @param thePrefix the prefix string to use, which isn't require unless one wasn't declared and the field expects one
     * @return the generated name
     */
    public String generateName( String thePrefix, String theCollectionName ) {
    	Conditions.checkConfiguration( !hasNameParameter || !Strings.isNullOrEmpty( theCollectionName ), "Setting '%s' for '%s.%s' is expecting a collection name but none was given.", this.name, this.containingType.getType().getSimpleName(), this.site.getName( ) );
    	
    	// we take the prefix as an override, if one is given, instead of using what is on the type
    	String prefix = Strings.isNullOrEmpty( thePrefix ) ? this.containingType.getSettingPrefix() : thePrefix;
    	Conditions.checkConfiguration( !hasPrefixParameter || !Strings.isNullOrEmpty( prefix ), "Setting '%s' for '%s.%s' is expecting a prefix but none was given.", this.name, this.containingType.getType().getSimpleName(), this.site.getName( ) );
    	
    	// verification of this name will occur by the caller
    	// this gets the containing (not declaring) prefix to use allow
    	// overrides to change what the prefix is to use
    	return String.format( nameFormat, prefix, theCollectionName );
    }
    
    /**
     * Indicates whether the setting must be found 
     * in the underlying configuration source.
     * @return true if required in the source, false otherwise
     */
    public boolean isRequired( ) {
    	return required;
    }

    /**
     * Indicates if this setting field represents settings collection.
     * @return true if this field represents some settings collection, false otherwise
     */
    public boolean isSettingsCollection( ) {
    	return settingsCollection;
    }
    
    /**
     * Indicates if this setting field represents the name given to a single
     * group of settings.
     * @return true indicates it is the field that represents the name for a group of settings, false otherwise
     */
    public boolean isSettingsName( ) {
    	return settingsName;
    } 
    
    /**
     * Gets the default value for the setting.
     * @return the default value
     */
    public Object getDefaultValue( ) {
    	return defaultValue;
    }
    
    /**
     * The method on the ConfigurationManager
     * for getting the setting value.
     * @return
     */
    public Method getSettingMethod( ) {
    	return settingMethod;
    }

    /**
     * Clones the existing object but specifying a different current type, which will
     * be a subclass of the original declaring type.
     */
    @Override
    protected SettingField cloneForSubclass( SettingType theContainingType ) {
    	if( this.isMap() ) {
	        return new SettingField( 
	        		this.name,
	        		this.keyTypes.get( 0 ),
	        		this.valueTypes.get( 0 ),
	        		this.site, 
	        		this.declaringType, 
	        		theContainingType,
	        		required,
	        		defaultValue,
	        		settingMethod );
    		
    	} else {
	        return new SettingField( 
	        		this.name,
	        		this.fieldValueType,
	        		this.valueTypes.get( 0 ),
	        		this.site, 
	        		this.declaringType, 
	        		theContainingType,
	        		required,
	        		defaultValue,
	        		settingMethod );
    	}
    }
}
