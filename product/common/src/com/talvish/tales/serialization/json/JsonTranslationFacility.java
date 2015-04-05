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
package com.talvish.tales.serialization.json;

import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.talvish.tales.businessobjects.ObjectId;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.reflection.TypeUtility;
import com.talvish.tales.parts.reflection.ValueType;
import com.talvish.tales.parts.sites.TranslatedDataSite;
import com.talvish.tales.parts.translators.StringToEnumTranslator;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.Readability;
import com.talvish.tales.serialization.SerializationField;
import com.talvish.tales.serialization.SerializationType;
import com.talvish.tales.serialization.SerializationTypeSource;
import com.talvish.tales.serialization.StringTranslationFacility;
import com.talvish.tales.serialization.TypeFormatAdapter;
import com.talvish.tales.serialization.json.translators.ArrayToJsonArrayTranslator;
import com.talvish.tales.serialization.json.translators.BooleanToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.ChainToStringToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.CollectionToJsonArrayTranslator;
import com.talvish.tales.serialization.json.translators.JsonArrayToArrayTranslator;
import com.talvish.tales.serialization.json.translators.JsonArrayToCollectionTranslator;
import com.talvish.tales.serialization.json.translators.JsonArrayToMapTranslator;
import com.talvish.tales.serialization.json.translators.JsonElementToStringToChainTranslator;
import com.talvish.tales.serialization.json.translators.JsonObjectToObjectTranslator;
import com.talvish.tales.serialization.json.translators.JsonObjectToPolymorphicObjectTranslator;
import com.talvish.tales.serialization.json.translators.JsonObjectToVoidTranslator;
import com.talvish.tales.serialization.json.translators.MapToJsonArrayTranslator;
import com.talvish.tales.serialization.json.translators.NumberToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.ObjectToJsonObjectTranslator;
import com.talvish.tales.serialization.json.translators.ObjectToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.PolymorphicObjectToJsonObjectTranslator;
import com.talvish.tales.serialization.json.translators.StringToJsonPrimitiveTranslator;
import com.talvish.tales.serialization.json.translators.VoidToJsonObjectTranslator;
import com.talvish.tales.system.Facility;

/**
 * This class manages translation to/from JSON.
 * @author jmolnar
 *
 */
public final class JsonTranslationFacility implements Facility {
	private final Map<JavaType, TypeFormatAdapter> adapters = new ConcurrentHashMap<>( 16, 0.75f, 1 );
	
	// these are using concurrent hash maps for slight protection, but concurrency factor is low
	// since we don't expect much concurrency and we don't want the memory overhead
    private final Map<JavaType, JsonTypeMap> typeMaps = new ConcurrentHashMap<>( 16, 0.75f, 1 );
    
// 	TODO: cannot store the string versions effectively until we have something to manage class/generic type combo
//	private final Map< Class<?>, Translator> toJsonStringTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );
//	private final Map< Class<?>, Translator> fromJsonStringTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );

	private final StringTranslationFacility stringTranslators;
	@SuppressWarnings("rawtypes")
	private final SerializationTypeSource typeSource;

	private final Gson humanGson;
	private final Gson machineGson;
	private final Gson defaultGson;
	private final Readability defaultReadability;
	private final JsonParser parser = new JsonParser( );
	
	public JsonTranslationFacility( SerializationTypeSource<?,?> theTypeSource ) {
		this( theTypeSource, Readability.MACHINE );
	}

