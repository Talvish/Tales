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

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;

import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.json.translators.JsonElementToBooleanTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToDateTimeTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToDoubleTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToFloatTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToIntegerTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToListTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToLongTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToMapTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToStringTranslator;

/**
 * This is a helper class that will generate the initial configuration setting value.
 * @author jmolnar
 *
 */
public final class JsonValueHelper {
	private static final Map< Class<?>, Translator> translators = new HashMap<Class<?>, Translator>( );
	
	static {
		translators.put( Integer.class, new JsonElementToIntegerTranslator( null ) );
		translators.put( int.class, new JsonElementToIntegerTranslator( null ) );

		translators.put( Long.class, new JsonElementToLongTranslator( null ) );
		translators.put( long.class, new JsonElementToLongTranslator( null ) );

		translators.put( Float.class, new JsonElementToFloatTranslator( null ) );
		translators.put( float.class, new JsonElementToFloatTranslator( null ) );

		translators.put( Double.class, new JsonElementToDoubleTranslator( null ) );
		translators.put( double.class, new JsonElementToDoubleTranslator( null ) );

		translators.put( Boolean.class, new JsonElementToBooleanTranslator( null ) ); 
		translators.put( boolean.class, new JsonElementToBooleanTranslator( null ) );

		translators.put( DateTime.class, new JsonElementToDateTimeTranslator( null ) );
		translators.put( String.class, new JsonElementToStringTranslator( null ) );
	}

	/**
	 * Gets a translator for a type, if available.
	 * @param theType the type to get a translator for 
	 * @return the translator or null if not found
	 */
	public static Translator getTranslator( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "Need a type to get a translator." );
		return translators.get( theType );
	}
	
	/**
	 * Generates a setting from a json element as the required type. 
	 * It will throw exceptions if the string cannot be converted.
	 * @param theName the name of the value 
	 * @param theValue the value to be translated
	 * @param theDescription the description for the setting
	 * @param isSensitive indicates if this is a private setting
	 * @param theSource the name given to the source of the value
	 * @param theType the type to convert the string value into
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public static LoadedSetting generateValue( String theName, JsonElement theValue, String theDescription, boolean isSensitive, String theSource, Class<?> theType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name cannot be null or empty.");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ), "Source cannot be null or empty.");
		Preconditions.checkNotNull( theType, "Need a type to be able to translate." );
		
		Translator translator = translators.get( theType );
		Preconditions.checkState( translator != null, "Type '%s' did not have a translator.", theType.getName() );

		// we want the string value, but it can technically fail the translation, so we put some precautions around it
		String stringValue = theValue.toString( );  
		// now for the real translation/setting creation
		try {
			return new LoadedSetting( theName, translator.translate( theValue ), stringValue, theDescription, isSensitive, theSource );
		} catch( TranslationException e ) {
			throw new ConfigurationException( String.format( "'%s' had a value of '%s' that could not be translated into a '%s'.", theName, stringValue, theType.getName( ) ), e );
		}
	}
	
	
	/**
	 * Generates a setting from a json element as a list of a specific type. 
	 * It will throw exceptions if the string cannot be converted.
	 * @param theName the name of the value 
	 * @param theValue the value to be translated
	 * @param theDescription the description for the setting
	 * @param isSensitive indicates if this is a private setting
	 * @param theSource the name given to the source of the value
	 * @param theElementType the type of the element 
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public static LoadedSetting generateList( String theName, JsonElement theValue, String theDescription, boolean isSensitive, String theSource, Class<?> theElementType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name cannot be null or empty.");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ), "Source cannot be null or empty.");
		Preconditions.checkNotNull( theElementType, "Need a type to be able to translate." );
		
		Translator elementTranslator = translators.get( theElementType );
		Preconditions.checkState( elementTranslator != null, "Element type '%s' did not have a translator.", theElementType.getName( ) );
		
		// NOTE: we generate this on the fly, we could store create items for lookup later 
		JsonElementToListTranslator collectionTranslator = new JsonElementToListTranslator( elementTranslator, null );

		// we want the string value, but it can technically fail the translation, so we put some precautions around it
		String stringValue = theValue.toString( );  
		// now for the real translation/setting creation
		try {
			return new LoadedSetting( theName, collectionTranslator.translate( theValue ), stringValue, theDescription, isSensitive, theSource );
		} catch( TranslationException e ) {
			throw new ConfigurationException( String.format( "'%s' had a value of '%s' that could not be translated into a list of '%s'.", theName, stringValue, theElementType.getName( ) ), e );
		}
	}

	
	/**
	 * Generates a setting from a json element as a map of the specific key and value types. 
	 * It will throw exceptions if the string cannot be converted.
	 * @param theName the name of the value 
	 * @param theValue the value to be translated
	 * @param theDescription the description for the setting
	 * @param isSensitive indicates if this is a private setting
	 * @param theSource the name given to the source of the value
	 * @param theKeyType the type of the key 
	 * @param theValueType the type of the value 
	 * @return a configuration setting generated for value, if found, null otherwise
	 */
	public static LoadedSetting generateMap( String theName, JsonElement theValue, String theDescription, boolean isSensitive, String theSource, Class<?> theKeyType, Class<?> theValueType ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "Name cannot be null or empty.");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theSource ), "Source cannot be null or empty.");
		Preconditions.checkNotNull( theKeyType, "Need a key type to be able to translate." );
		Preconditions.checkNotNull( theValueType, "Need a value type to be able to translate." );
		
		Translator keyTranslator = StringValueHelper.getTranslator( theKeyType ); // the key translator uses the standard string translators because json only supports strings for key types
		Translator valueTranslator = translators.get( theValueType );
		Preconditions.checkState( keyTranslator != null, "Key type '%s' did not have a translator.", theKeyType.getName( ) );
		Preconditions.checkState( valueTranslator != null, "Value type '%s' did not have a translator.", theValueType.getName( ) );
		
		// NOTE: we generate this on the fly, we could store create items for lookup later 
		JsonElementToMapTranslator collectionTranslator = new JsonElementToMapTranslator( keyTranslator, valueTranslator, null );

		// we want the string value, but it can technically fail the translation, so we put some precautions around it
		String stringValue = theValue.toString( );  
		// now for the real translation/setting creation
		try {
			return new LoadedSetting( theName, collectionTranslator.translate( theValue ), stringValue, theDescription, isSensitive, theSource );
		} catch( TranslationException e ) {
			throw new ConfigurationException( String.format( "'%s' had a value of '%s' that could not be translated into a map of '[%s,%s]'.", theName, stringValue, theKeyType.getName( ), theValueType.getName( ) ), e );
		}
	}
}
