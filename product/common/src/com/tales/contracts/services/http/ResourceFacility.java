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
package com.tales.contracts.services.http;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.data.DataContractTypeSource;
import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.translators.Translator;
import com.tales.serialization.Readability;
import com.tales.serialization.json.JsonTranslationFacility;
import com.tales.serialization.json.JsonTypeReference;
import com.tales.serialization.json.translators.ChainToJsonElementToStringTranslator;
import com.tales.serialization.json.translators.StringToJsonElementToChainTranslator;
import com.tales.services.DependencyException;
import com.tales.services.Status;
import com.tales.services.InvalidParameterException;
import com.tales.services.InvalidStateException;
import com.tales.services.NotFoundException;
import com.tales.services.http.FailureSubcodes;
import com.tales.system.Facility;


// TODO: - do not force all members existing when you save (json types)
//       - support returning a 202 (maybe we do the advanced return type, from the bug)

/**
 * This class is used to analyze a class and then setup results for 
 * using the class for mapping calls between an engine and a web interface.
 * The class DOES assume that exception handling registration occurs
 * during setup and never overlaps with the toResult calls. This means
 * it does no locking. So if registration and toResult calls need to
 * overlap, then the caller must manage the synchronization.
 * @author jmolnar
 *
 */
public final class ResourceFacility implements Facility {
	private static final Logger logger = LoggerFactory.getLogger( ResourceType.class );

	private final JsonTranslationFacility jsonTranslation;
	private final HashMap<Class<?>, ExceptionHandler<?>> exceptionHandlers = new HashMap<Class<?>, ExceptionHandler<?>>();
	
	/**
	 * Default constructor.
	 */
	public ResourceFacility( ) {
		this( new JsonTranslationFacility( new DataContractTypeSource( ), Readability.MACHINE, new LowerCaseEntityNameValidator(), new LowerCaseEntityNameValidator() ) );
	}
	
	/**
	 * Constructor taking the JSON translation facility to use.
	 * @param theJsonTranslationFacility the JSON translation facility to use
	 */
	public ResourceFacility( JsonTranslationFacility theJsonTranslationFacility ) {
		Preconditions.checkNotNull( theJsonTranslationFacility, "need the json translator facilities" );
		this.jsonTranslation = theJsonTranslationFacility;
		
		// register some default handlers
		registerExceptionHandler( DependencyException.class, new ExceptionHandler<DependencyException>() {
			public ResourceMethodResult toResult( ResourceMethod theMethod, DependencyException theException ) {
				return new ResourceMethodResult( 
						DependencyException.Problem.convert( theException.getProblem( ) ),
						null,
						String.format( 
								"A dependency failure occurred while running '%s.%s'.", 
								theMethod.getResourceType().getType().getSimpleName(), 
								theMethod.getMethod( ).getName( ) ), 
						theException );
			}
		});

		registerExceptionHandler( InvalidParameterException.class, new ExceptionHandler<InvalidParameterException>() {
			public ResourceMethodResult toResult( ResourceMethod theMethod, InvalidParameterException theException ) {
				return new ResourceMethodResult( 
						Status.CALLER_BAD_INPUT,
						theException.getCode(),
						String.format( 
								"Received invalid data for '%s.%s'.", 
								theMethod.getResourceType().getType().getSimpleName(), 
								theMethod.getMethod( ).getName( ) ), 
						theException );
			}
		});

		registerExceptionHandler( InvalidStateException.class, new ExceptionHandler<InvalidStateException>() {
			public ResourceMethodResult toResult( ResourceMethod theMethod, InvalidStateException theException ) {
				return new ResourceMethodResult( 
						Status.CALLER_BAD_STATE,
						theException.getCode(),
						String.format( 
								"'%s.%s' indicated it is in an invalid state.", 
								theMethod.getResourceType().getType().getSimpleName(), 
								theMethod.getMethod( ).getName( ) ), 
						theException );
			}
		});

		registerExceptionHandler( NotFoundException.class, new ExceptionHandler<NotFoundException>() {
			public ResourceMethodResult toResult( ResourceMethod theMethod, NotFoundException theException ) {
				return new ResourceMethodResult( 
						Status.CALLER_NOT_FOUND,
						theException.getCode(),
						String.format( 
								"Cannot find necessary data for '%s.%s'.", 
								theMethod.getResourceType().getType().getSimpleName(), 
								theMethod.getMethod( ).getName( ) ), 
						theException );
			}
		});
	}
	
