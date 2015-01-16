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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.reflection.JavaType;
import com.talvish.tales.parts.translators.Translator;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.system.Conditions;

/**
 * The details regarding a parameter on a method we are exposing in a resource.
 * @author jmolnar
 *
 */
public class ResourceMethodParameter {
	/**
	 * Indicates the type of parameter.
	 * @author jmolnar
	 *
	 */
	public enum ParameterSource {
		/**
		 * Parameter came from the header.
		 */
		HEADER,
		/**
		 * Parameter came from the path.
		 */
		PATH,
		/**
		 * Parameter came from the request body or query string.
		 */
		REQUEST,
		/**
		 * Parameter came from a cookie
		 */
		COOKIE,
		/**
		 * Parameter represents the context, such as HttpServletRequest or HttpServletResponse.
		 */
		CONTEXT,
	}
	
	/**
	 * Indicates the type of context being requested.
	 * @author jmolnar
	 *
	 */
	public enum ContextValue {
		/**
		 * Indicates this isn't a context value.
		 */
		NONE,
		/**
		 * Indicates it is looking for the HttpServletRequest object.
		 */
		HTTP_REQUEST,
		/**
		 * Indicates it is looking for the HttpServletResponse object.
		 */
		HTTP_RESPONSE,
		/**
		 * Indicates it is looking for the OperationContext object.
		 */
		OPERATION_CONTEXT,
	}
	
	/**
	 * Indicates the type of cookie.
	 * @author jmolnar
	 *
	 */
	public enum CookieValue {
		/**
		 * Indicates there isn't a cookie value.
		 */
		NONE,
		/**
		 * Indicates we are taking the value from the cookie and not the entire cookie.
		 */
		VALUE,
		/**
		 * Indicates we want the full cookie.
		 */
		COOKIE,
	}
	
	public static final String RESOURCE_METHOD_PARAMETER_NAME_VALIDATOR = "tales.contracts.resource_method_parameter_name";
	
