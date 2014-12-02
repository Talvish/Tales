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
package com.talvish.tales.contracts.services.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.talvish.tales.communication.Status;
import com.talvish.tales.contracts.Subcontract;
import com.talvish.tales.contracts.services.ContractStatus;
import com.talvish.tales.contracts.services.http.ResourceMethodParameter.ContextValue;
import com.talvish.tales.contracts.services.http.ResourceMethodParameter.CookieValue;
import com.talvish.tales.contracts.services.http.ResourceMethodParameter.ParameterSource;
import com.talvish.tales.contracts.services.http.ResourceOperation.Mode;
import com.talvish.tales.parts.RegularExpressionHelper;
import com.talvish.tales.parts.naming.LowerCaseEntityNameValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.sites.DataSiteException;
import com.talvish.tales.parts.translators.TranslationException;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.serialization.Readability;
import com.talvish.tales.serialization.UrlEncoding;
import com.talvish.tales.serialization.json.JsonTypeReference;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.services.http.FailureSubcodes;
import com.talvish.tales.services.http.servlets.ResourceServlet.AsyncState;

/**
 * This class represents a method that is mapping from a http request 
 * into some actual Java class.
 * @author jmolnar
 */
public class ResourceMethod extends Subcontract {
	private static final Logger logger = LoggerFactory.getLogger( ResourceType.class );

	public static final String RESOURCE_METHOD_NAME_VALIDATOR = "tales.contracts.resource_method_name";
	
	static {
		if( !NameManager.hasValidator( ResourceMethod.RESOURCE_METHOD_NAME_VALIDATOR ) ) {
			NameManager.setValidator( ResourceMethod.RESOURCE_METHOD_NAME_VALIDATOR, new LowerCaseEntityNameValidator( ) );
		}
	}
	
	// this allows us to indicate there are parameters in the path

	private static final String ESCAPED_CHAR_REGEX = "\\\\.";
	private static final String RANGE_REGEX = "\\{(?:[^}])*\\}";
	private static final String CHAR_CLASS_REGEX = String.format("\\[(?:[^\\]]|(?:%s))*\\]", ESCAPED_CHAR_REGEX ); // note: cannot use nest character class
	private static final String PARAMETER_FORMAT_REGEX = String.format( "(?:(?:%1$s)|[^{}\\[\\]]|(?:%2$s)|(?:%3$s))+", ESCAPED_CHAR_REGEX, CHAR_CLASS_REGEX, RANGE_REGEX );
	private static final String PARAMETER_REGEX = String.format( "\\{\\s*([_a-zA-Z][_a-zA-Z0-9]*)(?:\\s*:\\s*(%s))?\\s*\\}", PARAMETER_FORMAT_REGEX );
//	private static final String PARAMETER_REGEX = "\\{\\s*([_a-zA-Z][_a-zA-Z0-9]*)\\s*\\}"; // to remove the regex support for parameters, use just this line
	private static final Pattern PARAMETER_PATTERN = Pattern.compile( PARAMETER_REGEX );
	private static final int PARAMETER_NAME_GROUP = 1;
	private static final int PARAMETER_REGEX_GROUP = 2;
	
	// the following are used to escape path characters so they don't interfere with the path regex
	// NOTE: turns out that Pattern.quote should do the trick, but keeping these just in case
    // private static final String REGEX_CHAR_SEARCH_REGEX = "([\\\\\\*\\+\\[\\]\\?\\(\\)\\{\\}\\.\\^\\$])"; // any of these characters => \*+[]?(){}.^$
    // private static final Pattern REGEX_CHAR_SEARCH_PATTERN = Pattern.compile( REGEX_CHAR_SEARCH_REGEX ); 
    // private static final String REGEX_CHAR_REPLACE = "\\\\$1";

	// this regex is based on the segment/pchar definition from RFC 3986 (Appendix A): http://www.ietf.org/rfc/rfc3986.txt
	// JAX RS: http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html "By default, the URI variable must match the regular expression "[^/]+?"
	private static final String UNRESERVED_CHAR_REGEX = "[a-zA-Z0-9\\-\\.\\_\\~]";
	private static final String PCT_ENCODED_CHAR_REGEX = "%[0-9a-fA-F][0-9a-fA-F]"; // a percent encoded character (e.g. space is %20)
	private static final String SUB_DELIMS_CHAR_REGEX = "[!$&'()*+,;=]";
	private static final String PCHAR_REGEX = String.format( "(?:%s)|(?:%s)|(?:%s)|(?:[:@])", UNRESERVED_CHAR_REGEX, PCT_ENCODED_CHAR_REGEX, SUB_DELIMS_CHAR_REGEX );
	// TODO: don't share this like I am here
	static final String SEGMENT_COMPONENT_REGEX = String.format( "(?:%s)+", PCHAR_REGEX );
	private static final String PARAMETER_COMPONENT_REGEX = String.format( "(?:%1$s)*(?:%2$s)(?:%1$s)*", PCHAR_REGEX, PARAMETER_REGEX );
	private static final String PATH_COMPONENT_REGEX = String.format( "(?:(?:%s)|(?:%s))", SEGMENT_COMPONENT_REGEX, PARAMETER_COMPONENT_REGEX );
	private static final String PATH_REGEX = String.format( "(%1$s(?:/%1$s)*/?)?", PATH_COMPONENT_REGEX );
	private static final String METHOD_REGEX = "(?:GET|POST|PUT|DELETE|HEAD)";
	private static final String METHODS_REGEX = String.format( "%1$s(?:\\s*\\|\\s*%1$s)*", METHOD_REGEX );
	private static final String METHODS_PATH_REGEX = String.format( "^\\s*(%s)\\s*:\\s*(%s)$", METHODS_REGEX, PATH_REGEX );
	private static final Pattern METHODS_PATH_PATTERN = Pattern.compile( METHODS_PATH_REGEX );
	private static final int METHOD_GROUP = 1;
	private static final int PATH_GROUP = 2;

