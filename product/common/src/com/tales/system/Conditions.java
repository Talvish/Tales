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
package com.tales.system;

/**
 * A simple helper method class in a similar vein to Google's Preconditions class but
 * dealing with exceptions used for the services. 
 * @author Joseph Molnar
 *
 */
public class Conditions {
	/**
	 * Checks the condition and if false throws an InvalidParameterException.
	 */
	public static void checkParameter( boolean theCondition, String theName ) {
		checkParameter( theCondition, theName, "failed parameter check" );
	}
	
	/**
	 * Checks the condition and if false throws an InvalidParameterException.
	 */
	public static void checkParameter( boolean theCondition, String theName, String theMessage, Object... theMessageParameters ) {
		if( !theCondition ) {
			throw new InvalidParameterException( theName, String.format( theMessage, theMessageParameters ) );
		} 
	}
	
	/**
	 * Checks the condition and if false throws an InvalidStateException.
	 */
	public static void checkState( boolean theCondition ) {
		checkState( theCondition, "failed state check" );
	}
	
	/**
	 * Checks the condition and if false throws an InvalidStateException.
	 */
	public static void checkState( boolean theCondition, String theMessage, Object... theMessageParameters ) {
		if( !theCondition ) {
			throw new InvalidStateException( String.format( theMessage, theMessageParameters ) );
		} 
	}
	
	/**
	 * Checks the condition and if false throws a NotFoundException.
	 */
	public static void checkFound( boolean theCondition ) {
		checkFound( theCondition, "failed found check" );
	}
	
	/**
	 * Checks the condition and if false throws a NotFoundException.
	 */
	public static void checkFound( boolean theCondition, String theMessage, Object... theMessageParameters ) {
		if( !theCondition ) {
			throw new NotFoundException( String.format( theMessage, theMessageParameters ) );
		} 
	}
}
