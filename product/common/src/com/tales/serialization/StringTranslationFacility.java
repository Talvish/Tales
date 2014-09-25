// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.tales.serialization;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.tales.businessobjects.ObjectId;
import com.tales.parts.translators.BooleanToStringTranslator;
import com.tales.parts.translators.EnumToStringTranslator;
import com.tales.parts.translators.ObjectToStringTranslator;
import com.tales.parts.translators.StringToBigDecimalTranslator;
import com.tales.parts.translators.StringToBooleanTranslator;
import com.tales.parts.translators.StringToDateTimeTranslator;
import com.tales.parts.translators.StringToDoubleTranslator;
import com.tales.parts.translators.StringToEnumTranslator;
import com.tales.parts.translators.StringToFloatTranslator;
import com.tales.parts.translators.StringToIntegerTranslator;
import com.tales.parts.translators.StringToLongTranslator;
import com.tales.parts.translators.StringToObjectIdTranslator;
import com.tales.parts.translators.StringToStringTranslator;
import com.tales.parts.translators.StringToUuidTranslator;
import com.tales.parts.translators.Translator;
import com.tales.parts.translators.UuidToStringTranslator;
import com.tales.system.Facility;

/**
 * A helper class for dealing with translating to/from strings in a generic fashion.
 * @author jmolnar
 *
 */
