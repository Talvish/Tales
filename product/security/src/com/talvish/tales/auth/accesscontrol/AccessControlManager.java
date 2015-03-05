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
package com.talvish.tales.auth.accesscontrol;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;

import com.talvish.tales.auth.capabilities.CapabilityDefinitionManager;
import com.talvish.tales.auth.capabilities.CapabilityFamilyDefinition;
import com.talvish.tales.auth.jwt.JsonWebToken;

/**
 * This class is used to verify that the caller of methods has the appropriate
 * auth token. It typically checks that the token is well formed and valid, and  
 * that any expected claims and capabilities are present in the auth token. 
 * @author jmolnar
 *
 * @param <R> The type of access result. Subclasses can override the AcessResult to provide additional data that the base class does not.
 */
public abstract class AccessControlManager<R extends AccessResult> {
	private CapabilityDefinitionManager capabilityDefinitionManager;
	private final Map<Class<? extends Annotation>, AnnotationHandler> annotationHandlers = new ConcurrentHashMap<>( 8, 0.75f, 1 );
	private final Map<Class<? extends Annotation>, AnnotationHandler> externalAnnotationHandlers = Collections.unmodifiableMap( annotationHandlers );

	private final Map<Class<?>, TypeAccessDescriptor> accessTypes = new ConcurrentHashMap<>( 4, 0.75f, 1 );

	/**
	 * The base constructor that takes the required capability definition manager.
	 * @param theDefinitionManager the capability definition manager.
	 */
	public AccessControlManager( CapabilityDefinitionManager theDefinitionManager ) {
		Preconditions.checkNotNull( theDefinitionManager, "need a capability definition manager" );

		capabilityDefinitionManager = theDefinitionManager;
		
		registerAnnotationHandler( ClaimsAbsent.class, ( method, descriptor, manager ) -> {
			ClaimsAbsent annotation = method.getAnnotation( ClaimsAbsent.class );
			if( annotation != null ) {
				descriptor.addVerifier( new ClaimsAbsentVerifier( annotation.claims( ) ) );
			}
		} );

		registerAnnotationHandler( ClaimsRequired.class, ( method, descriptor, manager ) -> {
			ClaimsRequired annotation = method.getAnnotation( ClaimsRequired.class );
			if( annotation != null ) {
				descriptor.addVerifier( new ClaimsRequiredVerifier( annotation.claims( ) ) );
			}
		} );

		registerAnnotationHandler( ClaimValueRange.class, ( method, descriptor, manager ) -> {
			ClaimValueRange[] annotations = method.getAnnotationsByType( ClaimValueRange.class );
			if( annotations != null ) {
				for( ClaimValueRange annotation : annotations) {
					descriptor.addVerifier( new ClaimValueRangeVerifier( 
							annotation.claim(), 
							annotation.minimum(), 
							annotation.maximum( ) ) );
				}
			}
		} );
		registerAnnotationHandler( CapabilitiesRequired.class, ( method, descriptor, manager ) -> {
			CapabilitiesRequired[] annotations = method.getAnnotationsByType( CapabilitiesRequired.class );
			for( CapabilitiesRequired annotation : annotations ) {
				if( annotation.capabilities().length == 1 ) {
					String capabilityName = annotation.capabilities()[ 0 ];
					CapabilityFamilyDefinition capabilityFamily = manager.getCapabilityDefinitionManager().getFamily( annotation.family( ) );
					Preconditions.checkArgument( capabilityFamily != null, "method '%s.%s' is trying to use annotation '%s' but refers to a family, '%s', that does not exist", method.getDeclaringClass().getSimpleName(), method.getName( ), annotation.getClass().getSimpleName( ), annotation.family( ) );
					Preconditions.checkArgument( capabilityFamily.isDefined( capabilityName ), "method '%s.%s' is trying to use annotation '%s' but refers to a capability, '%s.%s', that does not exist", method.getDeclaringClass().getSimpleName(), method.getName( ), annotation.getClass().getSimpleName( ), annotation.family( ), capabilityName );
							
					int capabilityIndex = capabilityFamily.getCapability( capabilityName ).getIndex( );
					
					descriptor.addVerifier( 
							new CapabilityRequiredVerifier( 
									annotation.family(), 
									capabilityName,
									capabilityIndex ) );
				} else if( annotation.capabilities().length > 1 ) {
					CapabilityFamilyDefinition capabilityFamily = manager.getCapabilityDefinitionManager().getFamily( annotation.family( ) );
					Preconditions.checkArgument( capabilityFamily != null, "method '%s.%s' is trying to use annotation '%s' but refers to a family, '%s', that does not exist", method.getDeclaringClass().getSimpleName(), method.getName( ), annotation.getClass().getSimpleName( ), annotation.family( ) );

					descriptor.addVerifier( 
							new CapabilitiesRequiredVerifier( capabilityFamily.generateInstance( annotation.capabilities( ) ) ) );
				}
			}
		} );
	}
	
