// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
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
package com.talvish.tales.auth.capabilities;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.parts.naming.LowerCaseValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.serialization.OnDeserialized;

/**
 * This is represents a set of capabilities that a user, app may have.
 * The name of the group is expected to be unique amongst all groups.
 * Groups are not expected to have dynamically added/removed capabilities 
 * since modifications can have big implications in a system so there is
 * no mechanism to add or remove.
 * @author jmolnar
 *
 */
@DataContract( name="com.talvish.tales.auth.capability_family_definition" )
public class CapabilityFamilyDefinition {
	/**
	 * An enum representing different logical operators.
	 * This was created for combining capabilities 
	 * together in the the {@link CapabilityFamilyDefinition.generateInstance}
	 * method.
	 * @author jmolnar
	 *
	 */
	public static enum BitwiseOperator {
		AND,
		OR,
		XOR,
		ANDNOT,
	}
	
	public static final String NAME_VALIDATOR = "tales.auth.capability_family_name";
	
	static {
		if( !NameManager.hasValidator( NAME_VALIDATOR ) ) {
			NameManager.setValidator( NAME_VALIDATOR, new LowerCaseValidator( ) );
		}
	}
	
	@DataMember( name="name" )
	private String name;
	@DataMember( name="capabilities" )
	private List<CapabilityDefinition> capabilityList;
	private Map<String,CapabilityDefinition> capabilityMap;
	
	/**
	 * Constructor for deserialization purposes.
	 */
	protected CapabilityFamilyDefinition( ) {
		
	}
	
	/**
	 * The constructor for the group representing the definition.
	 * @param theName
	 * @param theDescription
	 * @param theCapabilities
	 */
	public CapabilityFamilyDefinition( String theName, Collection<CapabilityDefinition> theCapabilities ) {
		NameValidator nameValidator = NameManager.getValidator( NAME_VALIDATOR );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theCapabilities, "'%s' needs capabilities", theName );
		Preconditions.checkArgument( theCapabilities.size() > 0, "'%s' needs capabilities", theName );
		
		name = theName;
		
		int largestIndex = 0;
		