public final class StringTranslationFacility implements Facility {
	private final Map< Class<?>, Translator> toStringTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );
	private final Map< Class<?>, Translator> fromStringTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );

	/**
	 * Default constructor.
	 */
	public StringTranslationFacility( ) {
		// the from string translators . . .
		
		Translator toIntegerTranslator = new StringToIntegerTranslator( true, null, null );
		fromStringTranslators.put( Integer.class, toIntegerTranslator );
		fromStringTranslators.put( int.class, toIntegerTranslator );

		Translator toLongTranslator = new StringToLongTranslator( true, null, null );
		fromStringTranslators.put( Long.class, toLongTranslator );
		fromStringTranslators.put( long.class, toLongTranslator );

		Translator toFloatTranslator = new StringToFloatTranslator( true, null, null );
		fromStringTranslators.put( Float.class, toFloatTranslator );
		fromStringTranslators.put( float.class, toFloatTranslator );

		Translator toDoubleTranslator = new StringToDoubleTranslator( true, null, null );
		fromStringTranslators.put( Double.class, toDoubleTranslator );
		fromStringTranslators.put( double.class, toDoubleTranslator );

		Translator toBigDecimalTranslator = new StringToBigDecimalTranslator( true, null, null );
		fromStringTranslators.put( BigDecimal.class, toBigDecimalTranslator );

		
		Translator toBooleanTranslator = new StringToBooleanTranslator( true, null, null );
		fromStringTranslators.put( Boolean.class, toBooleanTranslator );
		fromStringTranslators.put( boolean.class, toBooleanTranslator );

		Translator toDateTimeTranslator = new StringToDateTimeTranslator( true, null, null );
		fromStringTranslators.put( DateTime.class, toDateTimeTranslator );

		Translator toStringTranslator = new StringToStringTranslator( true, "", null );
		fromStringTranslators.put( String.class, toStringTranslator );
		
		Translator toUUIDTranslator = new StringToUuidTranslator(true, null, null);
		fromStringTranslators.put( UUID.class, toUUIDTranslator );
		
		Translator toObjectIdTranslator = new StringToObjectIdTranslator( true, null, null );
		fromStringTranslators.put( ObjectId.class, toObjectIdTranslator );
		
		// the to string translators . . .
		
		Translator fromObjectTranslator = new ObjectToStringTranslator( "" );
				
		toStringTranslators.put( Integer.class, fromObjectTranslator );
		toStringTranslators.put( int.class, fromObjectTranslator );

		toStringTranslators.put( Long.class, fromObjectTranslator );
		toStringTranslators.put( long.class, fromObjectTranslator );

		toStringTranslators.put( Float.class, fromObjectTranslator);
		toStringTranslators.put( float.class, fromObjectTranslator );

		toStringTranslators.put( Double.class, fromObjectTranslator );
		toStringTranslators.put( double.class, fromObjectTranslator );

		toStringTranslators.put( BigDecimal.class, fromObjectTranslator );

		Translator fromBooleanTranslator = new BooleanToStringTranslator( "" );
		toStringTranslators.put( Boolean.class, fromBooleanTranslator );
		toStringTranslators.put( boolean.class, fromBooleanTranslator );

		toStringTranslators.put( DateTime.class, fromObjectTranslator );

		Translator fromStringTranslator = new StringToStringTranslator( true, "", null );
		toStringTranslators.put( String.class, fromStringTranslator );
		
		Translator fromUUIDTranslator = new UuidToStringTranslator( "" );
		toStringTranslators.put( UUID.class, fromUUIDTranslator );
		
		toStringTranslators.put( ObjectId.class,  fromObjectTranslator );
		
	}
	

	/***
	 * This method is used to add translators into the manager for ensuring proper conversion.
	 * @param theClass
	 * @param fromStringTranslator
	 * @param toStringTranslator
	 */
	public void registerTranslators( Class<?> theClass, Translator fromStringTranslator, Translator toStringTranslator ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkNotNull( fromStringTranslator, "need a from-string  translator" );
		Preconditions.checkNotNull( toStringTranslator, "need a to-string translator" );
		
		fromStringTranslators.put( theClass, fromStringTranslator );
		toStringTranslators.put( theClass, toStringTranslator );
	}
	
	/**
	 * Gets a translator to translate values from strings to values.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getFromStringTranslator( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a type to get a translator");
		Translator translator = fromStringTranslators.get( theType );
		
		if( translator == null && theType.isEnum( ) ) {
			// enums translators are generated as needed but then saved
			translator = new StringToEnumTranslator( theType );
			fromStringTranslators.put( theType, translator );
		}
		return translator;
	}

	/**
	 * Gets a translator to translator values from an object type to a string.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getToStringTranslator( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a type to get a translator");
		Translator translator = toStringTranslators.get( theType );
		
		if( translator == null && theType.isEnum( ) ) {
			// enums translators are generated as needed but then saved
			translator = new EnumToStringTranslator( theType );
			toStringTranslators.put( theType, translator );
		}
		return translator;
	}
	
	/**
	 * Translates the object into a string.
	 * @param theObject the object to translate
	 * @return the translated object, or null if null was the value of theObject
	 */
	public String toString( Object theObject ) {
		if( theObject == null ) {
			return null;
		} else {
			return toString( theObject, theObject.getClass( ) );
		}
	}

	/**
	 * Translates the object into a string.
	 * If a translator cannot be found an IllegalArgumentException is thrown.
	 * @param theObject the object to translate
	 * @param theType the class to use to pick a translator
	 * @return the translated object
	 */
	public String toString( Object theObject, Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a class to pick translator");
		Translator translator = this.toStringTranslators.get( theType );
		if( translator == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a translator for type '%s'.", theType.getName( ) ) );
		} else {
			return ( String )translator.translate( theObject );
		}
	}

	/**
	 * Translate the string into an object of a particular type.
	 * If a translator cannot be found an IllegalArgumentException is thrown.
	 * @param theString the string to translate
	 * @param theType the type to translate to (by using to pick a translator)
	 * @return the translated value
	 */
	@SuppressWarnings("unchecked")
	public <T> T fromString( String theString, Class<T> theType ) {
		Preconditions.checkNotNull( theType, "need a class to pick translator");
		Translator translator = this.fromStringTranslators.get( theType );
		if( translator == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a translator for type '%s'.", theType.getName( ) ) );
		} else {
			return ( T )translator.translate( theString );
		}
	}
}