	/**
	 * The capability definition manager used as the source for capability families and their
	 * capabilities. 
	 * @return the capability family
	 */
	public CapabilityDefinitionManager getCapabilityDefinitionManager( ) {
		return capabilityDefinitionManager;
	}

	/**
	 * An mechanism that allows developers to register their own <code>ClaimVerifier</code>s. When a method is analyzed to 
	 * look for potential claims or capabilities to verify, all registered annotation handlers are invoked so they can 
	 * for annotations they are interested. 
	 * @param theClass the type of annotation that can be placed on a method
	 * @param theHandler the function used to register to the claim verifiers on the method access descriptors
	 */
	public void registerAnnotationHandler( Class<? extends Annotation> theClass, AnnotationHandler theHandler ) {
		Preconditions.checkNotNull( theClass, "need the class for the annotation to register" );
		Preconditions.checkNotNull( theHandler, "need the handler for the annotation class '%s'", theClass.getSimpleName( ) );
		Preconditions.checkArgument( !annotationHandlers.containsKey( theClass ), "the annotation class '%s' has already been registered", theClass.getSimpleName( ) );
		
		annotationHandlers.put( theClass, theHandler );
	}
	
	/**
	 * Returns the annotation handlers registered with the system.
	 * @return the annotation handlers registered with the system
	 */
	public Map<Class<? extends Annotation>, AnnotationHandler> getAnnotationHandlers( ) {
		return externalAnnotationHandlers;
	}
	
	/**
	 * Generates a type descriptor that outlines the methods that are access controlled.
	 * This method only analyzes public methods.
	 * @param theClass the class to analyzed for access controlled methods
	 * @return the type, and within the type the methods, that are access controlled.
	 */
	public TypeAccessDescriptor generateTypeDescriptor( Class<?> theClass ) {
		Preconditions.checkNotNull( theClass, "cannot prepare a null class" );
		
		TypeAccessDescriptor accessType = accessTypes.get( theClass );
		
		if( accessType == null ) {
			// I could consider doing non-public methods in the future, process
			// the base classes first and move onto local
			Method[] methods = theClass.getMethods( );
			List<MethodAccessDescriptor> accessMethods = new ArrayList<>( methods.length );

			accessType = new TypeAccessDescriptor( theClass );

			// we loop through looking at each method to see if it was annotated
			// and if it was we then do a deeper dive to look at additional annotations
			for( Method method : methods ) {
				AccessControlled authAnnotation = method.getAnnotation( AccessControlled.class );
				if( authAnnotation != null ) {
					accessMethods.add( generateMethodDescriptor( authAnnotation.name(), method, accessType ) );
				}
			}
			accessType.setMethods( accessMethods );
			accessTypes.put( theClass, accessType );
		}
		return accessType;
	}
	
	/**
	 * Helper that looks at a given method, knowing it was marked for 
	 * access control. The method grabs the annotation handlers from the access
	 * control manager, and the annotation handlers will independently look at
	 * the method for specific annotations and if they exist, register the their
	 * verifiers.
	 * @param theName the name of the method, as identified by the annotation
	 * @param theMethod the method to analyze
	 * @param theTypeDescriptor the type this method is part of
	 * @return the resulting method access descriptor
	 */
	private MethodAccessDescriptor generateMethodDescriptor( String theName, Method theMethod, TypeAccessDescriptor theTypeDescriptor ) {
		MethodAccessDescriptor tokenVerifier = new MethodAccessDescriptor( theName, theMethod, theTypeDescriptor );

		for( Entry< Class<? extends Annotation>, AnnotationHandler> entry : getAnnotationHandlers().entrySet() ) {
			entry.getValue( ).analyze( theMethod, tokenVerifier, this );
		}

		return tokenVerifier;
	}
	
	/**
	 * The method that is called to verify that token has a valid shape and that the claims and
	 * capabilities required by the method are in the auth token. 
	 * @param theMethod the method that is being accessed
	 * @param theToken the token that has the claims and capabilities to check
	 * @return the <code>AccessResult</code> outlining the success or failure of the request to execute the specified method
	 */
	public abstract R verifyAccess( MethodAccessDescriptor theMethod, JsonWebToken theToken );
}
