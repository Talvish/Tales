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
package com.tales.client.http;

import java.util.UUID;

import com.tales.contracts.data.DataContract;
import com.tales.contracts.data.DataMember;

/**
 * The operation details of a request made to a Tales-enabled service.
 * This typically gives information like a request id, for tracking and
 * timing information.
 * @author jmolnar
 *
 */
@DataContract( name="com.tales.response.operation")
public class ResponseOperation {
	@DataMember( name="request_id" ) private UUID requestId;
	@DataMember( name="root_request_id" ) private UUID rootRequestId;
	@DataMember( name="parent_request_id" ) private UUID parentResultId;
	@DataMember( name="host_address" ) private String hostAddress;
	@DataMember( name="elapsed_time" ) private double elapsedTime;
	
	/**
	 * Constructor used by reflection.
	 */
	protected ResponseOperation( ) {
	}
	
	/**
	 * The id given to the request that this operation block came form.
	 * @return the id given to this request
	 */
	public UUID getRequestId( ) {
		return this.requestId;
	}
	
	/**
	 * The id given to the request that ultimately originated the request to this service.
	 * This helps with overall request tracing. 
	 * This may not always be returned.
	 * @return the root request that ultimately originated to the current request
	 */
	public UUID getRootRequestId( ) {
		return this.rootRequestId;
	}
	
	/**
	 * The id given to the request that ultimately originated the request to this service.
	 * This helps with overall request tracing.
	 * This may not always be returned.
	 * @return the root request that ultimately originated to the current request
	 */
	public UUID getParentRequestId( ) {
		return this.parentResultId;
	}
	
	/**
	 * The host that executed the request. 
	 * This may not always be returned.
	 * @return the host that executed the request.
	 */
	public String getHostAddress( ) {
		return this.hostAddress;
	}
	
	/**
	 * Indicates how long the request took to execute on the server, measured in milliseonds.
	 * @return how long it took the call to run on the server
	 */
	public double getElapsedTime( ) {
		return this.elapsedTime;
	}
}
