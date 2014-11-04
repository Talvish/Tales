// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
package com.talvish.tales.system.status;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This is a manager for status blocks.
 * @author jmolnar
 *
 */
public class StatusManager {
	private final Object lock = new Object( );
	private Map<String, StatusBlock> statusBlocks = new TreeMap<String,StatusBlock>( );
	
	// TODO: eventually I would like alert mechanisms tied in so that when
	//       certain things happen that a result can occur, alike alert, 
	//       shutdown, etc.
	
	/**
	 * Default constructor.
	 */
	public StatusManager( ) {
	}

	/**
	 * This method is called to register a status block based on an object that has
	 * MonitorableStatusBlock annotation on it. It will combine the values to anything
	 * that has that same block name
	 * @param theName the name to give the status block created from the item registered 
	 * @param theItem the item being registered
	 */
	public void register( String theName, Object theItem ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "must have the name to give the status block" );
		Preconditions.checkNotNull( theItem, "must have an item to look for a status block" );

    	// register based on the name and the statue values generated
    	register( theName, generateStatusValues( theItem ) );
	}
	
	/**
	 * This method is called to register a manually created status block.
	 * It will combine this block with any existing block using the same name.
	 * @param theBlock the block to register
	 */
	public void register( StatusBlock theBlock ) {
		Preconditions.checkNotNull( theBlock, "need the block to register" );
		register( theBlock.getName(), theBlock.getStatusValues() );
	}

	/**
	 * Called to register the status values against the block name passed in.
	 * This will combine with anything that exists with that block name.
	 * @param theBlock the block to register
	 */
	public void register( String theBlockName, Collection<StatusValue> theValues) {
		synchronized( lock ) {
			// ... under the assumption there will be a lot more reads of the status blocks 
			//     than registrations, copies are made here to ensure multi-threaded safety
			Map<String,StatusBlock> newBlocks = new TreeMap<String, StatusBlock>( statusBlocks );
			// ... generate and add the block to the copy
           	StatusBlock newBlock = generateBlock( theBlockName, theValues );
           	newBlocks.remove( theBlockName );
           	newBlocks.put( newBlock.getName(),  newBlock );
           	// ... set the collection, making it unmodifiable so those using it don't assume they can modify
           	this.statusBlocks = Collections.unmodifiableMap( newBlocks );
		}
	}

	/**
	 * Helper that generates a StatusBlock based on the values passed and any values
	 * that exist in the system under the same name.
	 * @param theBlockName the block name to combine
	 * @param theValues the values to add to the existing values
	 * @return the generated status block
	 */
	private StatusBlock generateBlock( String theBlockName, Collection<StatusValue> theValues ) {
		// get the current block
		StatusBlock foundBlock = this.statusBlocks.get( theBlockName );
		if( foundBlock != null ) {
			Map<String, StatusValue> allValues = new TreeMap<String, StatusValue>( );//new TreeMap<String, StatusValue>( theValues.size() + foundBlock.getStatusValues().size() );

			// add the old values  
			for( StatusValue value : foundBlock.getStatusValues( ) ) {
				allValues.put( value.getName( ),value );
			}

			// add the new values, making sure they the names don't already exist
			for( StatusValue value : theValues ) {
				if( allValues.containsKey( value.getName() ) ) {
					throw new IllegalArgumentException( String.format( "Unable to add status value '%s' to block '%s' because it already exists.", value.getName( ), theBlockName ) );
				} else {
					allValues.put( value.getName( ),value );
				}
			}
			return new StatusBlock( theBlockName, allValues.values( ) );
		} else {
			return new StatusBlock( theBlockName, theValues );		
		}
	}

	/**
	 * This method is called to register manually created status blocks.
	 * It will combine them with any blocks that have the same name.
	 * @param theBlocks the blocks to register
	 */
	public void register( Collection<StatusBlock> theBlocks ) {
		Preconditions.checkNotNull( theBlocks, "need the blocks to register" );

		synchronized( lock ) {
			// ... under the assumption there will be a lot more reads of the status blocks 
			//     than registrations, copies are made here to ensure multi-threaded safety
			Map<String,StatusBlock> newBlocks = new TreeMap<String, StatusBlock>( statusBlocks );
			
			
			// for each block being added, generate a new block that will combine
			for( StatusBlock block : theBlocks ) {
				StatusBlock newBlock = generateBlock( block.getName( ), block.getStatusValues( ) );
				newBlocks.remove( newBlock.getName( ) );
				newBlocks.put( newBlock.getName( ),  newBlock );
			}
           	// ... set the collection, making it unmodifiable so those using it don't assume they can modify
           	this.statusBlocks = Collections.unmodifiableMap( newBlocks );
		}
	}

	/**
	 * Returns the status blocks managed by this manager.
	 * @return the status blocks
	 */
	public Collection<StatusBlock> getStatusBlocks( ) {
		return this.statusBlocks.values();
	}
	
	/**
	 * This is a helper method that given an object will grab all the StatusMethod annotated 
	 * methods to create a set of StatusValues that can be monitored. This method assumes
	 * that at least one will be found and will except if none are found
	 * @param theItem the item to look for StatusMethod annotations 
	 * @return the set of StatusValues found
	 */
	private static Collection<StatusValue> generateStatusValues( Object theItem ) {
		Preconditions.checkNotNull( theItem, "Need to have an item to get status values" );
		
    	ArrayList<StatusValue> statusValues = new ArrayList<StatusValue>( 1 );
    	Class<?> itemClass = theItem.getClass();
    	Method[] methods = itemClass.getMethods( );
    	MonitorableStatusValue statusValueAnnotation;
    	
    	for( Method method : methods ) {
    		statusValueAnnotation = method.getAnnotation( MonitorableStatusValue.class );
    		if( statusValueAnnotation != null ) {
    			if( Strings.isNullOrEmpty( statusValueAnnotation.name() ) ) {
            		throw new IllegalStateException( String.format( "The status value method '%1$s' on type '%2$s' does not have a name value.", method.getName(), itemClass.getName( ) ) );
    			}

    			statusValues.add( new StatusValue( statusValueAnnotation.name(), statusValueAnnotation.description(), theItem, method ) );
    		}
    	}
    	
    	if( statusValues.isEmpty() ) {
    		throw new IllegalStateException( String.format( "The object of type '%1$s' does not have any status methods defined.", itemClass.getName( ) ) );
    	}
    	
    	return statusValues;
	}

//	/**
//	 * Helper method that looks for a particular annotation.
//	 * @param <T> the type of annotation class
//	 * @param theItemClass the item class to find the annotation for
//	 * @return the annotation if found, null otherwise
//	 */
//	private static <T extends Annotation> T getAnnotation( Class<?> theItemClass, Class<T> theAnnotationClass ) {
//		T statusAnnotation;
//		
//		do {
//       	statusAnnotation = theItemClass.getAnnotation( theAnnotationClass );
//       	if( statusAnnotation != null ) {
//       		break;
//       	} else {
//       		theItemClass = theItemClass.getSuperclass();
//       	}
//       
//       } while( theItemClass != null );
//       
//       return statusAnnotation;
//	}
}
