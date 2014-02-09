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
package com.tales.serialization.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import com.tales.parts.naming.NameValidator;
import com.tales.parts.naming.NopNameValidator;
import com.tales.parts.reflection.FieldDescriptor;
import com.tales.parts.reflection.TypeDescriptor;
import com.tales.parts.reflection.ValueType;
import com.tales.parts.sites.TranslatedDataSite;
import com.tales.parts.translators.StringToEnumTranslator;
import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;
import com.tales.serialization.Readability;
import com.tales.serialization.SerializationTypeSource;
import com.tales.serialization.StringTranslationFacility;
import com.tales.serialization.json.translators.ArrayToJsonArrayTranslator;
import com.tales.serialization.json.translators.BooleanToJsonPrimitiveTranslator;
import com.tales.serialization.json.translators.ChainToStringToJsonPrimitiveTranslator;
import com.tales.serialization.json.translators.CollectionToJsonArrayTranslator;
import com.tales.serialization.json.translators.JsonArrayToArrayTranslator;
import com.tales.serialization.json.translators.JsonArrayToCollectionTranslator;
import com.tales.serialization.json.translators.JsonArrayToMapTranslator;
import com.tales.serialization.json.translators.JsonElementToStringToChainTranslator;
import com.tales.serialization.json.translators.JsonObjectToObjectTranslator;
import com.tales.serialization.json.translators.JsonObjectToVoidTranslator;
import com.tales.serialization.json.translators.MapToJsonArrayTranslator;
import com.tales.serialization.json.translators.NumberToJsonPrimitiveTranslator;
import com.tales.serialization.json.translators.ObjectToJsonObjectTranslator;
import com.tales.serialization.json.translators.ObjectToJsonPrimitiveTranslator;
import com.tales.serialization.json.translators.StringToJsonPrimitiveTranslator;
import com.tales.serialization.json.translators.VoidToJsonObjectTranslator;
import com.tales.system.Facility;

/**
 * This class manages translation to/from JSON.
 * @author jmolnar
 *
 */
public final class JsonTranslationFacility implements Facility {
	private final Map<Class<?>, JsonTypeReference> translators = new ConcurrentHashMap<>( 16, 0.75f, 1 );
	
	
	// these are using concurrent hash maps for slight protection, but concurrency factor is low
	// since we don't expect much concurrency and we don't want the memory overhead
    private final Map<Class<?>, JsonTypeMap> typeMaps = new ConcurrentHashMap< Class<?>, JsonTypeMap>( 16, 0.75f, 1 );
    
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
	
	private NameValidator typeNameValidator;
	private NameValidator memberNameValidator;
		

	public JsonTranslationFacility( SerializationTypeSource<?,?> theTypeSource ) {
		this( theTypeSource, Readability.MACHINE, null, null );
	}

	public JsonTranslationFacility( SerializationTypeSource<?,?> theTypeSource, Readability theDefaultReadability ) {
		this( theTypeSource, theDefaultReadability, null, null );
	}