	private final List<String> verbs;
	private final ResourceOperation.Mode mode;
	private final String specifiedPath; // need more here to interpret due to data inside of it
	private final String orderingPath; // a helper path to determining order of match
	private final String parameterPath; // a path, mainly for external use, that has just parameter names (no regex's)
	private final String pathRegex;
	private final Pattern pathPattern;
	private final ResourceType resourceType;
	
	private final List<String> pathParams;
	private final List<ResourceMethodParameter> methodParameters;
	private final ResourceMethodReturn methodReturn;

	private final Method method;
	private final ContractStatus status = new ContractStatus( );
	
	/**
	 * Package constructor taking in all needed data.
	 * @param theName the visual/status name to give the method
	 * @param theMethodPath the actual path, relative to the root, the method will be off of
	 * @param theMethod the reflected method represented by this call
	 * @param theResourceType the resource that contains this method
	 * @param theResourceFacility the resource facility to use to help setup the method, its parameters, etc
	 */
	ResourceMethod( 
			String theName, 
			String theDescription, 
			String[] theVersions, 
			String theMethodPath,
			Method theMethod, 
			ResourceOperation.Mode theMode, 
			ResourceType theResourceType, 
			ResourceFacility theResourceFacility ) {
		super( theName, theDescription, theVersions, theResourceType );
		
		NameValidator nameValidator = NameManager.getValidator( ResourceMethod.RESOURCE_METHOD_NAME_VALIDATOR );

		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Resource method '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theMethod, "need a reflected method" );
		Preconditions.checkNotNull( theMode, "need a mode" );
		Preconditions.checkNotNull( theResourceType, "need a resource type" );
		Matcher pathMatcher = METHODS_PATH_PATTERN.matcher( theMethodPath );
		Preconditions.checkArgument( pathMatcher.matches( ), String.format( "the path string '%s' on '%s.%s' does not conform to the pattern '%s'", theMethodPath, theResourceType.getType( ).getName(), theMethod.getName(), METHODS_PATH_REGEX ) );
		Preconditions.checkNotNull( theResourceFacility, "need the resource facility" );
		Preconditions.checkArgument( theResourceType.supports( this.getSupportedVersions() ), String.format( "Resource method '%s.%s' has a contract version not supported by the Resource type", theResourceType.getType().getSimpleName(), theMethod.getName() ) );

		String verbsString = pathMatcher.group( METHOD_GROUP );
		String[] verbsArray = verbsString.split( "\\s*\\|\\s*" );
		ArrayList<String> verbsList = new ArrayList<String>( );
		for( String verb : verbsArray ) {
			verbsList.add( new String( verb ) );
		}
		verbs = Collections.unmodifiableList( verbsList );
		mode = theMode;
		
		specifiedPath = pathMatcher.group( PATH_GROUP );
		method = theMethod;
		resourceType = theResourceType;
		
		List<String> newPathParams = new ArrayList<String>( );
				
		// FIRST, we need to create the proper path to match and save the path references generated
		String[] paths = generatePaths( specifiedPath, resourceType.getBoundPath(), newPathParams );
		parameterPath = paths[ 2 ];
		orderingPath = paths[ 1 ];
		pathRegex = paths[ 0 ];
		pathPattern = Pattern.compile( pathRegex );
		pathParams = Collections.unmodifiableList( newPathParams );

		
		// SECOND, make sure we have proper parameters
		Type[] paramTypes = method.getGenericParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		Annotation[] paramAnnotations;
		Annotation paramAnnotation;
		ResourceMethodParameter parameter;
		JavaType paramType;
		List<ResourceMethodParameter> newMethodParameters = new ArrayList<ResourceMethodParameter>( );
		
		// we have to iterate over all of the annotations on parameters
		// make sure they are our type and then store what is important
		for( int paramCount = 0; paramCount < paramTypes.length; paramCount += 1 ) {
			// get the parameter type, the parameter generic type and the annotations for the parameter
			paramType = new JavaType( paramTypes[ paramCount ] );
			paramAnnotations = annotations[ paramCount ];
			if( paramAnnotations.length <= 0 ) {
				throw new IllegalStateException( String.format( "Parameter %s of type '%s' on method '%s.%s' does not have a parameter annotation.", paramCount + 1, paramType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
				// we have a problem
			} else {
				for( int annotationCount = 0; annotationCount < paramAnnotations.length; annotationCount += 1 ) {
					parameter = null;
					paramAnnotation = paramAnnotations[ annotationCount ];

					// so check the type of parameter and then create the parameter reference 
					if( paramAnnotation instanceof PathParam ) {
						parameter = generatePathParameter( ( PathParam )paramAnnotation, paramType, paramCount, theResourceFacility );
						
					} else if( paramAnnotation instanceof RequestParam ) {
						parameter = generateRequestParameter( ( RequestParam )paramAnnotation, paramType, paramCount, theResourceFacility );
					
					} else if( paramAnnotation instanceof HeaderParam ) {
						parameter = generateHeaderParameter( ( HeaderParam )paramAnnotation, paramType, paramCount, theResourceFacility );
						
					} else if( paramAnnotation instanceof CookieParam ) {
						parameter = generateCookieParameter( ( CookieParam )paramAnnotation, paramType, paramCount, theResourceFacility );

					} else if( paramAnnotation instanceof ContextParam ) {
						parameter = generateContextParameter( ( ContextParam )paramAnnotation, paramType, paramCount, theResourceFacility );
					}
					
					// make sure we made a parameter, verify the state, and save if
					// we don't except here if a parameter info isn't made since it
					// could be any annotation that refers to the parameter ... 
					// we verify that a parameter was created later
					if( newMethodParameters.size() >= paramCount + 1 ) {
						// we have more than one annotations on this parameter
						throw new IllegalStateException( String.format( "Parameter %s of type '%s' on method '%s.%s' has more than one parameter annotation.", paramCount + 1, paramType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
					} else if( parameter != null ) {
						// finally, we save the parameter
						newMethodParameters.add( parameter );
					}
				}
			}
		}
		// make sure we have an annotation for each parameter
		if( newMethodParameters.size() < paramTypes.length ) {
			throw new IllegalStateException( String.format( "Method '%s.%s' has parameters without a parameter annotation.", method.getDeclaringClass().getName(), method.getName() ) );
		}
		// make sure that every path parameter mentioned in the path was
		// found as a parameter in the method, we should be able to look
		// over the list of path parameters compare to method parameters
		// a simple way is to keep a count and then verify we used them
		// all, but it wont' say which one, so we do this instead
		// to verify
		for( String pathParam : pathParams ) {
			//boolean found = false;
			for( ResourceMethodParameter methodParam : newMethodParameters ) {
				if( pathParam.equals( methodParam.getValueName() ) ) {
					if( !methodParam.getSource().equals( ParameterSource.PATH ) ) {
						throw new IllegalStateException( String.format( 
								"Method '%s.%s' has a path referring to parameter '%s' but the parameter in the method says the parameter is sourced from '%s'.", 
								method.getDeclaringClass().getName(), 
								method.getName(),
								pathParam,
								methodParam.getSource( ) ) );
					} else {
						//found = true;
						break;
					}
				}
			}
//			TODO: not ensuring the path parameters have a method equivalent, just in case they want to identify a dynamic area, but not care about the value			
//			if( !found ) {
//				// means we have a path parameter that isn't being used
//				throw new IllegalStateException( String.format( "Method '%s.%s' has a path parameterparameters without a parameter annotation.", method.getDeclaringClass().getName(), method.getName() ) );
//			}
		}
		
		methodParameters = Collections.unmodifiableList( newMethodParameters );

		// THIRD, look at the return type and make sure we have something appropriate

		JavaType returnType = new JavaType( method.getGenericReturnType( ) );		
		JsonTypeReference typeReference;
		if( Void.TYPE.equals( returnType ) ) {
			// void returns are very simple
			this.methodReturn = new ResourceMethodReturn( returnType, this );
			
		} else if( ResourceResult.class.isAssignableFrom( returnType.getUnderlyingClass() ) ) {
			// if this is the special resource response type, then we need
			// to pull the data down a bit differently to get the actual type
			returnType = new JavaType( ( ( ParameterizedType ) returnType.getType() ).getActualTypeArguments( )[ 0 ] );
			typeReference = theResourceFacility.getJsonFacility().getTypeReference( returnType );
			if( typeReference == null ) {
				throw new IllegalStateException( String.format( "Return type '%s' on method '%s.%s' could not be analyzed because a translator could not be found.", returnType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
			} else {
				this.methodReturn = new ResourceMethodReturn( returnType, true, typeReference.getToJsonTranslator(), this );
			}

		} else {
			// otherwise the type is just something we are looking to return
			typeReference = theResourceFacility.getJsonFacility().getTypeReference( returnType );
			if( typeReference == null ) {
				throw new IllegalStateException( String.format( "Return type '%s' on method '%s.%s' could not be analyzed because a translator could not be found.", returnType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
			} else {
				this.methodReturn = new ResourceMethodReturn( returnType, false, typeReference.getToJsonTranslator(), this );
			}
		}
	}

	/**
	 * Generates 1) a version of the path that is expanded to cover 
	 * the regular expression to match url parameters, 2) a version
	 * to help with building order for matching and 3) a version
	 * that contains just the names of the parameters, mainly 
	 * used for exporting externally. The order version of the path
	 * helps with the situation where you have ...
	 * <pre>
	 *   /collection/{id}
	 *   /collection/find
	 * </pre>
	 * ... since '{id}' will match 'find'.
	 * @param thePath the path to generate a regex version of
	 * @param thePathParams collects the list of path parameters found
	 * @return three element array with a regex version of the path, the compare/order version of the path and version containing just parameter names
	 */
	private static String[] generatePaths( String thePath, String theRootPath, List<String> thePathParams ) {
		// I hate not having out parameters but want to keep my class fields final 
		// and don't want to create a class just for a return, so an array it is
		String[] paths = new String[ 3 ];
		StringBuilder regexPathBuilder = new StringBuilder( '^' );
		StringBuilder orderPathBuilder = new StringBuilder( );
		StringBuilder paramPathBuilder = new StringBuilder( );
		Matcher parameterMatcher = PARAMETER_PATTERN.matcher( thePath );
		String paramName;

		// since the path here is relative, we append the root to make it a full path but we only
		// do this if the path isn't matching the root spot (and if the root wasn't asking for a /) 

		regexPathBuilder.append( theRootPath );
		orderPathBuilder.append( theRootPath );
		paramPathBuilder.append( theRootPath );
		if( !theRootPath.endsWith( "/" ) && !Strings.isNullOrEmpty( thePath ) ) {
			regexPathBuilder.append( "/" );
			orderPathBuilder.append( "/" );
			paramPathBuilder.append( "/" );
		}
		
		// we need to go through the path given, extract and store the parameters given and
		// create a regex that will be used to match the path given to us by the servlet
		int lastEnd = 0;
		String helper;
		// we start by looking for strings that match our parameter notion {name}
		while( parameterMatcher.find( ) ) {
			if( lastEnd < parameterMatcher.start() ) {
				// if we found a parameter we look to see if there is text before the parameter match  
				// that we need to copy to our new path, we also escape the string in case it contains
				//  regex characters (since we are building a regex)
				helper = thePath.substring( lastEnd, parameterMatcher.start( ) );
				regexPathBuilder.append( Pattern.quote( helper ) );
				// for the order path, we don't care about escaping since we aren't treating it as a regex
				orderPathBuilder.append( helper );
				// for the param path, we don't need to worry about escaping either
				paramPathBuilder.append( helper );
			}
			// get the parameter name
			paramName = parameterMatcher.group( PARAMETER_NAME_GROUP );
			// we save the parameter name for later use, the index in the array is important since it will
			// represent the regex group location to we can  later tell which parameter name the match 
			// will belong to . . . but WE CAN ONLY HAVE ONE!
			if( thePathParams.contains( paramName ) ) {
				// yes doing a linear search isn't exactly speed, but there should be a small
				// number in here and rather not take up space storing another structure 
				throw new IllegalArgumentException( String.format( "More than one definition for url parameter '%s' found in path '%s'.", paramName, thePath ) );
			}
			thePathParams.add( paramName );
			// we now, instead of putting in the parameter name, put the regex that will be used to 
			// match FOR the parameter name
			if( parameterMatcher.groupCount() >= PARAMETER_REGEX_GROUP && parameterMatcher.group( PARAMETER_REGEX_GROUP ) != null ) { // it appears like a bug in the regex parsing; seeing 2 groups, but getting the second group returns null, so checking here
				// in this case, the developer gave a regex to use
				regexPathBuilder.append( '(' );
				regexPathBuilder.append( RegularExpressionHelper.toNoncapturingExpression( parameterMatcher.group( PARAMETER_REGEX_GROUP ) ) );
				regexPathBuilder.append( ')' );
				
			} else {
				// in this case, no regex was given in the param so we use the default (standard URL segment)
				regexPathBuilder.append( '(' );
				regexPathBuilder.append( SEGMENT_COMPONENT_REGEX );
				regexPathBuilder.append( ')' );
			}
			// for match path, we use # as a marker for regexes
			orderPathBuilder.append( "*" );
			// now we add the name to the param path
			paramPathBuilder.append( '{' );
			paramPathBuilder.append( paramName );
			paramPathBuilder.append( '}' );

			lastEnd = parameterMatcher.end( );
		}
		if( lastEnd < thePath.length() ) {
			// if we have more text to save, we save it and escape it as well to have a safe regex
			helper = thePath.substring( lastEnd, thePath.length() );
			regexPathBuilder.append( Pattern.quote( helper ) );
			// no need to escape here
			orderPathBuilder.append( helper );
			// no need to escape for param path either
			paramPathBuilder.append( helper );
		}
		regexPathBuilder.append( '$' );
		// return the regex version of the path and the matching version
		paths[ 0 ] = regexPathBuilder.toString();
		paths[ 1 ] = orderPathBuilder.toString();
		paths[ 2 ] = paramPathBuilder.toString();
		return  paths;
	}

	/**
	 * Helper method that creates a parameter for a path reference.
	 * It assumes path parameters were setup already.
	 * @param theParamAnnotation the annotation for the path parameter
	 * @param theParamType the type of the parameter
	 * @param theParamIndex the index of the parameter in the list of the method's parameters
	 * @return a parameter object
	 */
	private ResourceMethodParameter generatePathParameter( PathParam theParamAnnotation, JavaType theParamType, int theParamIndex, ResourceFacility theResourceFacility ) {
		ResourceMethodParameter parameter;
		
		int pathParamOffset;
		Translator translator;
		String paramName;
		paramName = ( ( PathParam )theParamAnnotation ).name( );
		
		// if we have any param type we verify we haven't seen one already
		// and if a path parameter we verify that we have seen that parameter 
		// in the path definition
		pathParamOffset = pathParams.indexOf( paramName );
		if( pathParamOffset < 0 ) {
			// oops, the name cannot be found anywhere
			throw new IllegalStateException( String.format( "Parameter %s of type '%s' on method '%s.%s' has a path parameter that refers to the unknown path parameter '%s'.", theParamIndex + 1, theParamType.getSimpleName(), method.getDeclaringClass().getName(), method.getName(), paramName ) );
		} else {
			// get the translation for this type, which should be a simple type
			// since this is a simple type, we only get the from string translators, not json ones
			translator = theResourceFacility.getJsonFacility().getFromStringTranslator( theParamType );
			if( translator == null ) {
				throw new IllegalStateException( String.format( "Parameter %s of type '%s' on method '%s.%s' is not recognized as a path type that can be translated.", theParamIndex + 1, theParamType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
			} else {
				parameter = new ResourceMethodParameter( ParameterSource.PATH, theParamType, theParamIndex, paramName, pathParamOffset, translator, theParamAnnotation.sensitive(), this );
			}
		}
		return parameter;
	}

	/**
	 * Helper method that creates a parameter for a request (query string or body) reference.
	 * @param theParamAnnotation the annotation for the path parameter
	 * @param theParamType the type of the parameter
	 * @param theParamIndex the index of the parameter in the list of the method's parameters
	 * @return a parameter object
	 */	
	private ResourceMethodParameter generateRequestParameter( RequestParam theParamAnnotation, JavaType theParamType, int theParamIndex, ResourceFacility theResourceFacility ) {
		ResourceMethodParameter parameter;
		
		Translator translator;
		String paramName;
		paramName = ( ( RequestParam )theParamAnnotation ).name( );

		// get the translator to use
		translator = theResourceFacility.getFromParameterTranslator(theParamType);
		if( translator == null ) {
			throw new IllegalStateException( String.format( "Could not find a translator for parameter %s of type '%s' on method '%s.%s'.", theParamIndex + 1, theParamType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
		} else {
			parameter = new ResourceMethodParameter( ParameterSource.REQUEST, theParamType, theParamIndex, paramName, translator, theParamAnnotation.sensitive(), this );
		}
		return parameter;
	}

	/**
	 * Helper method that creates a parameter for a header reference.
	 * @param theParamAnnotation the annotation for the header parameter
	 * @param theParamType the type of the parameter
	 * @param theParamIndex the index of the parameter in the list of the method's parameters
	 * @return a parameter object
	 */	
	private ResourceMethodParameter generateHeaderParameter( HeaderParam theParamAnnotation, JavaType theParamType, int theParamIndex, ResourceFacility theResourceFacility ) {
		ResourceMethodParameter parameter;
		
		Translator translator;
		String paramName;
		paramName = ( ( HeaderParam )theParamAnnotation ).name( );

		// get the translator to use
		translator = theResourceFacility.getFromParameterTranslator(theParamType);
		if( translator == null ) {
			throw new IllegalStateException( String.format( "Could not find a translator for parameter %s of type '%s' on method '%s.%s'.", theParamIndex + 1, theParamType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
		} else {
			parameter = new ResourceMethodParameter( ParameterSource.HEADER, theParamType, theParamIndex, paramName, translator, theParamAnnotation.sensitive(), this );
		}
		return parameter;
	}
	
	/**
	 * Helper method that creates a parameter for a cookie reference.
	 * @param theParamAnnotation the annotation for the cookie parameter
	 * @param theParamType the type of the parameter
	 * @param theParamIndex the index of the parameter in the list of the method's parameters
	 * @return a parameter object
	 */	
	private ResourceMethodParameter generateCookieParameter( CookieParam theParamAnnotation, JavaType theParamType, int theParamIndex, ResourceFacility theResourceFacility ) {
		ResourceMethodParameter parameter;
		
		String paramName;
		paramName = ( ( CookieParam )theParamAnnotation ).name( );

		if( Cookie.class.isAssignableFrom( theParamType.getUnderlyingClass() )) {
			parameter = new ResourceMethodParameter( ParameterSource.COOKIE, theParamType, theParamIndex, paramName, null, theParamAnnotation.sensitive(), this );
		} else {
			// get the translator to use
			Translator translator = theResourceFacility.getFromParameterTranslator(theParamType);
			if( translator == null ) {
				throw new IllegalStateException( String.format( "Could not find a translator for parameter %s of type '%s' on method '%s.%s'.", theParamIndex + 1, theParamType.getSimpleName(), method.getDeclaringClass().getName(), method.getName() ) );
			} else {
				parameter = new ResourceMethodParameter( ParameterSource.COOKIE, theParamType, theParamIndex, paramName, translator, theParamAnnotation.sensitive(), this );
			}
		}
		return parameter;
	}

	/**
	 * Helper method that creates a parameter for a header reference.
	 * @param theParamAnnotation the annotation for the header parameter
	 * @param theParamType the type of the parameter
	 * @param theParamIndex the index of the parameter in the list of the method's parameters
	 * @return a parameter object
	 */	
	private ResourceMethodParameter generateContextParameter( ContextParam theParamAnnotation, JavaType theParamType, int theParamIndex, ResourceFacility theResourceFacility ) {
		ResourceMethodParameter parameter;
		
		// the constructor will validate as needed
		parameter = new ResourceMethodParameter( ParameterSource.CONTEXT, theParamType, theParamIndex, theParamAnnotation.sensitive(), this );
		return parameter;
	}
	
	/**
	 * The HTTP verbs that this method will run via.
	 * @return the collection of strings of the http verb
	 */
	public List<String> getVerbs( ) {
		return verbs;
	}
	
	/**
	 * Returns how the method was declared to execute.
	 * This may return DEFAULT which means you need to
	 * look to the type to see how it should run.
	 * If you want to see how it should run use 
	 * getUsableMethod( ).
	 * @return how the method wsa declared to execute.
	 */
	public ResourceOperation.Mode getDeclaredMode( ) {
		return mode;
	}

	/**
	 * Returns how the method should be 
	 * @return how the method wsa declared to execute.
	 */
	public ResourceOperation.Mode getUsableMode( ) {
		return mode == Mode.DEFAULT ? this.resourceType.getMode( ) : mode;
	}

	/**
	 * Returns the parameters for the method.
	 * @return the list of parameters
	 */
	public List<ResourceMethodParameter> getParameters( ) {
		return this.methodParameters;
	}
	
	/**
	 * Returns the return of the method.
	 * @return the return of the method
	 */
	public ResourceMethodReturn getReturn( ) {
		return this.methodReturn;
	}
	
	/**
	 * The reflected method this resource method is associated with.
	 * @return the reflected method
	 */
	public Method getMethod( ) {
		return method;
	}
	
	/**
	 * The original string path outlined by the method developer. 
	 * @return the path specified by the method developer
	 */
	public String getSpecifiedPath( ) {
		return specifiedPath;
	}
	
	/**
	 * The path containing the parameters, but stripped of 
	 * any regex's.
	 * @return the path containing just the parameter names
	 */
	public String getParameterPath( ) {
		return parameterPath;
	}
	
	/**
	 * Package level helper returning a version of the path 
	 * to aid in determining order of potentially conflicting
	 * paths, for example ...
	 * 	 * <pre>
	 *   /collection/{id}
	 *   /collection/find
	 * </pre>
	 * ... since '{id}' will match 'find'.
	 * @return the ordering path
	 */
	String getOrderingPath( ) {
		return orderingPath; 
	}
	
	/**
	 * The actual regular expression version of the path, including
	 * root, used when running.
	 * @return the path used to match when running
	 */
	public String getMatchingPath( ) {
		return this.pathRegex;
	}
	
	/**
	 * Returns the resource type this method is a part of.
	 * @return the resource type this method is a part of
	 */
	public ResourceType getResourceType( ) {
		return this.resourceType;
	}
	
	
	/**
	 * Returns the status around the method being called.
	 * @return the status for this particular method
	 */
	public ContractStatus getStatus( ) {
		return this.status;
	}

	/**
	 * A simple method that will indicate if the resource method will
	 * match the given path.
	 * @param aPath the path to match against
	 * @return true if matches, false otherwise
	 */
	public boolean matchesPath( String aPath ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( aPath ), "need a path to compare" );
		Matcher pathMatcher = pathPattern.matcher( aPath );
		
		return pathMatcher.matches( );
	}
	
	/// *********** new matching work
	public static class MatchStatus {
		private final int parameterMatches;
		private final int parameterMisses;
		private final int pathIndex;
		private final Matcher pathMatcher;
		
		public MatchStatus( int theParameterMatches, int theParameterMisses, Matcher thePathMatcher, int thePathIndex ) {
			parameterMatches = theParameterMatches;
			parameterMisses = theParameterMisses;
			pathMatcher = thePathMatcher;
			pathIndex = thePathIndex;			
		}
		
		public int getParameterMatches( ) {
			return parameterMatches;
		}

		public int getParameterMisses( ) {
			return parameterMisses;
		}
		
		public Matcher getPathMatcher( ) {
			return pathMatcher;
		}

		public int getPathIndex( ) {
			return pathIndex;
		}
	}
	
	public MatchStatus match( HttpServletRequest theRequest, int thePathIndex ) {
		// first we need to get the request URI and ensure it matches
		String uri = theRequest.getRequestURI();

		Matcher pathMatcher = pathPattern.matcher( uri );
		boolean matched = pathMatcher.matches( );
		MatchStatus status = null;
		int parameterMatches = 0;
		int parameterMisses = 0;
		
		// TODO: a perf boast could be to count other paramter types as matches, so we can check if all matched
		if( matched ) {
			for( ResourceMethodParameter parameter: this.methodParameters ) {
				if( parameter.getSource() == ParameterSource.REQUEST ) {
					if( theRequest.getParameter(parameter.getValueName()) != null ) {
						parameterMatches += 1;
					} else {
						parameterMisses += 1;
					}
//				} else {
					// this was being done to help if we match parameters where we can
//					parameterMatches += 1;
				}
			}
			status = new MatchStatus( parameterMatches, parameterMisses, pathMatcher, thePathIndex );
		}
		return status;
	}
	/// *********** new matching work
	
	/**
	 * This is called to execute the given method. It will first attempt
	 * to match the path if successful, it will execute the method.
	 * @param theObject the instance to run the method against
	 * @param theRequest the request to extra URI and parameter information for execution
	 * @return a result object describing the success or failure
	 */
	public ResourceMethodResult execute( 
			Object theObject, 
			HttpServletRequest theRequest, 
			HttpServletResponse theResponse, 
			OperationContext theContext ,
			Matcher thePathMatcher, 
			ResourceFacility theResourceFacility, 
			AsyncState theAsyncState ) {

		// TODO: move this entire method out
		logger.info( 
				"Executing, {}, resource method '{}.{}' (aka '{}').", new Object[]{
				 theAsyncState != null ? "non-blocking" : "blocking",
				this.resourceType.getType().getName(), 
				this.method.getName( ), 
				this.getName( ) } );
		StringBuilder loggedParameterBuilder = new StringBuilder( );
		ResourceMethodResult result = null;
		int loggedParameters = 0;
		// start the execution timer		
		long startTimestamp = System.nanoTime(); 
		try {
			// first we need to get the request URI and ensure it matches
			String uri = theRequest.getRequestURI();
	
			// if we have a match, we need to generate the parameters to use 
			Object[] parameters	= new Object[ this.methodParameters.size( ) ];
			String stringValue;
			Object actualValue;
			ParameterSource parameterSource;
			Map<String, Cookie> cookieMap = null; // set to null since we don't always use it
			
			// NOTE: I could support the idea of default values here, which would be kind cool

			// NOTE: if I want to support the idea of supporting overloads then I could
			//       use a regex to match to the 'first' operations and then start looking
			//       at the parameters, pull them out, see if they exist . . . then do
			//       the next one and see
			
			for( ResourceMethodParameter parameter : this.methodParameters ) {
				try {
					parameterSource = parameter.getSource( );
					stringValue = null;
					actualValue = null;
					if( parameterSource == ParameterSource.CONTEXT ) {
						// if we have a context parameter, we need to set the context value
						if( parameter.getContextValue() == ContextValue.HTTP_REQUEST ) {
							parameters[ parameter.getMethodParamOffset() ] = theRequest;
						} else if( parameter.getContextValue() == ContextValue.HTTP_RESPONSE ) {
							parameters[ parameter.getMethodParamOffset() ] = theResponse;
						} else {
							// then they want the context
							parameters[ parameter.getMethodParamOffset() ] = theContext;
						}
					} else if( parameterSource == ParameterSource.COOKIE && parameter.getCookieValue() == CookieValue.COOKIE ) {
						// see if we have the map, otherwise we create the map
						if( cookieMap == null ) {
							cookieMap = processCookies( theRequest.getCookies() );
						}
						// we know the map was created and so we find the cookie, which may be null
						parameters[ parameter.getMethodParamOffset() ] = cookieMap.get( parameter.getValueName( ) );
						
					} else {
						// if we have a value parameter source, then we need to retrieve and convert
						if( parameterSource == ParameterSource.PATH ) {
							// this means we have a reference to something in the url path
							stringValue = UrlEncoding.decode( theRequest.getCharacterEncoding( ), thePathMatcher.group( parameter.getPathReference() + 1 ) );
						} else if( parameterSource == ParameterSource.REQUEST ) {
							// this means it is a reference to a query string param or post body url encoded item
							stringValue = theRequest.getParameter( parameter.getValueName() );
						} else if( parameterSource == ParameterSource.HEADER ) {
							// this means we have a header reference
							stringValue = theRequest.getHeader( parameter.getValueName() );
						} else if( parameterSource == ParameterSource.COOKIE ) {
							// at this point we know this is a value we are looking for, not a Cookie type
							// see if we have the map, otherwise we create the map
							if( cookieMap == null ) {
								cookieMap = processCookies( theRequest.getCookies() );
							}
							// we know the map was created and so we find the cookie
							Cookie cookie = cookieMap.get( parameter.getValueName( ) );
							if( cookie != null ) {
								stringValue = cookie.getValue( );
							}
						} else {
							throw new IllegalStateException( String.format( "Parameter '%s' for request '%s', using path '%s', is using an unsupported source of '%s'.", parameter.getValueName(), this.getName(), this.parameterPath, parameterSource ) );
						}
						
						// the following is for logging purposes
						if( theContext.getResponseTarget() == Readability.MACHINE ) {
							if( loggedParameters > 0 ) {
								loggedParameterBuilder.append( ", " );
							}
						} else {
							loggedParameterBuilder.append( "\n\t" );
						}
						loggedParameterBuilder.append( parameter.getValueName( ) );
						loggedParameterBuilder.append( " = " );
						if( parameter.isSensitive( ) ) {
							loggedParameterBuilder.append( "<SENSITIVE>" );
						} else {
							loggedParameterBuilder.append( stringValue );
						}
						loggedParameters += 1;
						
						actualValue = parameter.translate( stringValue );
						if( actualValue == null && parameter.getType().getUnderlyingClass( ).isPrimitive() ) {
							// if we have a null value and primitive, we have a problem
							throw new TranslationException( String.format( "Attempting to set primitive type '%s' to null.", parameter.getType().getName() ) );
						} else {
							//TODO: see if there is validation support
							parameters[ parameter.getMethodParamOffset() ] = actualValue;
						}
					}
					
				// the exceptions below are handled here since they are definitely about the data coming in so no one else is meant to trap
				} catch( JsonParseException e ) {
					// if this happens then we have a problem with what the caller sent so we return now with a failed result
					result = new ResourceMethodResult( Status.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s', using path '%s', is not valid JSON.", parameter.getValueName(), this.getName(), this.parameterPath ), e );
				} catch( TranslationException e) {
					// if this happens then we have a problem with what the caller sent so we return now with a failed result
					result = new ResourceMethodResult( Status.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s', using path '%s', is not the expected format.", parameter.getValueName(), this.getName(), this.parameterPath ), e );
				} catch( DataSiteException e ) {
					// if this happens then we passed at least parts of translation, but still saw a failure, typically due to things like attempting to assign
					// null to a primitive type, etc, so we return now with a failed result
					result = new ResourceMethodResult( Status.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s, using path '%s',' was not assignable ... check for null values when they aren't expected (e.g using primitive types).", parameter.getValueName(), this.getName(), this.parameterPath ), e );
				}
			}
			// if we have a result we errored out
			// so we check to make sure before we 
			// process
			if( result == null ) {
		  		//we have the parameters so invoke the method, which may cause an exception (caught in the outer try)
				Object typeLessResult = method.invoke( theObject, parameters );
				if( this.methodReturn.isResourceResponse() ) {
					ResourceResult<?> resourceResult = ( ResourceResult<?> )typeLessResult; 
					if( resourceResult == null ) {
						result = new ResourceMethodResult( Status.LOCAL_ERROR, null, String.format( "ResourceResult was null for '%s'.", uri ), null );
					} else {
						result = new ResourceMethodResult( ( JsonElement )this.methodReturn.translate( resourceResult.getValue( ) ), resourceResult );
					}
				} else if( this.methodReturn.isVoid() ) {
					// the void return type case is just an unknown empty object
					result = new ResourceMethodResult( new JsonObject( ) );
				} else {
					// the non-void return type case will translate the result
					result = new ResourceMethodResult( ( JsonElement )this.methodReturn.translate( typeLessResult ) );
				}
			}

		} catch (InvocationTargetException e ) {
			// so the called method excepted, so we need to grab the 
			// the cause and see what kind of problem we had
			result = theResourceFacility.toResult( this, e.getCause( ) ); 
			
		} catch( Exception e ) {
			String message = String.format( 
					"Unmanaged exception '%s' occurred while running '%s.%s'.",
					e.getClass( ).getSimpleName( ), 
					this.getResourceType().getType().getSimpleName(), 
					this.getMethod( ).getName( ) );
			logger.error( message, e );
			result = new ResourceMethodResult( 
					Status.LOCAL_ERROR, 
					FailureSubcodes.UNHANDLED_EXCEPTION,
					message,
					e );

		} finally {
			// record when we ended
			long executionTime = System.nanoTime( ) - startTimestamp;
			status.recordExecutionTime( executionTime );
			logger.info( 
					"Executed, {}, resource method '{}.{}' (aka '{}') in {} ms with {} parameter(s) resulting in status '{}'. {}", new Object[] {
					( theAsyncState != null ? "non-blocking" + ( theAsyncState.hasCompleted() ? " though timed-out" : "" ) : "blocking" ),
					this.resourceType.getType().getName(),
					this.method.getName( ), 
					this.getName( ), 
					( ( double )executionTime ) * 0.000001, 
					loggedParameters,
					result == null ? "unknown" : result.getCode(),
					loggedParameterBuilder.toString() } );
		}
		return result;
	}
	
	/**
	 * Helper method that takes the cookies from the request 
	 * and creates a map from them.
	 * @param theCookies the cookies from the request
	 * @return the map of cookies, mapping name to cookie and if no cookies are found this will return an empty map.
	 */
	private Map<String, Cookie> processCookies( Cookie[] theCookies ) {
		Map<String, Cookie> cookieMap;
		
		if( theCookies != null ) {
			cookieMap = new HashMap<String, Cookie>( theCookies.length );
			
			for( Cookie cookie : theCookies ) {
				cookieMap.put( cookie.getName( ), cookie );
			}
		} else {
			// the getCookies call can return null ...
			cookieMap = new HashMap<String, Cookie>( 0 );
		}
		return cookieMap;
	}

//	/**
//	 * This is called to execute the given method. It will first attempt
//	 * to match the path if successful, it will execute the method.
//	 * @param theObject the instance to run the method against
//	 * @param theRequest the request to extra URI and parameter information for execution
//	 * @return returns null if it didn't match, otherwise a result objec describing the success or failure
//	 */
//	public ResourceMethodResult execute( Object theObject, HttpServletRequest theRequest, HttpServletResponse theResponse, ResourceFacility theResourceFacility ) {
//		// TODO: move this out
//		// start the execution timer		
//		long startTimestamp = System.nanoTime(); 
//		try {
//			// first we need to get the request URI and ensure it matches
//			String uri = theRequest.getRequestURI();
//	
//			Matcher pathMatcher = pathPattern.matcher( uri );
//			boolean matched = pathMatcher.matches( );
//			
//			if( matched ) {
//				// record that a request to the method came in
//				status.recordReceivedRequest();
//
//				// if we have a match, we need to generate the parameters to use 
//				Object[] parameters	= new Object[ this.methodParameters.size( ) ];
////				int matchedParameters = 0;
//				String stringValue;
//				Object actualValue;
//				ParameterSource parameterSource;
//				// NOTE: I could support the idea of default values here, which would be kind cool
//	
//				// NOTE: if I want to support the idea of supporting overloads then I could
//				//       use a regex to match to the 'first' operations and then start looking
//				//       at the parameters, pull them out, see if they exist . . . then do
//				//       the next one and see
//				
//				for( ResourceMethodParameter parameter : this.methodParameters ) {
//					try {
//						parameterSource = parameter.getSource( );
//						if( parameterSource == ParameterSource.CONTEXT ) {
//							// if we have a context parameter, we need to set the context value
//							if( parameter.getContextValue() == ContextValue.HTTP_REQUEST ) {
//								parameters[ parameter.getMethodParamOffset() ] = theRequest;
//							} else {
//								parameters[ parameter.getMethodParamOffset() ] = theResponse;
//							}
//						} else {
//							// if we have a value parameter source, then we need to retrieve and convert
//							if( parameterSource == ParameterSource.PATH ) {
//								// this means we have a reference to something in the url path
//								stringValue = UrlEncoding.decode( theRequest.getCharacterEncoding( ), pathMatcher.group( parameter.getPathReference() + 1 ) );
//							} else if( parameterSource == ParameterSource.REQUEST ) {
//								// this means it is a reference to a query string param or post body url encoded item
//								stringValue = theRequest.getParameter( parameter.getValueName() );
//							} else {
//								// this means we have a header reference
//								stringValue = theRequest.getHeader( parameter.getValueName() );
//							}
//							
//	//						if( stringValue != null ) {
//	//							// we track how many we matched to see 
//	//							// if we should be considered a match
//	//							matchedParameters += 1;
//	//						}
//							// 
//							actualValue = parameter.translate( stringValue );
//							if( actualValue == null && parameter.getType().isPrimitive() ) {
//								// if we have a null value and primitive, we have a problem
//								throw new TranslationException( String.format( "Attempting to set primitive type '%s' to null.", parameter.getType().getName() ) );
//							} else {
//								parameters[ parameter.getMethodParamOffset() ] = actualValue;
//							}
//						}
//						
//					// the exceptions below are handled here since they are definitely about the data coming in so no one else is meant to trap
//					} catch( JsonParseException e ) {
//						// if this happens then we have a problem with what the caller sent so we return now with a failed result
//						return new ResourceMethodResult( Failure.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s' is not valid JSON.", parameter.getValueName(), uri ), e );
//					} catch( TranslationException e) {
//						// if this happens then we have a problem with what the caller sent so we return now with a failed result
//						return new ResourceMethodResult( Failure.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s' is not the expected format.", parameter.getValueName(), uri ), e );
//					} catch( DataSiteException e ) {
//						// if this happens then we passed at least parts of translation, but still saw a failure, typically due to things like attempting to assign
//						// null to a primitive type, etc, so we return now with a failed result
//						return new ResourceMethodResult( Failure.CALLER_BAD_INPUT, null, String.format( "Parameter '%s' for request '%s' was not assignable, check for null values when they aren't expected.", parameter.getValueName(), uri ), e );
//					}
//				}
////				if( matchedParameters == parameters.length ) {
////			  		//we have the parameters so invoke the method, which may cause an exception (caught in the outer try)
//					Object result = method.invoke( theObject, parameters );
//					if( !this.methodReturn.isVoid() ) {
//						// the non-void return type case will translate the result
//						result = this.methodReturn.translate( result );
//					} else {
//						// the void return type case is just an unknown empty object
//						result = new JsonObject( );
//					}
//					// we return with the results 
//					return new ResourceMethodResult( ( JsonElement )result );
////				} else {
////					// we didn't have all the parameters
////					return null;
////				}
//			} else {
//				// we didn't match anything, so we return null
//				return null;
//			}
//
//		} catch (InvocationTargetException e ) {
//			// so the called method excepted, so we need to grab the 
//			// the cause and see what kind of problem we had
//			return theResourceFacility.toResult( this, e.getCause( ) );
//			
//		} catch( Exception e ) {
//			return new ResourceMethodResult( 
//					Failure.LOCAL_ERROR, 
//					FailureSubcodes.UNHANDLED_EXCEPTION,
//					String.format( 
//							"Unmanaged exception '%s' occurred while running '%s.%s'.",
//							e.getClass( ).getSimpleName( ), 
//							this.getResourceType().getType().getSimpleName(), 
//							this.getMethod( ).getName( ) ), 
//					e );
//
//		} finally {
//			// record when we ended
//			status.recordExecutionTime( System.nanoTime( ) - startTimestamp );
//		}
//	}
}