		// first we need to find the largest index, because we will be allocating against it
		// and we don't assume that what is given is in order and that indexes given are 
		// within the range of the number of indexes
		for( CapabilityDefinition capability : theCapabilities ) {
			if( capability.getIndex( ) > largestIndex ) {
				largestIndex = capability.getIndex( );
			}
		}
		// need to allocate the map we are going to use
		capabilityMap = new HashMap<>( largestIndex + 1 );
		// now we allocate based on the array, which will have null values between meaning unset capabilities
		CapabilityDefinition[] array = new CapabilityDefinition[ largestIndex + 1 ];
		// and now we put the capabilities into the array that is ultimately used
		for( CapabilityDefinition capabilityDefinition : theCapabilities ) {
			Preconditions.checkArgument( capabilityDefinition.getFamily().equals( name ), "Capability '%s.%s' uses a different family name than the familiy name '%s'.", capabilityDefinition.getFamily(), capabilityDefinition.getName(), name );
			Preconditions.checkArgument( array[ capabilityDefinition.getIndex( ) ] != null, "Capability '%s.%s' is set to use index %s, which is already set with '%s'.", capabilityDefinition.getFamily(), capabilityDefinition.getName(), capabilityDefinition.getIndex(), array[ capabilityDefinition.getIndex( ) ].getName( ) );
			Preconditions.checkArgument( !capabilityMap.containsKey( capabilityDefinition.getName( ) ), "Capability '%s.%s' has already been defined.", capabilityDefinition.getFamily(), capabilityDefinition.getName() );
			
			array[ capabilityDefinition.getIndex( ) ] = capabilityDefinition;
			capabilityMap.put( capabilityDefinition.getName( ), capabilityDefinition );
		}
		capabilityList = Collections.unmodifiableList( Arrays.asList( array ) );
	}
	
	/**
	 * Helper method called when deserialization occurs. 
	 * The method helps ensure we have the data in memory that we want.
	 */
	@OnDeserialized( )
	private void onDeserialized( ) {
		// need to allocate the map we are going to use
		capabilityMap = new HashMap<>( capabilityList.size( ) );
		// we make the assumption that the list is perfectly fine/valid
		for( CapabilityDefinition capabilityDefinition : capabilityList ) {
			Preconditions.checkArgument( !capabilityMap.containsKey( capabilityDefinition.getName( ) ), "Capability '%s.%s' has already been defined.", capabilityDefinition.getFamily(), capabilityDefinition.getName() );
			capabilityMap.put( capabilityDefinition.getName( ), capabilityDefinition );
		}
	}
	
	/**
	 * Returns the name of the family.
	 * @return the name of the family.
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The complete list of capabilities in order of their indexes.
	 * It is possible that not all index have a capability defined
	 * and you will there for see a null definition that that index.
	 * @return the list of capabilities
	 */
	public List<CapabilityDefinition> getCapabilities( ) {
		return capabilityList;
	}
	
	/**
	 * Indicates if the index has a capability definition. As a note
	 * this means that the index is within range AND that a definition 
	 * is not null. 
	 * @param theIndex the index to check
	 * @return returns true if a definition exists, false otherwise
	 */
	public boolean isDefined( int theIndex ) {
		Preconditions.checkArgument( theIndex >= 0, "need a index greater than or equal to 0" );
		return theIndex >= 0 && theIndex <= capabilityList.size() - 1 && this.capabilityList.get( theIndex ) != null;
	}
	
	/**
	 * Indicates if the name has a capability definition.
	 * @param theName the name to check
	 * @return returns true if a definition exists, false otherwise
	 */
	public boolean isDefined( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name to check" );;
		return capabilityMap.containsKey( theName );
	}

	/**
	 * Returns a particular capability definition based on the index.
	 * If the index is not within the range of the defined 
	 * capabilities than an IllegalArgumentException is 
	 * thrown. This also differentiates with a null return
	 * which may happen if that particular index is within
	 * range but a definition has not yet been defined.
	 * @param theIndex the index of the capability to retrieve
	 * @return the capability definition or null if the definition is not defined for that index
	 */
	public CapabilityDefinition getCapability( int theIndex ) {
		Preconditions.checkArgument( theIndex >= 0 && theIndex <= capabilityList.size() - 1, "failed getting from '%s' because the index %s is not within the range 0 - %s", this.name, theIndex, capabilityList.size( ) - 1 );
		return this.capabilityList.get( theIndex );
	}

	/**
	 * Returns a particular capability definition based on the name.
	 * If the name is not found then an IllegalArgumentException 
	 * is thrown. This is to remain consistent with the other 
	 * getCapability method though it is not possible for this
	 * method to return null.
	 * @param theName the name of the capability to retrieve 
	 * @return the definition for the given given, null will not be returned
	 */
	public CapabilityDefinition getCapability( String theName ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "failed getting from '%s' because a name wan't provided", this.name );
		Preconditions.checkArgument( capabilityMap.containsKey( theName ), "failed getting from '%s' because the name '%s' doesn't exist", this.name, theName );
		return capabilityMap.get( theName );
	}

	/**
	 * Generates an empty set of capabilities that are sized to the number of
	 * capabilities defined for the family.
	 * @return an empty set of capabilities
	 */
	public Capabilities generateInstance( ) {
		return new Capabilities( getName( ), new BitSet( capabilityList.size( ) ) );
	}

	/**
	 * This generates a set of capabilities by combining capabilities from other instances.
	 * This is the same call generateInstance where the logical operator is an AND, meaning
	 * it will be restrictive in the final capabilities (meaning the capability has to exist
	 * in all sets).
	 * @param theSets set of capabilities to combine
	 * @return the resulting capability set
	 */
	public Capabilities generateInstance( Capabilities ... theSets ) {
		return generateInstance( BitwiseOperator.AND, theSets );
	}

	/**
	 * This generates a set of capabilities by combining capabilities from other instances.
	 * This allows taking capabilities assigned to principals, like users and apps, and combining
	 * them together to get the final capabilities to be used. The operator specified has a big 
	 * impact and should represent your security view. If you are looking to be restrictive by 
	 * default then use AND (capability has to be set in all sets to be in the final set) and if 
	 * you are looking to be permissive then you can used OR (as long as the capability is in one set
	 * it will be in the final set). The ANDNOT and XOR operations are there in case there are other
	 * options you are looking at.
	 * @param theOperator the way that the capability set should be combined 
	 * @param theSets set of capabilities to combine
	 * @return the resulting capability set
	 */
	public Capabilities generateInstance( BitwiseOperator theOperator, Capabilities ... theSets ) {
		Preconditions.checkArgument( theSets == null || theSets.length == 0, "need at least one set" );

		// we need to verify that each of the the assignments are on the same family
		BitSet capabilities = new BitSet( capabilityList.size( ) ); // we make sure it is the right size
		
		for( Capabilities family : theSets ) {
			Preconditions.checkArgument( family.getFamily().equals( this.getName( ) ), "cannot generate a capabilities for '%s' when one of the families says it is '%s'", this.getName( ), family.getFamily( ) );
			switch( theOperator ) {
			case AND:
				capabilities.and( family.getCapabilityBits( ) );
				break;
			case ANDNOT:
				capabilities.andNot( family.getCapabilityBits( ) );
				break;
			case OR:
				capabilities.or( family.getCapabilityBits( ) );
				break;
			case XOR:
				capabilities.xor( family.getCapabilityBits( ) );
				break;
			}
		}
		
		return new Capabilities( getName(), capabilities );
	}
}