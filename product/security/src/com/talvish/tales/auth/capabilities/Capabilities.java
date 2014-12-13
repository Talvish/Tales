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

import java.util.BitSet;

import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;
import com.talvish.tales.contracts.data.OnDeserialized;


/**
 * This is an instance of the capabilities that can be used
 * for managing authorization. This will be used via 
 * auth tokens, checks within methods, etc.
 * @author jmolnar
 *
 */
@DataContract( name="com.talvish.tales.auth.capabilities" )
public class Capabilities {
	@DataMember( name="family" )
	private String family;
	@DataMember( name="bits")
	private BitSet capabilityBits;
	private volatile String capabilityString;

	/**
	 * Constructor that will initialize the capabilities based on a string. 
	 * @param theFamily the name of the group
	 * @param theCapabilityString the string representing the capabilities
	 */
	public Capabilities( String theFamily, String theCapabilityString ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need a name" );
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theCapabilityString ), "'%s' doesn't have any capabilities", theFamily );
		
		family = theFamily;
		capabilityString = theCapabilityString;
		// we could consider doing this lazy, if it doesn't get looked at much and only create
		// during the has or set calls
		capabilityBits = BitSet.valueOf( DatatypeConverter.parseHexBinary( capabilityString ) );
	}
	
	/**
	 * Constructor that will initialize the group based on the bitset. 
	 * @param theFamily the name of the group
	 * @param theCapabilityBits the bitset representing the capabilities
	 */
	public Capabilities( String theFamily, BitSet theCapabilityBits ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theFamily ), "need a name" );
		Preconditions.checkNotNull( theCapabilityBits, "'%s' doesn't have any capabilities", theFamily );
		
		family = theFamily;
		capabilityBits = ( BitSet )theCapabilityBits.clone( );
		capabilityString = DatatypeConverter.printHexBinary( capabilityBits.toByteArray( ) );
	}
	
	/**
	 * Helper method called when deserialization occurs. 
	 * The method helps ensure we have the data in memory that we want.
	 */
	@OnDeserialized( )
	private void onDeserialized( ) {
		capabilityString = DatatypeConverter.printHexBinary( capabilityBits.toByteArray( ) );
	}

	/**
	 * The group name for the capabilities.
	 * @return the name of the group
	 */
	public String getFamily( ) {
		return family;
	}
	
	/**
	 * Indicates whether this group has a particularly capability set.
	 * For indexes that don't have a definition, false will always be
	 * returned (unless, for some reason, someone manually turned it ).
	 * @param theIndex the index to check
	 * @return returns true of the capability is on, false otherwise
	 */
	public boolean hasCapability( int theIndex ) {
		Preconditions.checkArgument( theIndex >= 0 && theIndex < capabilityBits.size( ), "the index, %s, is not within the range 0 to %s", theIndex, capabilityBits.size( ) - 1 );
		// TODO: consider if we want to allow doing this via name
		//       this only makes sense when there is access to 
		//		 the definition, though it would be kind of nice
		return capabilityBits.get( theIndex );
	}
	
	/**
	 * Sets the capability for the given index. 
	 * @param theIndex the index of the capability to enable
	 * @param theValue what value to set the capability to
	 */
	public void setCapability( int theIndex, boolean theValue ) {
		Preconditions.checkArgument( theIndex >= 0 && theIndex < capabilityBits.size( ), "the index, %s, is not within the range 0 to %s", theIndex, capabilityBits.size( ) - 1 );
		// set the bit
		capabilityBits.set( theIndex,  theValue );
		// we store this since changes to capabilities should be rare
		capabilityString = DatatypeConverter.printHexBinary( capabilityBits.toByteArray( ) );
	}

	/**
	 * Returns the capabilities represented by this group, in string form. 
	 * @return the capabilities
	 */
	public String getCapabilityString( ) {
		// if we don't have it set yet (e.g. due to serialization)
		if( capabilityString == null ) {
			capabilityString = DatatypeConverter.printHexBinary( capabilityBits.toByteArray( ) );
		}
		return capabilityString;
	}
	
	/**
	 * Returns the capabilities represented by this family, in bitset form.
	 * This call makes a copy during this call, so that modifications made
	 * to the returning set will not impact what is stored on the family.
	 * @return the capabilities
	 */
	public BitSet getCapabilityBits( ) {
		return ( BitSet )capabilityBits.clone( );
	}
}
