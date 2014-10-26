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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.contracts.Contract;

/**
 * This class represents an object that is mapped as an external HTTP resource. 
 * @author jmolnar
 *
 */
public class ResourceType extends Contract {
	// TODO: reconcile the HttpResourceContract and this . . .
	private List<ResourceMethod> methods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() );
	private List<ResourceMethod> getMethods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() );
	private List<ResourceMethod> postMethods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() );
	private List<ResourceMethod> putMethods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() ) ;
	private List<ResourceMethod> deleteMethods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() );
	private List<ResourceMethod> headMethods = Collections.unmodifiableList( new ArrayList<ResourceMethod>() );
	
	private final Class<?> boundClass;
	private final String boundPath;
	private final ResourceOperation.Mode mode;
	
	private static final String ROOT_PATH_REGEX = String.format( "/(?:%1$s/)*((?:%1$s)/?)?", ResourceMethod.SEGMENT_COMPONENT_REGEX );
	private static final Pattern ROOT_PATH_PATTERN = Pattern.compile( ROOT_PATH_REGEX );
	
	/**
	 * Constructor for the resource type. 
	 * @param theName the name of the contract
	 * @param theDescription the description of the contract
	 * @param theVersions the versions of the contract
	 * @param theBoundPath the path the resource is bound to
	 * @param theClass the class that backs this resource
	 */
	ResourceType( String theName, String theDescription, String[] theVersions, String theBoundPath, Class<?> theClass, ResourceOperation.Mode theMode ) {
		super( theName, theDescription, theVersions );
		Preconditions.checkNotNull( theClass, "need the bound class" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBoundPath ), String.format( "need a root path for the resource '%s'", theClass.getName( ) ) );
		Matcher pathMatcher = ROOT_PATH_PATTERN.matcher( theBoundPath );
		Preconditions.checkArgument( pathMatcher.matches( ), String.format( "the path string '%s' for type '%s' does not conform to the pattern '/URL_PATH'", theBoundPath, theClass.getName( ) ) );
		Preconditions.checkNotNull( theMode, String.format( "need an executionn mode for the resource '%s'", theClass.getName( ) ) );
		Preconditions.checkArgument( theMode != ResourceOperation.Mode.DEFAULT, String.format( "the execution cannot be set to DEFAULT for resource '%s'", theClass.getName( ) ) );

		boundClass = theClass;
		boundPath = theBoundPath;
		mode = theMode;
	}
	
	/**
	 * The root path the resource is bound to.
	 * @return the root path the resource is bound to 
	 */
	public String getBoundPath( ) {
		return this.boundPath;
	}
	
	/**
	 * The underlying class being exposed.
	 * @return the underlying class.
	 */
	public Class<?> getType( ) {
		return boundClass;
	}
	
	/**
	 * The underlying methods expose by the type.
	 * @return the collection of methods exposed
	 */
	public Collection<ResourceMethod> getMethods( ) {
		return methods;
	}
	
	/**
	 * The default execution mode for resource methods
	 * that are exposed as part of this resource.
	 * Individual methods may override this behaviour.
	 * @return the default execution mode for resource methods
	 */
	public ResourceOperation.Mode getMode( ) {
		return mode;
	}
	
	/**
	 * A helper method that is called by the underlying system that creates
	 * the resource types.
	 * @param theMethods the method exposed by this type
	 */
	void setMethods( Collection<ResourceMethod> theMethods ) {
		Preconditions.checkNotNull( theMethods, "need the methods" );

		Set<String> newMethodNames = new HashSet<String>( theMethods.size( ) ); // to help with duplicates
		ArrayList<ResourceMethod> newMethods = new ArrayList<ResourceMethod>( theMethods );

		ArrayList<ResourceMethod> newGetMethods = new ArrayList<ResourceMethod>( 0 ); 
		ArrayList<ResourceMethod> newPostMethods = new ArrayList<ResourceMethod>( 0 ); 
		ArrayList<ResourceMethod> newPutMethods = new ArrayList<ResourceMethod>( 0 ); 
		ArrayList<ResourceMethod> newDeleteMethods = new ArrayList<ResourceMethod>( 0 ); 
		ArrayList<ResourceMethod> newHeadMethods = new ArrayList<ResourceMethod>( 0 ); 

		// for speed reasons during execution, we separate the different method to the http verbs
		for( ResourceMethod method : newMethods ) {
			if( newMethodNames.contains( method.getName( ) ) ) {
				throw new IllegalStateException( String.format( "Resource '%s' already contains a method with name '%s'.", this.getName(), method.getName() ) );
			} else {
				for( String verb : method.getVerbs( ) ) {
					if( verb.equals( "GET" ) ) {
						insertPath( method, newGetMethods );
					} else if( verb.equals( "POST" ) ) {
						insertPath( method, newPostMethods );
					} else if( verb.equals( "PUT" ) ) {
						insertPath( method, newPutMethods );
					} else if( verb.equals( "DELETE" ) ) {
						insertPath( method, newDeleteMethods );
					} else if( verb.equals( "HEAD" ) ) {
						insertPath( method, newHeadMethods );
					} else {
						throw new IllegalStateException( String.format( "Method '%s.%s' references unrecognized HTTP verb '%s'", boundClass.getName( ), method.getName( ), verb ) );
					}
				}
				newMethodNames.add( method.getName() );
			}
		}
		
		this.methods = Collections.unmodifiableList( newMethods );
		this.getMethods = Collections.unmodifiableList( newGetMethods );
		this.postMethods = Collections.unmodifiableList( newPostMethods );
		this.putMethods = Collections.unmodifiableList( newPutMethods );
		this.deleteMethods = Collections.unmodifiableList( newDeleteMethods );
		this.headMethods = Collections.unmodifiableList( newHeadMethods );
	}
	
	/**
	 * Helper method for getting the path inserted into the requested collection.
	 * @param theMethod the method to add
	 * @param theExistingMethods the collection to add to
	 */
	private void insertPath( ResourceMethod theMethod, ArrayList<ResourceMethod> theExistingMethods ) {
		boolean inserted = false;
		for( int index = 0; index < theExistingMethods.size(); index += 1 ) {
			ResourceMethod existingMethod = theExistingMethods.get( index );
			if( existingMethod.matchesPath( theMethod.getOrderingPath( ) ) && !theMethod.matchesPath( existingMethod.getOrderingPath( ) ) ) {
				// if there is a match then the method we are trying to add is 
				// more specific and should therefore be inserted first
				theExistingMethods.add( index, theMethod );
				inserted = true;
				break;
			}
		}
		if( !inserted ) {
			// of course add if it wasn't added alread
			theExistingMethods.add( theMethod );
		}
	}

	/**
	 * Helper method for the servlet so it can get the appropriate set of
	 * methods for the particular http verb.
	 * @return the methods for the http verb
	 */
	public List<ResourceMethod> getGetMethods( ) {
		return getMethods;
	}

	/**
	 * Helper method for the servlet so it can get the appropriate set of
	 * methods for the particular http verb.
	 * @return the methods for the http verb
	 */
	public List<ResourceMethod> getPostMethods( ) {
		return postMethods;
	}

	/**
	 * Helper method for the servlet so it can get the appropriate set of
	 * methods for the particular http verb.
	 * @return the methods for the http verb
	 */
	public List<ResourceMethod> getPutMethods( ) {
		return putMethods;
	}

	/**
	 * Helper method for the servlet so it can get the appropriate set of
	 * methods for the particular http verb.
	 * @return the methods for the http verb
	 */
	public List<ResourceMethod> getDeleteMethods( ) {
		return deleteMethods;
	}

	/**
	 * Helper method for the servlet so it can get the appropriate set of
	 * methods for the particular http verb.
	 * @return the methods for the http verb
	 */
	public List<ResourceMethod> getHeadMethods( ) {
		return headMethods;
	}
}
