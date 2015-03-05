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

import com.google.common.base.Preconditions;

/**
 * The result of an access check by an <code>AccessControlManager</code>
 * @author jmolnar
 *
 */
public class AccessResult {
	private AccessStatus status = AccessStatus.UNKNOWN;
	private String message;
	
	/**
	 * Default constructor.
	 */
	public AccessResult( ) {
	}
	
	/**
	 * The direct status of the 
	 * @return
	 */
	public AccessStatus getStatus( ) {
		return status;
	}
	
	/**
	 * Any message that was included when setting the status.
	 * This is useful for debugging purposes and can be 
	 * returned to the caller of the method.
	 * @return the message, if any
	 */
	public String getMessage( ) {
		return message;
	}

	/**
	 * Sets the result of the access check. This sets
	 * to the status and the message. The message is 
	 * set to null
	 * @param theStatus the status of the access check
	 */
	public void setResult( AccessStatus theStatus ) {
		Preconditions.checkNotNull( "need a status" );
		status = theStatus;
		message = null;
	}
	
	/**
	 * Sets the result of the access check. This sets
	 * to the status and the message.  The message
	 * is created by using <code>String.format( theMessage, theParameters)</code>.
	 * @param theStatus the status of the access check
	 * @param theMessage a message to include
	 * @param theParameters any optional parameters to be placed into the message
	 */
	public void setResult( AccessStatus theStatus, String theMessage, Object ... theParameters ) {
		Preconditions.checkNotNull( "need a status" );
		status = theStatus;
		if( theMessage != null ) {
			message = String.format( theMessage, theParameters );
		}
	}
}