	public JsonTranslationFacility( SerializationTypeSource<?,?> theTypeSource, Readability theDefaultReadability ) {
		// TODO: change more than this, change translators
		//       and update the string translation facility 
		//       to do the same, and then using the 
		//       status servlet
		defaultReadability = theDefaultReadability;
		
		humanGson = new GsonBuilder( ).serializeNulls( ).setPrettyPrinting( ).create();
		machineGson = new GsonBuilder( ).serializeNulls( ).create();
		defaultGson = defaultReadability == Readability.HUMAN ? humanGson : machineGson; 
		
		stringTranslators = new StringTranslationFacility( );
		typeSource = theTypeSource;
		
		Translator numberToJsonTranslator = new NumberToJsonPrimitiveTranslator( );
		Translator booleanToJsonTranslator = new BooleanToJsonPrimitiveTranslator( );
		Translator objectToJsonTranslator = new ObjectToJsonPrimitiveTranslator( );
		Translator stringToJsonTranslator = new StringToJsonPrimitiveTranslator( );
		Translator jsonToVoidTransator = new JsonObjectToVoidTranslator( );
		Translator voidToJsonTranslator = new VoidToJsonObjectTranslator( );

		// TODO: these could be faster if we went to number directly and then cast into the right type, instead of turning them into strings first
		
		JavaType javaType;
		
		javaType = new JavaType( int.class );
		registerJsonElementTranslators( 
				javaType,
				"int32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );
		javaType = new JavaType( Integer.class );
		registerJsonElementTranslators( 
				javaType,
				"int32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );	
		
		javaType = new JavaType( long.class );
		registerJsonElementTranslators( 
				javaType,
				"int64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );
		javaType = new JavaType( Long.class );
		registerJsonElementTranslators( 
				javaType,
				"int64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );	
		
		javaType = new JavaType( float.class );
		registerJsonElementTranslators( 
				javaType,
				"float32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );
		javaType = new JavaType( Float.class );
		registerJsonElementTranslators( 
				javaType,
				"float32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );	
		
		javaType = new JavaType( double.class );
		registerJsonElementTranslators( 
				javaType,
				"float64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );
		javaType = new JavaType( Double.class );
		registerJsonElementTranslators( 
				javaType,
				"float64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				numberToJsonTranslator );	

		javaType = new JavaType( boolean.class );
		registerJsonElementTranslators( 
				javaType,
				"boolean", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				booleanToJsonTranslator );
		javaType = new JavaType( Boolean.class );
		registerJsonElementTranslators( 
				javaType,
				"boolean", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				booleanToJsonTranslator );	
		
		javaType = new JavaType( DateTime.class );
		registerJsonElementTranslators( 
				javaType,
				"datetime", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		javaType = new JavaType( OffsetDateTime.class );
		registerJsonElementTranslators( 
				javaType,
				"datetime", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		javaType = new JavaType( LocalDateTime.class );
		registerJsonElementTranslators( 
				javaType,
				"datetime", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		javaType = new JavaType( LocalDate.class );
		registerJsonElementTranslators( 
				javaType,
				"date", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		javaType = new JavaType( String.class );
		registerJsonElementTranslators( 
				javaType,
				"string", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				stringToJsonTranslator );
		
		javaType = new JavaType( UUID.class );
		registerJsonElementTranslators( 
				javaType,
				"uuid : string", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		javaType = new JavaType( ObjectId.class );
		registerJsonElementTranslators( 
				javaType,
				"objectid : string", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( javaType ) ),
				objectToJsonTranslator );

		// this is the standard void 'type' required when using reflection
		javaType = new JavaType( Void.TYPE );
		registerJsonElementTranslators( 
				javaType,
				"void", 
				jsonToVoidTransator,
				voidToJsonTranslator );
		// this void 'type' seems to be what is used when using voids as the type in generics
		javaType = new JavaType( Void.class );
		registerJsonElementTranslators( 
				javaType,
				"void", 
				jsonToVoidTransator,
				voidToJsonTranslator );
	}
	
	/**
	 * Gets the default readability for to-string conversions.
	 * @return the default readability
	 */
	public Readability getDefaultReaability( ) {
		return this.defaultReadability;
	}
	
	/***
	 * This method is used to add translators into the system for ensuring proper conversion.
	 * This is not meant for complex types, but instead 'primitive' types (eg. single value types)
	 * that will ultimately be turned into json strings. The translators must convert to and from 
	 * strings.
	 * @param theType the class the translators are for
	 * @param fromStringTranslator the translator that converts from a string into the type
	 * @param toStringTranslator the translator that converts from the type to a string
	 */
	public final void registerStringTranslators( JavaType theType, String theName, Translator fromStringTranslator, Translator toStringTranslator ) {
		Preconditions.checkNotNull( theType, "need a class" );
		Preconditions.checkNotNull( fromStringTranslator, "need a from-string translator" );
		Preconditions.checkNotNull( toStringTranslator, "need a to-string translator" );

		// TODO: put a log line if you are overwriting an existing one

		// register with the string handler
		stringTranslators.registerTranslators( theType, fromStringTranslator, toStringTranslator );
		// now keep local
		TypeFormatAdapter jsonTypeAdapter = new TypeFormatAdapter(
				theType,
				theName, 
				new JsonElementToStringToChainTranslator( fromStringTranslator ), 
				new ChainToStringToJsonPrimitiveTranslator( toStringTranslator ) );
		this.adapters.put( jsonTypeAdapter.getType(), jsonTypeAdapter );
	}

	/***
	 * This method is used to add translators into the system for ensuring proper conversion.
	 * This is meant for complex types. The translators must convert to and from a Gson JsonElement.
	 * @param theType the type the translators are for
	 * @param fromJsonTranslator the translator that converts from a Gson JsonElement into the type
	 * @param toJsonTranslator the translator that converts from the type to a Gson JsonElement
	 */
	public final void registerJsonElementTranslators( JavaType theType, String theName, Translator fromJsonTranslator, Translator toJsonTranslator ) {
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkNotNull( fromJsonTranslator, "need a from-Json translator" );
		Preconditions.checkNotNull( toJsonTranslator, "need a to-Json translator" );
		
		// TODO: put a log line if you are overwriting an existing one
		
		TypeFormatAdapter jsonTypeAdapter = new TypeFormatAdapter(
				theType,
				theName, 
				fromJsonTranslator, 
				toJsonTranslator );
		this.adapters.put( jsonTypeAdapter.getType(), jsonTypeAdapter );
	}

	/**
	 * Gets a translator to translate values from simple non-json input to values.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getFromStringTranslator( JavaType theType ) {
		return stringTranslators.getFromStringTranslator( theType );
	}

	/**
	 * Gets a translator to translator values from objects into non-json strings.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getToStringTranslator( JavaType theType ) {
		return stringTranslators.getToStringTranslator( theType );
	}

	/***
	 * Generates a json map for the given type based on the data contract
	 * definitions found on the type. It is presumed the type contains
	 * multiple members that are to be serialized to/from json and 
	 * therefore this method is not suitable for primitive or simple type.
	 * @param theType the type to generate a json map for
	 * @return the json type map
	 */
	@SuppressWarnings("unchecked")
	public JsonTypeMap generateTypeMap( JavaType theType ) {
		Preconditions.checkNotNull( theType, "need a type ");
		
		if( typeMaps.containsKey( theType ) ) {
			return typeMaps.get( theType );
			
		} else {
			// first we need to make sure we have contract for the type
			SerializationType<?, ?> reflectedType = this.typeSource.getSerializedType( theType );
			Preconditions.checkArgument( reflectedType != null, "Unable to generate type information for '%s' likely due to missing annotations.", theType.getSimpleName( ) );

			TypeFormatAdapter jsonTypeAdapter;
			List<TypeFormatAdapter> keyTypeAdapters;
			List<TypeFormatAdapter> valueTypeAdapters;
			
			// we save what we created for later use and we
			// save it early since there is a distinct chance
			// things will loop forever otherwise, and while
			// it isn't done, that shouldn't be an issue
			JsonTypeMap typeMap = new JsonTypeMap( reflectedType );
			typeMaps.put( theType, typeMap );

		
			Collection<SerializationField<?,?>> fields = this.typeSource.getSerializedFields( reflectedType );
			ArrayList<JsonMemberMap> members = new ArrayList<JsonMemberMap>( fields.size() );
			
			// now we iterate over the fields found by the analysis
			for( SerializationField<?,?> field : fields ) {
				if( field.isObject( ) && field.getValueTypes().size() > 1 ) {
					// need a list of type adapters and then when
					// done we pass the type information
					valueTypeAdapters = new ArrayList<>( field.getValueTypes( ).size( ) );
					for( ValueType<?,?> valueType : field.getValueTypes( ) ) {
						// we need to get translators made for each of the value types						
						jsonTypeAdapter = getTypeAdapter( valueType.getType() );		
		                if( jsonTypeAdapter == null ) {
							throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the type adapter could not be found.", valueType.getType(), theType.getName( ), field.getSite().getName( ) ) );
		            	} else {
		            		valueTypeAdapters.add( jsonTypeAdapter );
		            	}
					}
					members.add( new JsonMemberMap( field, new TranslatedDataSite(
							field.getSite(), 
							new PolymorphicObjectToJsonObjectTranslator( valueTypeAdapters ), 
							new JsonObjectToPolymorphicObjectTranslator( valueTypeAdapters ) ), typeMap ) );					
				} else if( field.isCollection( ) && field.getValueTypes().size( ) >  1 ) {
					// so we need to grab the type of the element that was used
					
					// first let's grab the type adapters
					valueTypeAdapters = new ArrayList<>( field.getValueTypes( ).size( ) );
					for( ValueType<?,?> valueType : field.getValueTypes( ) ) {
						// we need to get translators made for each of the value types						
						jsonTypeAdapter = getTypeAdapter( valueType.getType() );		
		                if( jsonTypeAdapter == null ) {
							throw new IllegalStateException( String.format( "Element type '%s' on field '%s.%s' could not be analyzed because the type adapter could not be found.", valueType.getType(), theType.getName( ), field.getSite().getName( ) ) );
		            	} else {
		            		valueTypeAdapters.add( jsonTypeAdapter );
		            	}
					}
					// then we create the member map but we
					// must distinguish between the arrays and
					// standard library collections
					if( field.getSite().getType().getUnderlyingClass().isArray() ) {
						members.add( new JsonMemberMap( 
								field, 
								new TranslatedDataSite(
										field.getSite(),
										new ArrayToJsonArrayTranslator( new PolymorphicObjectToJsonObjectTranslator( valueTypeAdapters ) ),
										new JsonArrayToArrayTranslator( TypeUtility.extractClass( TypeUtility.extractComponentType( field.getSite().getType( ).getType() ) ), new JsonObjectToPolymorphicObjectTranslator( valueTypeAdapters ) ) ), 
										typeMap ) );

					} else {
						members.add( new JsonMemberMap( 
								field,
								new TranslatedDataSite(
										field.getSite(),
										new CollectionToJsonArrayTranslator( new PolymorphicObjectToJsonObjectTranslator( valueTypeAdapters ) ),
										new JsonArrayToCollectionTranslator( new JsonObjectToPolymorphicObjectTranslator( valueTypeAdapters ), field.getSite().getType( ).getUnderlyingClass() ) ), 
										typeMap ) );
					}
					
				} else if( field.isMap( ) ) {
					// this is a bit more interesting because maps already have
					// an intermediate object holding key and value adapters so
					// and also because there may be more than one value type but 
					// not key type
					
					// first let's grab the value adapters
					valueTypeAdapters = new ArrayList<>( field.getValueTypes( ).size( ) );
					for( ValueType<?,?> valueType : field.getValueTypes( ) ) {
						// we need to get translators made for each of the value types						
						jsonTypeAdapter = getTypeAdapter( valueType.getType() );		
		                if( jsonTypeAdapter == null ) {
							throw new IllegalStateException( String.format( "Value type '%s' on field '%s.%s' could not be analyzed because the type adapter could not be found.", valueType.getType(), theType.getName( ), field.getSite().getName( ) ) );
		            	} else {
		            		valueTypeAdapters.add( jsonTypeAdapter );
		            	}
					}
					
					// second, let's grab the key adapters
					keyTypeAdapters = new ArrayList<>( field.getKeyTypes( ).size( ) );
					for( ValueType<?,?> keyType : field.getKeyTypes( ) ) {
						// we need to get translators made for each of the key types						
						jsonTypeAdapter = getTypeAdapter( keyType.getType() );		
		                if( jsonTypeAdapter == null ) {
							throw new IllegalStateException( String.format( "Key type '%s' on field '%s.%s' could not be analyzed because the type adapter could not be found.", keyType.getType(), theType.getName( ), field.getSite().getName( ) ) );
		            	} else {
		            		keyTypeAdapters.add( jsonTypeAdapter );
		            	}
					}
					// okay so now we need to build the member adapter
					members.add( new JsonMemberMap(
							field, 
							new TranslatedDataSite(
									field.getSite(),
									new MapToJsonArrayTranslator( keyTypeAdapters, valueTypeAdapters ),
									new JsonArrayToMapTranslator( keyTypeAdapters, valueTypeAdapters, field.getSite( ).getType( ).getUnderlyingClass() ) ),
									typeMap ) );
					
				} else {
					jsonTypeAdapter = getTypeAdapter( field.getSite().getType() );
	                if( jsonTypeAdapter == null ) {
						throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the type adapter could not be found.", field.getSite().getType().getSimpleName( ), theType.getName( ), field.getSite().getName( ) ) );
	            	}
	
	                members.add( new JsonMemberMap( field, new TranslatedDataSite( field.getSite(), jsonTypeAdapter.getToFormatTranslator( ), jsonTypeAdapter.getFromFormatTranslator( ) ), typeMap ) );
				}
			}
			// save the members now that we have them all
			typeMap.setMembers( members );
			
			return typeMap;
		}
	}

	/**
	 * Helper method that translates the given object, of the specific type, into a JsonElement.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @param theGenericType the generic type of the object, if available
	 * @return the JsonElement representing the object
	 */
	public <O> JsonElement toJsonElement( O theObject, JavaType theType ) {
		// TODO: getToJsonElementTranslator doesn't save items when there is a generic type, so this generates dead objects for some types like collections
		Preconditions.checkNotNull( theType, "need a type" );
		TypeFormatAdapter typeAdapter = getTypeAdapter( theType );
		if( typeAdapter == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a type adapter for type '%s'.", theType.getName( ) ) );
		} else {
			return ( JsonElement )typeAdapter.getToFormatTranslator().translate( theObject );
		}
	}

	/**
	 * Helper method that translates the given JsonElement into the object of the specific type. 
	 * @param theObject the json element to translate
	 * @param theType the type to translate into
	 * @return the translated object
	 */
	@SuppressWarnings("unchecked")
	public <O> O fromJsonElement( JsonElement theObject, JavaType theType ) {
		Preconditions.checkNotNull( theType, "need a type" );
		TypeFormatAdapter typeAdapter = getTypeAdapter( theType );
		if( typeAdapter == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a type adapter for type '%s'.", theType.getName( ) ) );
		} else {
			return ( O )typeAdapter.getFromFormatTranslator().translate( theObject );
		}	
	}

	/**
	 * Helper method that translates the given object, of the specific type, into a JSON string.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @return the json string representing the object
	 */
	public <O> String toJsonString( O theObject, JavaType theType ) {
		return defaultGson.toJson( toJsonElement( theObject, theType ) );
	}

	/**
	 * Helper method that translates the given object, of the specific type, into a JSON string.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @param theGenericType the generic type of the object, if available
	 * @param theReadability the readability to use 
	 * @return the json string representing the object
	 */
	public <O> String toJsonString( O theObject, JavaType theType, Readability theReadability ) {
		// TODO: this doesn't do the strings in a 'readable' way either, which would be nice 
		//       (need to have toJsonElement take readability and have string translators) 
		Gson gson = theReadability == Readability.HUMAN ? this.humanGson : this.machineGson;
		return gson.toJson( toJsonElement( theObject, theType ) );
	}

	/**
	 * Helper method that translates the string, containing JSON, into the object of the specific type. 
	 * @param theString the json string to translate
	 * @param theType the type to translate into
	 * @param theGenericType the generic type to translate into
	 * @return the translated object
	 */
	public <O> O fromJsonString( String theString, JavaType theType ) {
		try {
			return fromJsonElement( parser.parse( theString ), theType );
		} catch( JsonParseException e ) {
			throw new TranslationException( e );
		}
	}


	/**
	 * This method is called to get or generate a type adapter for a type.
	 * @param theType the type to translate to
	 * @return the type adapter for the type
	 */
	public TypeFormatAdapter getTypeAdapter( JavaType theType ) {
		TypeFormatAdapter jsonTypeAdapter = adapters.get( theType );
		if( jsonTypeAdapter == null ) {
	    	if( Map.class.isAssignableFrom( theType.getUnderlyingClass() ) ) {
	    		if( !( theType.getType() instanceof ParameterizedType ) ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type adapter for a map because the parameterized type was not given when '%s' generic types are expected.", theType.getUnderlyingClass().getTypeParameters().length ) );
		    	} else {
		            JavaType keyType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 0 ] );
		            JavaType valueType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 1 ] );
		            TypeFormatAdapter keyTypeAdapter = getTypeAdapter( keyType );
		            TypeFormatAdapter valueTypeAdapter = getTypeAdapter( valueType );
	
		            if( keyTypeAdapter == null ) {
		            	throw new IllegalStateException( String.format( "Unable to create a type adapter for a map because a type adapter for key type '%s' could not be found.", keyType.getName( ) ) );
		            } else if( valueTypeAdapter == null ) {
		            	throw new IllegalStateException( String.format( "Unable to create a type adapter for a map because a type adapter for value type '%s' could not be found.", valueType.getName( ) ) );
		            } else {
		            	jsonTypeAdapter = new TypeFormatAdapter( 
		            			theType, 
		            			"map", // TODO: need to generate better
		            			new JsonArrayToMapTranslator( keyTypeAdapter.getFromFormatTranslator(), valueTypeAdapter.getFromFormatTranslator(), theType.getUnderlyingClass() ),
		            			new MapToJsonArrayTranslator( keyTypeAdapter.getToFormatTranslator(), valueTypeAdapter.getToFormatTranslator() ) );
		            }
		    	}
	            
	    	} else if( Collection.class.isAssignableFrom( theType.getUnderlyingClass() ) ) {
	    		if( !( theType.getType( ) instanceof ParameterizedType ) ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type adapter for a collection because the parameterized type was not given when '%s' generic types are expected.", theType.getUnderlyingClass().getTypeParameters().length ) );
		    	} else {
	        		// start be seeing if we have a collection and if so generate some translators
	                JavaType elementType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 0 ] );
	                TypeFormatAdapter elementTypeAdapter = getTypeAdapter( elementType );

	                if( elementTypeAdapter == null ) {
		            	throw new IllegalStateException( String.format( "Unable to create a type adapter for a collection because a type adapter for element type '%s' could not be found.", elementType.getName( ) ) );
		            } else {
		            	jsonTypeAdapter = new TypeFormatAdapter( 
		            			theType, 
		            			"list", // TODO: need to generate better
		            			new JsonArrayToCollectionTranslator( elementTypeAdapter.getFromFormatTranslator(), theType.getUnderlyingClass() ),
		            			new CollectionToJsonArrayTranslator( elementTypeAdapter.getToFormatTranslator() ) );
		            }
		    	}
	    		
        	} else if( theType.getUnderlyingClass().isArray( ) ) {
        		JavaType elementType = new JavaType( TypeUtility.extractComponentType( theType.getType( ) ) );
        		TypeFormatAdapter elementTypeAdapter = getTypeAdapter( elementType );
	            if( elementTypeAdapter == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type adapter for an array because a type adapter for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	jsonTypeAdapter = new TypeFormatAdapter( 
	            			theType, 
	            			"list", // TODO: need to generate better
	            			new JsonArrayToArrayTranslator( elementType.getUnderlyingClass(), elementTypeAdapter.getFromFormatTranslator() ),
	            			new ArrayToJsonArrayTranslator( elementTypeAdapter.getToFormatTranslator() ) );
	            }
        		
        	} else if( theType.getUnderlyingClass().isEnum( ) ) {
            	jsonTypeAdapter = new TypeFormatAdapter( 
            			theType,
            			"enum : string", // TODO: need to generate better
            			new JsonElementToStringToChainTranslator( new StringToEnumTranslator( theType.getUnderlyingClass() ) ),
            			new ObjectToJsonPrimitiveTranslator( ) );
			} else {
				JsonTypeMap typeMap = generateTypeMap( theType );
				if( typeMap == null ) {
					throw new IllegalStateException( String.format( "Unable to create a type adapter for complex type '%s' because a json type map could not be generated.", theType.getName() ) );
				} else {
	            	jsonTypeAdapter = new TypeFormatAdapter( 
	            			theType,
	            			typeMap.getReflectedType().getName(),
	            			new JsonObjectToObjectTranslator( typeMap ),
	            			new ObjectToJsonObjectTranslator( typeMap ) );
				}
			}
	    	
	    	// we cache these later
			adapters.put( jsonTypeAdapter.getType(), jsonTypeAdapter );
		}

		return jsonTypeAdapter;
	}

	/**
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates to a JsonEelement from the specified type.
	 * @param theType the type to translate from
	 * @param theGenericType the generic details of the type to translate from
	 * @param theFoundTypeMaps an optional collection which will be used to store type maps found, including element types, aggregated member types
	 * @return an external facing name to represent the type
	 */
	public String generateTypeName( JavaType theType, Set<JsonTypeMap> theFoundTypeMaps ) {
		// TODO: this shoudl be disappearing once the other work related to polymorphic/custom types are done
		TypeFormatAdapter typeAdapter = this.adapters.get( theType );
		String typeString = typeAdapter != null ? typeAdapter.getName() : null;

		if( typeString  == null ) {
    		if( Map.class.isAssignableFrom( theType.getUnderlyingClass() ) ) {
	            JavaType keyType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 0 ] );
	            JavaType valueType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 1 ] );
	            String keyTypeString = generateTypeName( keyType, theFoundTypeMaps );
	            String valueTypeString = generateTypeName( valueType, theFoundTypeMaps );

	            if( keyTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a map because a name for key type '%s' could not be found.", keyType.getName( ) ) );
	            } else if( valueTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a map because a name for value type '%s' could not be found.", valueType.getName( ) ) );
	            } else {
	            	typeString = String.format( "map[ %s, %s ]", keyTypeString, valueTypeString );
	            }
			
    		} else if( Collection.class.isAssignableFrom( theType.getUnderlyingClass( ) ) ) {
        		// first see if we have a collection, and look to get a generic type for it
                JavaType elementType = new JavaType( ( ( ParameterizedType )theType.getType( ) ).getActualTypeArguments( )[ 0 ] );
                String elementTypeString = generateTypeName( elementType, theFoundTypeMaps );
	            
                if( elementTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a collection because a name for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	typeString = String.format( "list[ %s ]", elementTypeString );
	            }

        	} else if( theType.getUnderlyingClass().isArray( ) ) {
        		// next see if we have an array to get
        		JavaType elementType = new JavaType( TypeUtility.extractComponentType( theType.getType( ) ) );
                String elementTypeString = generateTypeName( elementType, theFoundTypeMaps );
                if( elementTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for an array because a name for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	typeString = String.format( "list[ %s ]", elementTypeString );
	            }
        	} else if( theType.getUnderlyingClass().isEnum( ) ) {
        		typeString = "string";
        		// TODO: ideally this would set a precondition for this particular entry (or have a type and for the type)
        		//       
			} else {
				JsonTypeMap typeMap = this.generateTypeMap( theType );
				if( typeMap == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a complex type '%s' because a name could not be found.", theType.getName( ) ) );
				} else {
					// if none of the above, we have a complex type
	            	typeString = typeMap.getReflectedType( ).getName( );
	            	if( theFoundTypeMaps != null ) {
	            		// we only look for children types if
	            		// a collection to add to it sent in
		            	theFoundTypeMaps.add( typeMap );
		            	
		            	// we should loop through the members and add to this list
		            	// if there are any to add
		            	for( JsonMemberMap memberMap : typeMap.getMembers( ) ) {
		            		// TODO: we have more than one type to consider
		            		generateTypeName( 
		            				memberMap.getReflectedField().getSite().getType(), 
		            				theFoundTypeMaps );
		            	}
	            	}
				}
			}
		}
		return typeString;
	}

// TODO: need to see if we need this
//	public void getTypes( JavaType theType, HashMap<String, JsonTypeMap> theDataTypes ) {
//		JsonTypeReference nameInfo = this.translators.get( theType );
//		String typeString = nameInfo != null ? nameInfo.getName() : null;
//
//		if( typeString  == null ) {
//    		if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
//	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
//	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
//	            getTypes( keyType, null, theDataTypes );
//	            getTypes( valueType, null, theDataTypes );
//			
//    		} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
//        		// first see if we have a collection, and look to get a generic type for it
//                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
//                getTypes( elementType, null, theDataTypes );
//
//        	} else if( theType.isArray( ) ) {
//        		// next see if we have an array to get
//        		Class<?> elementType = theType.getComponentType( );
//               	getTypes( elementType, null, theDataTypes );
//
//        	} else if( theType.isEnum( ) ) {
//        		// do nothing for enums
//
//        	} else {
//				JsonTypeMap typeMap = this.generateTypeMap( theType, theGenericType );
//				if( typeMap == null ) {
//	            	throw new IllegalStateException( String.format( "Unable to get a type name for a complex type '%s' because a name could not be found.", theType.getName( ) ) );
//				} else {
//					// if none of the above, we have a complex type
//	            	typeString = typeMap.getReflectedType( ).getName( );
//	            	theDataTypes.put( typeMap.getReflectedType( ).getName( ), typeMap );
//	            	
//	            	// next we should loop through the members and add to this list
//	            	for( JsonMemberMap memberMap : typeMap.getMembers( ) ) {
//	            		getTypes( memberMap.getReflectedField().getSite().getType(), memberMap.getReflectedField().getSite().getGenericType(), theDataTypes );
//	            	}
//				}
//			}
//		}
//	}
}