	public JsonTranslationFacility( SerializationTypeSource<?,?> theTypeSource, Readability theDefaultReadability, NameValidator theTypeNameValidator, NameValidator theMemberNameValidator ) {
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
		
		typeNameValidator = theTypeNameValidator == null ? new NopNameValidator( ) : theTypeNameValidator;
		memberNameValidator = theMemberNameValidator == null ? new NopNameValidator( ) : theMemberNameValidator;
		
		Translator numberToJsonTranslator = new NumberToJsonPrimitiveTranslator( );
		Translator booleanToJsonTranslator = new BooleanToJsonPrimitiveTranslator( );
		Translator objectToJsonTranslator = new ObjectToJsonPrimitiveTranslator( );
		Translator stringToJsonTranslator = new StringToJsonPrimitiveTranslator( );
		Translator jsonToVoidTransator = new JsonObjectToVoidTranslator( );
		Translator voidToJsonTranslator = new VoidToJsonObjectTranslator( );

		// TODO: these could be faster if we went to number directly and then cast into the right type, instead of turning them into strings first
		
		this.translators.put( int.class, new JsonTypeReference(
				int.class, "int32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( int.class ) ),
				numberToJsonTranslator ) );
		this.translators.put( Integer.class, new JsonTypeReference(
				Integer.class, "int32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Integer.class ) ),
				numberToJsonTranslator ) );	
		
		this.translators.put( long.class, new JsonTypeReference(
				long.class, "int64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( long.class ) ),
				numberToJsonTranslator ) );
		this.translators.put( Long.class, new JsonTypeReference(
				Long.class, "int64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Long.class ) ),
				numberToJsonTranslator ) );	
		
		this.translators.put( float.class, new JsonTypeReference(
				float.class, "float32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( float.class ) ),
				numberToJsonTranslator ) );
		this.translators.put( Float.class, new JsonTypeReference(
				Float.class, "float32", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Float.class ) ),
				numberToJsonTranslator ) );	
		
		this.translators.put( double.class, new JsonTypeReference(
				double.class, "float64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( double.class ) ),
				numberToJsonTranslator ) );
		this.translators.put( Double.class, new JsonTypeReference(
				Double.class, "float64", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Double.class ) ),
				numberToJsonTranslator ) );	

		this.translators.put( boolean.class, new JsonTypeReference(
				boolean.class, "boolean", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( boolean.class ) ),
				booleanToJsonTranslator ) );
		this.translators.put( Boolean.class, new JsonTypeReference(
				Boolean.class, "boolean", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Boolean.class ) ),
				booleanToJsonTranslator ) );	
		
		this.translators.put( DateTime.class, new JsonTypeReference(
				DateTime.class, "datetime", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( DateTime.class ) ),
				objectToJsonTranslator ) );
		
		this.translators.put( String.class, new JsonTypeReference(
				String.class, "string", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( String.class ) ),
				stringToJsonTranslator ) );
		
		this.translators.put( UUID.class, new JsonTypeReference(
				UUID.class, "uuid : string", 
				new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( UUID.class ) ),
				objectToJsonTranslator ) );
		
		this.translators.put( Void.TYPE, new JsonTypeReference(
				Void.TYPE, "void", 
				jsonToVoidTransator,
				voidToJsonTranslator ) );
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
	 * @param theClass the class the translators are for
	 * @param fromStringTranslator the translator that converts from a string into the type
	 * @param toStringTranslator the translator that converts from the type to a string
	 */
	public void registerStringTranslators( Class<?> theClass, String theName, Translator fromStringTranslator, Translator toStringTranslator ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkNotNull( fromStringTranslator, "need a from-string translator" );
		Preconditions.checkNotNull( toStringTranslator, "need a to-string translator" );
		
		// register with the string handler
		stringTranslators.registerTranslators( theClass, fromStringTranslator, toStringTranslator );
		// now keep local
		this.translators.put( theClass,  new JsonTypeReference(
				theClass, theName, 
				new JsonElementToStringToChainTranslator( fromStringTranslator ), 
				new ChainToStringToJsonPrimitiveTranslator( toStringTranslator ) ) );
	}

	/***
	 * This method is used to add translators into the system for ensuring proper conversion.
	 * This is meant for complex types. The translators must convert to and from a Gson JsonElement.
	 * @param theClass the class the translators are for
	 * @param fromJsonTranslator the translator that converts from a Gson JsonElement into the type
	 * @param toJsonTranslator the translator that converts from the type to a Gson JsonElement
	 */
	public void registerJsonElementTranslators( Class<?> theClass, String theName,  Translator fromJsonTranslator, Translator toJsonTranslator ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkNotNull( fromJsonTranslator, "need a from-Json translator" );
		Preconditions.checkNotNull( toJsonTranslator, "need a to-Json translator" );
		
		this.translators.put( theClass,  new JsonTypeReference( theClass, theName, fromJsonTranslator, toJsonTranslator ) );
	}

	/**
	 * Gets a translator to translate values from simple non-json input to values.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getFromStringTranslator( Class<?> theType ) {
		return stringTranslators.getFromStringTranslator( theType );
	}

	/**
	 * Gets a translator to translator values from objects into strings.
	 * @param theType the type to get a translator for
	 * @return the translator, or {@code null} if one was not found
	 */
	public Translator getToStringTranslator( Class<?> theType ) {
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
	public JsonTypeMap generateTypeMap( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a type ");
		
		if( typeMaps.containsKey( theType ) ) {
			return typeMaps.get( theType );
			
		} else {
			// first we need to make sure we have contract for the type
			TypeDescriptor<?, ?> reflectedType = this.typeSource.getSerializedType( theType );
			// validate the name
			if( !typeNameValidator.isValid( reflectedType.getName( ) ) ) {
				throw new IllegalStateException( String.format( "Type '%s' is using the name '%s' that does not conform to validator '%s'.", reflectedType.getType().getName(), reflectedType.getName( ), typeNameValidator.getClass().getSimpleName() ) );
			}

			JsonTypeMap typeMap = new JsonTypeMap( reflectedType );
			JsonTypeReference typeReference;
		
			Collection<FieldDescriptor<?,?>> fields = this.typeSource.getSerializedFields( reflectedType );
			ArrayList<JsonMemberMap> members = new ArrayList<JsonMemberMap>( fields.size() );
			
			// now we iterate over the fields found by the analysis
			for( FieldDescriptor<?,?> field : fields ) {
				if( field.isObject( ) && field.getValueTypes().size() > 1 ) {
					for( ValueType<?,?> valueType : field.getValueTypes( ) ) {
						// we need to get translators made for each of the value types						
						typeReference = getTypeReference( valueType.getType(), valueType.getGenericType( ) );		
		                if( typeReference == null ) {
							throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the type reference could not be found.", field.getSite().getType(), theType.getName( ), field.getSite().getName( ) ) );
		                } else if( !memberNameValidator.isValid( field.getName( ) ) ) {
		            		throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", reflectedType.getType().getName(), field.getSite().getName(), field.getName( ), memberNameValidator.getClass().getSimpleName() ) );
		            	}
					}
					
				} else {
					typeReference = getTypeReference( field.getSite( ).getType(), field.getSite().getGenericType() );
	                if( typeReference == null ) {
						throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the type reference could not be found.", field.getSite().getType(), theType.getName( ), field.getSite().getName( ) ) );
	                } else if( !memberNameValidator.isValid( field.getName( ) ) ) {
	            		throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", reflectedType.getType().getName(), field.getSite().getName(), field.getName( ), memberNameValidator.getClass().getSimpleName() ) );
	            	}
	
	                members.add( new JsonMemberMap( field, new TranslatedDataSite( field.getSite(), typeReference.getToJsonTranslator( ), typeReference.getFromJsonTranslator( ) ), typeMap ) );
				}
			}
			// save the members
			typeMap.setMembers( members );
			// and save what we created for later use
			typeMaps.put( theType, typeMap );
			
			return typeMap;
		}
	}

	/**
	 * Helper method, meant for low volume use (otherwise get and save the translator),
	 * that translates the given object, of the specific type, into a JsonElement.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @param theGenericType the generic type of the object, if available
	 * @return the JsonElement representing the object
	 */
	public <O> JsonElement toJsonElement( O theObject, Class<O> theType, Type theGenericType ) {
		// TODO: getToJsonElementTranslator doesn't save items when there is a generic type, so this generates dead objects for some types like collections
		Preconditions.checkNotNull( theType, "need a type" );
		JsonTypeReference typeReference = getTypeReference( theType, theGenericType );
		if( typeReference == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a type reference for type '%s'.", theType.getName( ) ) );
		} else {
			return ( JsonElement )typeReference.getToJsonTranslator().translate( theObject );
		}
	}

	/**
	 * Helper method, meant for low volume use (otherwise get and save the translator),
	 * that translates the given JsonElement into the object of the specific type. 
	 * @param theObject the json element to translate
	 * @param theType the type to translate into
	 * @param theGenericType the generic type to translate into
	 * @return the translated object
	 */
	@SuppressWarnings("unchecked")
	public <O> O fromJsonElement( JsonElement theObject, Class<O> theType, Type theGenericType ) {
		// TODO: getFromJsonElementTranslator doesn't save items when there is a generic type, so this generates dead objects for some types like collections
		Preconditions.checkNotNull( theType, "need a type" );
		JsonTypeReference typeReference = getTypeReference( theType, theGenericType );
		if( typeReference == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a type reference for type '%s'.", theType.getName( ) ) );
		} else {
			return ( O )typeReference.getFromJsonTranslator().translate( theObject );
		}	
	}

	/**
	 * Helper method, meant for low volume use (otherwise get and save the translator),
	 * that translates the given object, of the specific type, into a JSON string.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @param theGenericType the generic type of the object, if available
	 * @return the json string representing the object
	 */
	public <O> String toJsonString( O theObject, Class<O> theType, Type theGenericType ) {
		return defaultGson.toJson( toJsonElement( theObject, theType, theGenericType ) );
	}

	/**
	 * Helper method, meant for low volume use (otherwise get and save the translator),
	 * that translates the given object, of the specific type, into a JSON string.
	 * @param theObject the object to translate
	 * @param theType the type of the object
	 * @param theGenericType the generic type of the object, if available
	 * @param theReadability the readability to use 
	 * @return the json string representing the object
	 */
	public <O> String toJsonString( O theObject, Class<O> theType, Type theGenericType, Readability theReadability ) {
		// TODO: this doesn't do the strings in a 'readable' way either, which would be nice 
		//       (need to have toJsonElement take readability and have string translators) 
		Gson gson = theReadability == Readability.HUMAN ? this.humanGson : this.machineGson;
		return gson.toJson( toJsonElement( theObject, theType, theGenericType ) );
	}

	/**
	 * Helper method, meant for low volume use (otherwise get and save the translator),
	 * that translates the string, containing JSON, into the object of the specific type. 
	 * @param theString the json string to translate
	 * @param theType the type to translate into
	 * @param theGenericType the generic type to translate into
	 * @return the translated object
	 */
	public <O> O fromJsonString( String theString, Class<O> theType, Type theGenericType ) {
		try {
			return fromJsonElement( parser.parse( theString ), theType, theGenericType );
		} catch( JsonParseException e ) {
			throw new TranslationException( e );
		}
	}


	/**
	 * This method is called to get or generate a type reference for the class, and its generic details.
	 * @param theType the type to translate to
	 * @param theGenericType the generic details of the type to translate to
	 * @return the type reference for the type
	 */
	public JsonTypeReference getTypeReference( Class<?> theType, Type theGenericType ) {
		JsonTypeReference typeReference = translators.get( theType );
		
		// TODO: this doesn't save items when there is a generic type, so this generates dead objects for some types like collections
		if( typeReference == null ) {
	    	if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
	            JsonTypeReference keyTypeReference = getTypeReference( keyType, null );
	            JsonTypeReference valueTypeReference = getTypeReference( valueType, null );

	            if( keyTypeReference == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type reference for a map because a type reference for key type '%s' could not be found.", keyType.getName( ) ) );
	            } else if( valueTypeReference == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type reference for a map because a type refernece for value type '%s' could not be found.", valueType.getName( ) ) );
	            } else {
	            	typeReference = new JsonTypeReference( 
	            			theType, "map", // TODO: need to generate better
	            			new JsonArrayToMapTranslator( keyTypeReference.getFromJsonTranslator(), valueTypeReference.getFromJsonTranslator(), theType ),
	            			new MapToJsonArrayTranslator( keyTypeReference.getToJsonTranslator(), valueTypeReference.getToJsonTranslator() ) );
	            	// we don't save this reference since it requires more than a class to do a lookup
	            }
	            
	    	} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
        		// start be seeing if we have a collection and if so generate some translators
                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
                JsonTypeReference elementTypeReference = getTypeReference( elementType, null );
	            
                if( elementTypeReference == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type reference for a collection because a type reference for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	typeReference = new JsonTypeReference( 
	            			theType, "list", // TODO: need to generate better
	            			new JsonArrayToCollectionTranslator( elementTypeReference.getFromJsonTranslator(), theType ),
	            			new CollectionToJsonArrayTranslator( elementTypeReference.getToJsonTranslator() ) );
	            	// we don't save this translator in our translator map since it requires more than a class to do a lookup
	            }
        	} else if( theType.isArray( ) ) {
        		JsonTypeReference elementTypeReference = getTypeReference( theType.getComponentType( ), null );
	            if( elementTypeReference == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create a type reference for an array because a type reference for element type '%s' could not be found.", theType.getComponentType( ).getName( ) ) );
	            } else {
	            	typeReference = new JsonTypeReference( 
	            			theType, "list", // TODO: need to generate better
	            			new JsonArrayToArrayTranslator( theType.getComponentType(), elementTypeReference.getFromJsonTranslator() ),
	            			new ArrayToJsonArrayTranslator( elementTypeReference.getToJsonTranslator() ) );
	        		// save so we don't continually create
	        		translators.put( theType, typeReference );
	            }
        	} else if( theType.isEnum( ) ) {
            	typeReference = new JsonTypeReference( 
            			theType, "enum : string", // TODO: need to generate better
            			new JsonElementToStringToChainTranslator( new StringToEnumTranslator( theType ) ),
            			new ObjectToJsonPrimitiveTranslator( ) );
            	// save so we don't continually create
            	translators.put( theType, typeReference );
			} else {
				JsonTypeMap typeMap = generateTypeMap( theType );
				if( typeMap == null ) {
					throw new IllegalStateException( String.format( "Unable to create a type reference for complex type '%s' because a json type map could not be generated.", theType.getName() ) );
				} else {
					// TODO: if/when we have generic types parameters here we will no longer be
					//       able to cache like this
	            	typeReference = new JsonTypeReference( 
	            			theType, typeMap.getReflectedType().getName(),
	            			new JsonObjectToObjectTranslator( typeMap ),
	            			new ObjectToJsonObjectTranslator( typeMap ) );
	            	// save so we don't continually create
	            	translators.put( theType, typeReference );
				}
			}
		}
		return typeReference;
	}

	/**
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates to a JsonEelement from the specified type.
	 * @param theType the type to translate from
	 * @param theGenericType the generic details of the type to translate from
	 * @param theFoundTypeMaps an optional collection which will be used to store type maps found, including element types, aggregated member types
	 * @return an external facing name to represent the type
	 */
	public String generateTypeName( Class<?> theType, Type theGenericType, Set<JsonTypeMap> theFoundTypeMaps ) {
		// TODO: this shoudl be disappearing once the other work related to polymorphic/custom types are done
		JsonTypeReference nameInfo = this.translators.get( theType );
		String typeString = nameInfo != null ? nameInfo.getName() : null;

		if( typeString  == null ) {
    		if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
	            String keyTypeString = generateTypeName( keyType, null, theFoundTypeMaps );
	            String valueTypeString = generateTypeName( valueType, null, theFoundTypeMaps );

	            if( keyTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a map because a name for key type '%s' could not be found.", keyType.getName( ) ) );
	            } else if( valueTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a map because a name for value type '%s' could not be found.", valueType.getName( ) ) );
	            } else {
	            	typeString = String.format( "map[ %s, %s ]", keyTypeString, valueTypeString );
	            }
			
    		} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
        		// first see if we have a collection, and look to get a generic type for it
                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
                String elementTypeString = generateTypeName( elementType, null, theFoundTypeMaps );
	            
                if( elementTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a collection because a name for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	typeString = String.format( "list[ %s ]", elementTypeString );
	            }

        	} else if( theType.isArray( ) ) {
        		// next see if we have an array to get
        		Class<?> elementType = theType.getComponentType( );
                String elementTypeString = generateTypeName( elementType, null, theFoundTypeMaps );
                if( elementTypeString == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for an array because a name for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	typeString = String.format( "list[ %s ]", elementTypeString );
	            }
        	} else if( theType.isEnum( ) ) {
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
		            				memberMap.getReflectedField().getSite().getGenericType(), 
		            				theFoundTypeMaps );
		            	}
	            	}
				}
			}
		}
		return typeString;
	}


	public void getTypes( Class<?> theType, Type theGenericType, HashMap<String, JsonTypeMap> theDataTypes ) {
		JsonTypeReference nameInfo = this.translators.get( theType );
		String typeString = nameInfo != null ? nameInfo.getName() : null;

		if( typeString  == null ) {
    		if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
	            getTypes( keyType, null, theDataTypes );
	            getTypes( valueType, null, theDataTypes );
			
    		} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
        		// first see if we have a collection, and look to get a generic type for it
                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
                getTypes( elementType, null, theDataTypes );

        	} else if( theType.isArray( ) ) {
        		// next see if we have an array to get
        		Class<?> elementType = theType.getComponentType( );
               	getTypes( elementType, null, theDataTypes );

        	} else if( theType.isEnum( ) ) {
        		// do nothing for enums

        	} else {
				JsonTypeMap typeMap = this.generateTypeMap( theType );
				if( typeMap == null ) {
	            	throw new IllegalStateException( String.format( "Unable to get a type name for a complex type '%s' because a name could not be found.", theType.getName( ) ) );
				} else {
					// if none of the above, we have a complex type
	            	typeString = typeMap.getReflectedType( ).getName( );
	            	theDataTypes.put( typeMap.getReflectedType( ).getName( ), typeMap );
	            	
	            	// next we should loop through the members and add to this list
	            	for( JsonMemberMap memberMap : typeMap.getMembers( ) ) {
	            		getTypes( memberMap.getReflectedField().getSite().getType(), memberMap.getReflectedField().getSite().getGenericType(), theDataTypes );
	            	}
				}
			}
		}
	}
}
