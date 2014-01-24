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
package com.tales.system.status;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This is a manager for status blocks.
 * @author jmolnar
 *
 */
public class StatusManager {
	private final Object lock = new Object( );
	private Collection<StatusBlock> statusBlocks = new ArrayList<StatusBlock>( 0 );
	
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
	 * MonitorableStatusBlock annotation on it.
	 * @param theName the name to give the status block created from the item registered 
	 * @param theItem the item being registered
	 */
	public boolean register( String theName, Object theItem ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "must have the name to give the status block" );
		Preconditions.checkNotNull( theItem, "must have an item to look for a status block" );

		StatusBlock statusBlock = null;
    	Collection<StatusValue> statusValues = null;

    	// ... we look for status value annotations on items to know what to grab, and ...
    	statusValues = generateStatusValues( theItem );

        // ... we can assume we have values since the other method excepts if there are none 
       	statusBlock = new StatusBlock( theName, statusValues );
       	// ... we now register the blocks 
       	return register( statusBlock );
	}
	
	/**
	 * This method is called to register a manually created status block.
	 * @param theBlock the block to register
	 */
	public boolean register( StatusBlock theBlock ) {
		Preconditions.checkNotNull( theBlock, "need the block to register" );

		boolean registered = false;
		
		synchronized( lock ) {
			// ... under the assumption there will be a lot more reads of the status blocks 
			//     than registrations, copies are made here to ensure multi-threaded safety
			ArrayList<StatusBlock> newBlocks = new ArrayList<StatusBlock>( statusBlocks );
			// ... add the block to the copy
           	newBlocks.add( theBlock );
           	// ... set the collection, making it unmodifiable so those using it don't assume they can modify
           	this.statusBlocks = Collections.unmodifiableCollection( newBlocks );
           	registered = true;
		}
		return registered;
	}

	/**
	 * This method is called to register manually created status blocks.
	 * @param theBlocks the blocks to register
	 */
	public boolean register( Collection<StatusBlock> theBlocks ) {
		Preconditions.checkNotNull( theBlocks, "need the blocks to register" );

		boolean registered = false;
		
		synchronized( lock ) {
			// ... under the assumption there will be a lot more reads of the status blocks 
			//     than registrations, copies are made here to ensure multi-threaded safety
			ArrayList<StatusBlock> newBlocks = new ArrayList<StatusBlock>( statusBlocks );
			// ... add the blocks to the copy
           	newBlocks.addAll( theBlocks );
           	// ... set the collection, making it unmodifiable so those using it don't assume they can modify
           	this.statusBlocks = Collections.unmodifiableCollection( newBlocks );
           	registered = true;
		}
		return registered;
	}

	/**
	 * Returns the status blocks managed by this manager.
	 * @return the status blocks
	 */
	public Collection<StatusBlock> getStatusBlocks( ) {
		return this.statusBlocks;
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
