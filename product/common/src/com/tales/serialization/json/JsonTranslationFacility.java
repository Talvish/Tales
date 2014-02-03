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
import org.joda.time.Period;

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
	// these are using concurrent hash maps for slight protection, but concurrency factor is low
	// since we don't expect much concurrency and we don't want the memory overhead
    private final Map<Class<?>, JsonTypeMap> typeMaps = new ConcurrentHashMap< Class<?>, JsonTypeMap>( 16, 0.75f, 1 );
    
	private final Map< Class<?>, Translator> toJsonElementTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );
	private final Map< Class<?>, Translator> fromJsonElementTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );

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
		
		// translators to convert from a json element into a type
		
		// TODO: these could be faster if we went to number directly and then cast into the right type, instead of turning them into strings first
		fromJsonElementTranslators.put( Integer.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Integer.class ) ) );
		fromJsonElementTranslators.put( int.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( int.class ) ) );

		fromJsonElementTranslators.put( Long.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Long.class ) ) );
		fromJsonElementTranslators.put( long.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( long.class ) ) );

		fromJsonElementTranslators.put( Float.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Float.class ) ) );
		fromJsonElementTranslators.put( float.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( float.class ) ) );

		fromJsonElementTranslators.put( Double.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Double.class ) ) );
		fromJsonElementTranslators.put( double.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( double.class ) ) );

		fromJsonElementTranslators.put( Boolean.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( Boolean.class ) ) );
		fromJsonElementTranslators.put( boolean.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( boolean.class ) ) );

		fromJsonElementTranslators.put( DateTime.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( DateTime.class ) ) );
		fromJsonElementTranslators.put( String.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( String.class ) ) );

		fromJsonElementTranslators.put( UUID.class, new JsonElementToStringToChainTranslator( stringTranslators.getFromStringTranslator( UUID.class ) ) );

		
		// translators to convert from a type into a json element
		
		Translator numberToJsonTranslator = new NumberToJsonPrimitiveTranslator();
		toJsonElementTranslators.put( Integer.class, numberToJsonTranslator );
		toJsonElementTranslators.put( int.class, numberToJsonTranslator );

		toJsonElementTranslators.put( Long.class, numberToJsonTranslator );
		toJsonElementTranslators.put( long.class, numberToJsonTranslator );

		toJsonElementTranslators.put( Float.class, numberToJsonTranslator );
		toJsonElementTranslators.put( float.class, numberToJsonTranslator );

		toJsonElementTranslators.put( Double.class, numberToJsonTranslator );
		toJsonElementTranslators.put( double.class, numberToJsonTranslator );

		Translator booleanToJsonTranslator = new BooleanToJsonPrimitiveTranslator();
		toJsonElementTranslators.put( Boolean.class, booleanToJsonTranslator );
		toJsonElementTranslators.put( boolean.class, booleanToJsonTranslator );

		Translator objectToJsonTranslator = new ObjectToJsonPrimitiveTranslator( );
		toJsonElementTranslators.put( DateTime.class, objectToJsonTranslator );
		
		Translator stringToJsonTranslator = new StringToJsonPrimitiveTranslator();
		toJsonElementTranslators.put( String.class, stringToJsonTranslator );

		toJsonElementTranslators.put( UUID.class, objectToJsonTranslator );
		
		Translator voidToJsonTranslator = new VoidToJsonObjectTranslator( );
		toJsonElementTranslators.put( Void.class, voidToJsonTranslator );
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
	public void registerStringTranslators( Class<?> theClass, Translator fromStringTranslator, Translator toStringTranslator ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkNotNull( fromStringTranslator, "need a from-string translator" );
		Preconditions.checkNotNull( toStringTranslator, "need a to-string translator" );
		
		// register with the string handler
		stringTranslators.registerTranslators( theClass, fromStringTranslator, toStringTranslator );
		// and keep copy with the locals
		fromJsonElementTranslators.put( theClass, new JsonElementToStringToChainTranslator( fromStringTranslator ) );
		toJsonElementTranslators.put( theClass, new ChainToStringToJsonPrimitiveTranslator( toStringTranslator ) );
	}

	/***
	 * This method is used to add translators into the system for ensuring proper conversion.
	 * This is meant for complex types. The translators must convert to and from a Gson JsonElement.
	 * @param theClass the class the translators are for
	 * @param fromJsonTranslator the translator that converts from a Gson JsonElement into the type
	 * @param toJsonTranslator the translator that converts from the type to a Gson JsonElement
	 */
	public void registerJsonElementTranslators( Class<?> theClass, Translator fromJsonTranslator, Translator toJsonTranslator ) {
		Preconditions.checkNotNull( theClass, "need a class" );
		Preconditions.checkNotNull( fromJsonTranslator, "need a from-Json translator" );
		Preconditions.checkNotNull( toJsonTranslator, "need a to-Json translator" );
		
		fromJsonElementTranslators.put( theClass, fromJsonTranslator );
		toJsonElementTranslators.put( theClass, toJsonTranslator );
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
			
			Translator toJsonTranslator = null;
			Translator fromJsonTranslator = null;
			
			Collection<FieldDescriptor<?,?>> fields = this.typeSource.getSerializedFields( reflectedType );
			ArrayList<JsonMemberMap> members = new ArrayList<JsonMemberMap>( fields.size() );
			
			// now we iterate over the fields found by the analysis
			for( FieldDescriptor<?,?> field : fields ) {
        		// TODO: we have more than one type to consider
                toJsonTranslator = getToJsonElementTranslator( field.getSite( ).getType(), field.getSite().getGenericType() );
                fromJsonTranslator = getFromJsonElementTranslator( field.getSite( ).getType(), field.getSite().getGenericType() );

                if( toJsonTranslator == null ) {
					throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the to translator could not be found.", field.getSite().getType(), theType.getName( ), field.getSite().getName( ) ) );
                } else if( fromJsonTranslator == null ) {
					throw new IllegalStateException( String.format( "Type '%s' on field '%s.%s' could not be analyzed because the from translator could not be found.", field.getSite().getType(), theType.getName( ), field.getSite().getName( ) ) );
                } else if( !memberNameValidator.isValid( field.getName( ) ) ) {
            		throw new IllegalStateException( String.format( "Field '%s.%s' is using the name '%s' that does not conform to validator '%s'.", reflectedType.getType().getName(), field.getSite().getName(), field.getName( ), memberNameValidator.getClass().getSimpleName() ) );
            	}

                members.add( new JsonMemberMap( field, new TranslatedDataSite( field.getSite(), toJsonTranslator, fromJsonTranslator ), typeMap ) );
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
		Translator translator = getToJsonElementTranslator( theType, theGenericType );
		if( translator == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a translator for type '%s'.", theType.getName( ) ) );
		} else {
			return ( JsonElement )translator.translate( theObject );
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
		Translator translator = getFromJsonElementTranslator( theType, theGenericType );
		if( translator == null ){
			throw new IllegalArgumentException( String.format( "Unable to find a translator for type '%s'.", theType.getName( ) ) );
		} else {
			return ( O )translator.translate( theObject );
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
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates to a JsonEelement from the specified type.
	 * @param theType the type to translate from
	 * @param theGenericType the generic details of the type to translate from
	 * @return the translator for the type
	 */
	public Translator getToJsonElementTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = toJsonElementTranslators.get( theType );
		// TODO: getToJsonElementTranslator doesn't save items when there is a generic type, so this generates dead objects for some types like collections

		if( translator == null ) {
    		if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
	            Translator keyTranslator = getToJsonElementTranslator( keyType, null );
	            Translator valueTranslator = getToJsonElementTranslator( valueType, null );

	            if( keyTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a map because a translator for key type '%s' could not be found.", keyType.getName( ) ) );
	            } else if( valueTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a map because a translator for value type '%s' could not be found.", valueType.getName( ) ) );
	            } else {
	            	translator = new MapToJsonArrayTranslator( keyTranslator, valueTranslator );
	            	// we don't save this translator in our translator map since it requires more than a class to do a lookup
	            }
			
    		} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
        		// first see if we have a collection, and look to get a generic type for it
                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
                Translator elementTranslator = getToJsonElementTranslator( elementType, null );
	            
                if( elementTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a collection because a translator for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	translator = new CollectionToJsonArrayTranslator( elementTranslator );
	            	// we don't save this translator in our translator map since it requires more than a class to do a lookup
	            }

        	} else if( theType.isArray( ) ) {
        		// next see if we have an array to get
				Translator elementTranslator = getToJsonElementTranslator( theType.getComponentType( ), null );
	            if( elementTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for an array because a translator for element type '%s' could not be found.", theType.getComponentType( ).getName( ) ) );
	            } else {
					translator = new ArrayToJsonArrayTranslator( elementTranslator );
	        		// save so we don't continually create
	        		toJsonElementTranslators.put( theType, translator );
				}
        	} else if( theType.isEnum( ) ) {
        		translator = new ObjectToJsonPrimitiveTranslator( );
        		// save so we don't continually create
        		toJsonElementTranslators.put( theType, translator );
			} else {
				// if none of the above, we have a complex type
				JsonTypeMap typeMap = generateTypeMap( theType );
				if( typeMap == null ) {
					throw new IllegalStateException( String.format( "Unable to create a translator for complex type '%s' because a json map could not be generated.", theType.getName() ) );
				} else {
					// TODO: if/when we have generic types parameters here we will no longer be
					//       able to cache like this
					translator = new ObjectToJsonObjectTranslator( typeMap );
	        		// save so we don't continually create
	        		toJsonElementTranslators.put( theType, translator );
				}
			}
		}
		return translator;
	}

	/**
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates from a JsonElement to the specified type.
	 * @param theType the type to translate to
	 * @param theGenericType the generic details of the type to translate to
	 * @return the translator for the type
	 */
	public Translator getFromJsonElementTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = fromJsonElementTranslators.get( theType );
		// TODO: getFromJsonElementTranslator doesn't save items when there is a generic type, so this generates dead objects for some types like collections
	
		if( translator == null ) {
	    	if( Map.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
	            Class<?> keyType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ];
	            Class<?> valueType = ( Class<?> )( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 1 ];
	            Translator keyTranslator = getFromJsonElementTranslator( keyType, null );
	            Translator valueTranslator = getFromJsonElementTranslator( valueType, null );

	            if( keyTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a map because a translator for key type '%s' could not be found.", keyType.getName( ) ) );
	            } else if( valueTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a map because a translator for value type '%s' could not be found.", valueType.getName( ) ) );
	            } else {
	            	translator = new JsonArrayToMapTranslator( keyTranslator, valueTranslator, theType );
	            	// we don't save this translator in our translator map since it requires more than a class to do a lookup
	            }
	            
	    	} else if( Collection.class.isAssignableFrom( theType ) && ( theGenericType instanceof ParameterizedType ) ) {
        		// start be seeing if we have a collection and if so generate some translators
                Class<?> elementType = ( Class<?> )( ( ( ParameterizedType ) theGenericType ).getActualTypeArguments( )[ 0 ] );
                Translator elementTranslator = getFromJsonElementTranslator( elementType, null );
	            
                if( elementTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for a collection because a translator for element type '%s' could not be found.", elementType.getName( ) ) );
	            } else {
	            	translator = new JsonArrayToCollectionTranslator( elementTranslator, theType );
	            	// we don't save this translator in our translator map since it requires more than a class to do a lookup
	            }
        	} else if( theType.isArray( ) ) {
				Translator elementTranslator = getFromJsonElementTranslator( theType.getComponentType( ), null );
	            if( elementTranslator == null ) {
	            	throw new IllegalStateException( String.format( "Unable to create translator for an array because a translator for element type '%s' could not be found.", theType.getComponentType( ).getName( ) ) );
	            } else {
					translator = new JsonArrayToArrayTranslator( theType.getComponentType(), elementTranslator ); 
	        		// save so we don't continually create
	        		fromJsonElementTranslators.put( theType, translator );
	            }
        	} else if( theType.isEnum( ) ) {
        		translator = new JsonElementToStringToChainTranslator( new StringToEnumTranslator( theType ) );
        		// save so we don't continually create
        		fromJsonElementTranslators.put( theType, translator );
			} else {
				JsonTypeMap typeMap = generateTypeMap( theType );
				if( typeMap == null ) {
					throw new IllegalStateException( String.format( "Unable to create a translator for complex type '%s' because a json map could not be generated.", theType.getName() ) );
				} else {
					// TODO: if/when we have generic types parameters here we will no longer be
					//       able to cache like this
					translator = new JsonObjectToObjectTranslator( typeMap );
	        		// save so we don't continually create
	        		fromJsonElementTranslators.put( theType, translator );
				}
			}
		}
		return translator;
	}

	
	
	/**
	 * Holds the base types we support for simple translation to a type.
	 */
	private static final HashMap<Class<?>, String> baseTypeToStringMap = new HashMap<Class<?>, String>();

    static {
		baseTypeToStringMap.put( int.class, "int32" );
		baseTypeToStringMap.put( Integer.class, "int32" );
		baseTypeToStringMap.put( long.class, "int64" );
		baseTypeToStringMap.put( Long.class, "int64" );
		baseTypeToStringMap.put( float.class, "float32" );
		baseTypeToStringMap.put( Float.class, "float32" );
		baseTypeToStringMap.put( double.class, "float64" );
		baseTypeToStringMap.put( Double.class, "float64" );

		baseTypeToStringMap.put( boolean.class, "boolean" );
		baseTypeToStringMap.put( Boolean.class, "boolean" );

		baseTypeToStringMap.put( String.class, "string" );
		baseTypeToStringMap.put( DateTime.class, "datetime" );
		baseTypeToStringMap.put( Period.class, "period" );
		baseTypeToStringMap.put( UUID.class, "string : UUID" );
		
		baseTypeToStringMap.put( Void.TYPE, "void" );
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
		String typeString = baseTypeToStringMap.get( theType );

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
		String typeString = baseTypeToStringMap.get( theType );

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