	/**
	 * Returns the facility responsible for converting to/from JSON.
	 * @return the JSON translation facility
	 */
	public JsonTranslationFacility getJsonFacility( ) {
		return jsonTranslation;
	}
	
	/**
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates from a string value, as expected by a http request parameter
	 * to the specified type.
	 * @param theType the type to translate from
	 * @param theGenericType the generic details of the type to translate from
	 * @return the translator for the type
	 */
	public Translator getFromParameterTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator;
		// first try to get a simple translator, if there is one then
		// it is a simple type that we dont' need to do any additional 
		// json parsing on
		translator = this.jsonTranslation.getFromStringTranslator( theType );
		if( translator == null ) {
			// did not find one so we are now look for something that will be more complicated
			JsonTypeReference typeReference = this.jsonTranslation.getTypeReference(theType, theGenericType);
			if( typeReference != null ) {
				// NOTE: we don't store these because we cannot index off of type/generic type (yet)
				//       and because of that, jsonTranslators is unable to cache their results
				//       but it would be nice to cache
				translator = new StringToJsonElementToChainTranslator( typeReference.getFromJsonTranslator() );
			}
		}
		return translator;
	}
		
	/**
	 * This method is called to get or generate a translator for the class, and its generic details.
	 * The translator translates from a value to a string value that would be expected to be sent
	 * as an http request parameter for the specified type.
	 * @param theType the type to translate from
	 * @param theGenericType the generic details of the type to translate to
	 * @return the translator for the type
	 */
	public Translator getToParameterTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator;
		// first try to get a simple translator, if there is one then
		// it is a simple type that we dont' need to do any additional 
		// json parsing on
		translator = this.jsonTranslation.getToStringTranslator( theType );
		if( translator == null ) {
			// did not find one so we are now look for something that will be more complicated
			JsonTypeReference typeReference = this.jsonTranslation.getTypeReference(theType, theGenericType);
			if( typeReference != null ) {
				// NOTE: we don't store these because we cannot index off of type/generic type (yet)
				//       and because of that, jsonTranslators is unable to cache their results
				//       but it would be nice to cache
				translator = new ChainToJsonElementToStringTranslator( typeReference.getToJsonTranslator() );
			}
		}
		return translator;
	}

	/**
	 * This method takes the resource object and creates the meta information around
	 * it. This will use the path and generate fresh linkage to the associate resource.
	 * @param theResource the resource to generate for
	 * @param thePath the path of the resource
	 * @return the generated meta information
	 */
	public final ResourceType generateResource( Object theResource, String thePath ) {
		Preconditions.checkNotNull( theResource, "need an object to get the class to generate for" );
		return generateResource( theResource.getClass(), thePath );
	}

	/**
	 * This method takes the resource class and creates the meta information around
	 * it. This will use the path and generate fresh linkage to the associate resource.
	 * @param theResource the resource to generate for
	 * @param thePath the path of the resource
	 * @return the generated meta information
	 */
	public final ResourceType generateResource( Class<?> theResourceClass, String thePath ) {
		Preconditions.checkNotNull( theResourceClass, "need a class to generate a resource type" );
		Preconditions.checkNotNull( thePath, "need a non-null root path" );
		ResourceContract resourceAnnotation = theResourceClass.getAnnotation( ResourceContract.class );
		Preconditions.checkState( resourceAnnotation != null, String.format( "The class '%s' needs to have the resource contract annotation", theResourceClass.getName( ) ) );
        Preconditions.checkState( !Strings.isNullOrEmpty( resourceAnnotation.name() ), String.format( "The contract name for class '%s' must be given", theResourceClass.getName( ) ) );
		Preconditions.checkState( resourceAnnotation.versions() != null && resourceAnnotation.versions().length > 0, String.format( "The contract for class '%s' must have at least one version", theResourceClass.getName( ) ) );

		if( thePath.endsWith( "/*" ) ) {
			thePath = thePath.substring( 0, thePath.length() - 2 );
		}
		ResourceType resourceType = new ResourceType( 
				resourceAnnotation.name(),
				resourceAnnotation.description(),
				resourceAnnotation.versions(),
				thePath, 
				theResourceClass );

		// if not, we analyze and then store it
		ArrayList<ResourceMethod> resourceMethods = new ArrayList<ResourceMethod>( );
		
		// loop over a class's public method and see if it has marked with the method attribute
		for( Method method : theResourceClass.getMethods() ) {
			ResourceOperation operationAnnotation = method.getAnnotation( ResourceOperation.class );
			if( operationAnnotation != null ) {
				// if this method does have the annotation, we still verify the method is public
				if( !Modifier.isPublic( method.getModifiers() ) ) { 
					throw new IllegalStateException( String.format( "The method '%s.%s' is not marked public.", theResourceClass.getName(), method.getName() ) );
				} else {
					String name = operationAnnotation.name();
					if( Strings.isNullOrEmpty( name ) ) {
						name = method.getName();
					}
					resourceMethods.add( new ResourceMethod( 
							name,
							operationAnnotation.description(),
							operationAnnotation.versions() == null || operationAnnotation.versions().length == 0 ? resourceAnnotation.versions() : operationAnnotation.versions(),  
							operationAnnotation.path( ), 
							method, 
							operationAnnotation.mode( ), 
							operationAnnotation.signedRequest( ),
							operationAnnotation.signedResponse( ),
							resourceType, 
							this ) );
				}
			}
		}
		
		// now set the methods, which may do some additional validation
		resourceType.setMethods( resourceMethods );
		// we don't store this resource type anywhere because it may be specific to an 
		// instance, not the type itself, because of the path reference
		return resourceType;
	}

	// TODO: re-enable this when I move it out
	/**
	 * The mechanism that allows people to register exception handlers for method execution failures.
	 * @param theExceptionClass the exception class 
	 * @param theHandler the handler that is called if the associated exception occurs
	 */
	final <E extends Throwable>void registerExceptionHandler( Class<E> theExceptionClass, ExceptionHandler<E> theHandler ) {
		Preconditions.checkNotNull( theExceptionClass, "need the exception class to handle" );
		Preconditions.checkNotNull( theHandler, "need a handler ");
		Preconditions.checkState( !exceptionHandlers.containsKey( theExceptionClass ), String.format( "An exception handler for class '%s' already exists.", theExceptionClass.getName( ) ) );
		
		exceptionHandlers.put( theExceptionClass, theHandler );
	}
	
	/**
	 * Package level helper method that is called when a method is executed but fails
	 * due to an exception.
	 * @param theMethod the method that failed
	 * @param theException the cause of the failure
	 * @return the result we will give back to the caller
	 */
	final <E extends Throwable>ResourceMethodResult toResult( ResourceMethod theMethod, E theException ) {
		ResourceMethodResult result = null;
		try {
			Preconditions.checkNotNull( theMethod, "need the method" );
			Preconditions.checkNotNull( theException, "need the exception" );
		
			@SuppressWarnings("unchecked")
			ExceptionHandler<E> mapper = (ExceptionHandler<E>) exceptionHandlers.get( theException.getClass( ) );
			if( mapper != null ) {
				result = mapper.toResult( theMethod, theException );
			}
			
			if( result == null ) {
				String message = String.format( 
						"Unmanaged exception '%s' occurred while running '%s.%s'.", 
						theException.getClass( ).getSimpleName( ), 
						theMethod.getResourceType().getType().getSimpleName(), 
						theMethod.getMethod( ).getName( ) ); 
				logger.error( message, theException );
				result = new ResourceMethodResult( 
						Status.LOCAL_ERROR, 
						FailureSubcodes.UNHANDLED_EXCEPTION,
						message,
						theException );
			} else {
				// we don't log the exception here since it was elected to be handled
				// we could consider moving to add configuration to switch is important
				logger.info( String.format( 
						"Remapped a response for '%s.%s' to status '%s' with message: %s", 
						theMethod.getResourceType().getType().getSimpleName(), 
						theMethod.getMethod( ).getName( ),
						result.getCode( ),
						theException.getMessage() ) );
			}
			
		} catch( Exception e ) {
			String message = String.format( 
					 "While attempting to handle exception '%s', which occurred while running '%s.%s', exception '%s' occurred.", 
					theException == null ? "<unknown>" : theException.getClass( ).getSimpleName( ), 
					theMethod == null ? "<unknown>" : theMethod.getResourceType().getType().getSimpleName(), 
					theMethod == null ? "<unknown>" : theMethod.getMethod( ).getName( ),
					e.getClass( ).getSimpleName( ) ); 
			logger.error( message, theException );
			result = new ResourceMethodResult( 
					Status.LOCAL_ERROR, 
					FailureSubcodes.UNHANDLED_EXCEPTION,
					message,
					theException );
		}

		return result;
	}
}
