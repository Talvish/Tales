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
package com.talvish.tales.services;

import java.util.UUID;

import com.google.common.base.Strings;
import com.talvish.tales.serialization.Readability;

/**
 * This class represents the operational context about the
 * current running request.
 * @author jmolnar
 *
 */
public class OperationContext {
	/**
	 * Amount of details for the responses.
	 * @author jmolnar
	 *
	 */
	public enum Details {
		NORMAL,
		ALL,
	}
	private final String rootRequestId;
	private final String parentRequestId;
	private final String currentRequestId;
	private final long startTimestamp;
	private final Readability responseTarget;
	private final Details responseDetails;
	// TODO: consider adding the Contract and ContractVersion to here
//	TODO: private final String infoLevel; // this changes both what is in the log files and what is responded with over the wire

	/**
	 * The constructor taking most data we need for the context. It generates a request id for this current call.
	 * @param theRootRequestId the id of the first/root request to our services
	 * @param theCallingRequestId the id of the calling request
	 * @param theReadability whether the response targets machine or human readability, this is optional and defaults to machine
	 */
	public OperationContext(
			String theRootRequestId,
			String theCallingRequestId,
			Readability theReadability,
			Details theDetails ) {

		this.parentRequestId = theCallingRequestId;
		this.currentRequestId = UUID.randomUUID().toString();
		this.startTimestamp = System.nanoTime();
		
		if( Strings.isNullOrEmpty( theRootRequestId ) ) {
			this.rootRequestId = this.currentRequestId; // we presume if the root request isn't set then this is the root
		} else {
			this.rootRequestId = theRootRequestId;
		}
		
		responseTarget = theReadability == null ? Readability.MACHINE : theReadability;
		responseDetails = theDetails == null ? Details.NORMAL : theDetails;
	}
	
	/**
	 * The ID of the original request that hit any of our services.
	 * @return the original, first, request id
	 */
	public String getRootRequestId( ) {
		return this.rootRequestId;
	}
	
	/**
	 * The request ID of the service that make the current request to our service.
	 * This may be null if this is the root/first call.
	 * @return the calling request id
	 */
	public String getParentRequestId( ) {
		return this.parentRequestId;
	}
	
	/**
	 * The request ID for the current call. 
	 * @return the current request id
	 */
	public String getCurrentRequestId( ) {
		return this.currentRequestId;
	}
	
	/**
	 * This returns the amount of time since the operation was started.
	 * @return time since operation started
	 */
	public long calculateElapsedTime( ) {
		return System.nanoTime() - this.startTimestamp;
	}
	
	/**
	 * Returns what who the request is targeting for reading the response, human or machine.
	 * @return the target for the response
	 */
	public Readability getResponseTarget( ) {
		return this.responseTarget;
	}
	
	/**
	 * Returns the amount of detail to return in a response.
	 * @return the amount of detail in a response
	 */
	public Details getResponseDetails( ) {
		return this.responseDetails;
	}
}