	static {
		if( !NameManager.hasValidator( ResourceMethodParameter.RESOURCE_METHOD_PARAMETER_NAME_VALIDATOR ) ) {
			NameManager.setValidator( ResourceMethodParameter.RESOURCE_METHOD_PARAMETER_NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}
	
	// TODO: support translators in both directions AND getting/setting data (both directions)

	private final ParameterSource source;
	private final ContextValue contextValue;
	private final CookieValue cookieValue;
	private final ResourceMethod resourceMethod;
	private final JavaType type;
	private final int methodParamOffset;
	private final int pathReference;
	private final String valueName;
	private final boolean sensitive;
	private final Translator valueTranslator;

	/**
	 * Constructor called when there is a context parameter.
	 */
	ResourceMethodParameter( ParameterSource theSource, JavaType theType, int theMethodParamOffset, boolean isSensitive, ResourceMethod theMethod ) {
		this( theSource, theType, theMethodParamOffset, null, -1, null, isSensitive, theMethod );
	}
	
	/**
	 * Constructor called when there is a request parameter, header or cookie parameter referenced.
	 */
	ResourceMethodParameter( ParameterSource theSource, JavaType theType, int theMethodParamOffset, String theValueName, Translator theValueTranslator, boolean isSensitive, ResourceMethod theMethod ) {
		this( theSource, theType, theMethodParamOffset, theValueName, -1, theValueTranslator, isSensitive, theMethod );
	}

	/**
	 * Shared constructor called from above then request parameter is referenced, or directly if we have a path parameter.
	 */
	ResourceMethodParameter( ParameterSource theSource, JavaType theType, int theMethodParamOffset, String theValueName, int thePathReference, Translator theValueTranslator, boolean isSensitive, ResourceMethod theMethod ) {
		NameValidator nameValidator = NameManager.getValidator( ResourceMethodParameter.RESOURCE_METHOD_PARAMETER_NAME_VALIDATOR );
		
		Preconditions.checkNotNull( theSource, "the source of the parameter must be given");
		Preconditions.checkArgument( theSource == ParameterSource.PATH && thePathReference >= 0 || theSource != ParameterSource.PATH && thePathReference == -1, "if a path param, than path parameter must be non-negative, otherwise the path parameter must be -1" );
		Preconditions.checkNotNull( theMethod, "need a method" );
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkArgument( theMethodParamOffset >= 0, "need a non-negative parameter offset" );
		Preconditions.checkArgument( theSource == ParameterSource.CONTEXT || ( theSource != ParameterSource.CONTEXT && !Strings.isNullOrEmpty( theValueName ) ), "need a value name" );
		Preconditions.checkArgument( theSource == ParameterSource.HEADER || theSource == ParameterSource.COOKIE || theSource == ParameterSource.CONTEXT || ( theSource != ParameterSource.HEADER && nameValidator.isValid( theValueName ) ), String.format( "Parameter '%s' on resource method '%s' does not conform to validator '%s'.", theValueName, theMethod.getName( ), nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkArgument( theSource == ParameterSource.CONTEXT || theSource == ParameterSource.COOKIE || ( theSource != ParameterSource.CONTEXT && theSource != ParameterSource.COOKIE && theValueTranslator != null ), "need a translator" );
		source = theSource;
		type = theType;
		methodParamOffset = theMethodParamOffset;
		valueName = theValueName;
		sensitive = isSensitive;
		valueTranslator = theValueTranslator;
		pathReference = thePathReference;
		resourceMethod = theMethod;
		
		if( source == ParameterSource.CONTEXT ) {
			if( HttpServletRequest.class.isAssignableFrom( type.getUnderlyingClass() ) ) {
				this.contextValue = ContextValue.HTTP_REQUEST;				
			} else if( HttpServletResponse.class.isAssignableFrom( type.getUnderlyingClass() ) ) {
				this.contextValue = ContextValue.HTTP_RESPONSE;
			} else if( OperationContext.class.isAssignableFrom( type.getUnderlyingClass() ) ) {
				this.contextValue = ContextValue.OPERATION_CONTEXT;
			} else {
				throw new IllegalArgumentException( "attempting to request a context parameter, the parameter isn't a request or response object" );
			}
		} else {
			this.contextValue = ContextValue.NONE;
		}
		if( source == ParameterSource.COOKIE ) {
			if( Cookie.class.isAssignableFrom( type.getUnderlyingClass() ) ) {
				this.cookieValue = CookieValue.COOKIE;
			} else {
				Conditions.checkParameter( theValueTranslator != null, "theValueTranslator", "need a translator" );
				this.cookieValue = CookieValue.VALUE;
			}
		} else {
			this.cookieValue = CookieValue.NONE;
		}
	}
	
	/**
	 * Returns the source of the parameter data.
	 * @return the source of the parameter data
	 */
	public ParameterSource getSource( ) {
		return source;
	}
	
	/**
	 * Returns the context value type. 
	 * This is set when the parameter source is context.
	 * @return the context value type
	 */
	public ContextValue getContextValue( ) {
		return this.contextValue;
	}
	
	/**
	 * Returns the cookie value type. 
	 * This is set when the parameter source is a cookie.
	 * @return the cookie value type
	 */
	public CookieValue getCookieValue( ) {
		return this.cookieValue;
	}
	
	/**
	 * The method this parameter is a parameter for.
	 * @return the method the parameter is a parameter for.
	 */
	public ResourceMethod getResourceMethod( ) {
		return resourceMethod;
	}
	
	/**
	 * The name of the value in the either the path, the post body, etc..
	 * @return the name of the value.
	 */
	public String getValueName( ) {
		return valueName;
	}
	
	/**
	 * Returns if the parameter contains sensitive information.
	 * @return true if sensitive, false otherwise
	 */
	public boolean isSensitive( ) {
		return this.sensitive;
	}
	/**
	 * The type of the parameter.
	 * @return the type of the parameter
	 */
	public JavaType getType( ) {
		return type;
	}
	
	/**
	 * The offset of the parameter in the method's signature.
	 * @return the offset in the method signature
	 */
	public int getMethodParamOffset( ) {
		return this.methodParamOffset;
	}
	
	/**
	 * The offset in the path parameter when there is a path parameter specified.
	 * @return the offset in the path parameters, or -1 if not suitable.
	 */
	public int getPathReference( ) {
		return pathReference;
	}

	/**
	 * This is called when the method is going to be run.
	 * It is called to translate a raw value into what the value type the parameter expects.
	 * @param theObject to translate
	 * @return the translated object
	 */
	public Object translate( Object theObject ) {
		return valueTranslator.translate( theObject );
	}
}