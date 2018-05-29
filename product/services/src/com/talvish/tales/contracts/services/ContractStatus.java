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
package com.talvish.tales.contracts.services;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.talvish.tales.system.status.AverageLong;
import com.talvish.tales.system.status.MonitorableStatusValue;
import com.talvish.tales.system.status.RatedLong;

/**
 * Contains status information regarding the associated contract.
 * @author jmolnar
 *
 */
public class ContractStatus {
	private AtomicLong clientErrors				= new AtomicLong( 0 );
	private RatedLong clientErrorRate			= new RatedLong( );
	private volatile DateTime lastClientError	= null;
	
	private AtomicLong localErrors 				= new AtomicLong( 0 );
	private RatedLong localErrorRate			= new RatedLong( );
	private volatile DateTime lastLocalError	= null;

	private AtomicLong unavailableErrors 		= new AtomicLong( 0 );
	private RatedLong unavailableErrorRate		= new RatedLong( );
	private volatile DateTime lastUnavailableError	= null;

	private AtomicLong dependentErrors 			= new AtomicLong( 0 );
	private RatedLong dependentErrorRate		= new RatedLong( );
	private volatile DateTime lastDependentError	= null;

	private AtomicLong successes 				= new AtomicLong( 0 );		
	private RatedLong successRate 				= new RatedLong( );
	private volatile DateTime lastSuccess		= null;

	private AtomicLong requests					= new AtomicLong( 0 );
	private RatedLong requestRate				= new RatedLong( );
	private volatile DateTime lastRequest		= null;
	
	private AverageLong executionTime			= new AverageLong( );
	
	/**
	 * Records that a request was sent to the contract.
	 */
	public void recordReceivedRequest( ) {
		requests.incrementAndGet();
		requestRate.increment();
		lastRequest = new DateTime( DateTimeZone.UTC );
	}
	
	/**
	 * Records the response time of a call.
	 * @param theExecutionTime the response time of a call.
	 */
	public void recordExecutionTime( long theExecutionTime ) {
		executionTime.add( theExecutionTime );
	}
	
	/**
	 * Indicates a successful call occurred.
	 */
	public void recordSuccess( ) {
		successes.incrementAndGet();
		successRate.increment();
		lastSuccess = new DateTime( DateTimeZone.UTC );
	}
	/**
	 * Indicates an unsuccessful call occurred 
	 * due to the client sending bad data.
	 */
	public void recordClientError( ) {
		clientErrors.incrementAndGet();
		clientErrorRate.increment();
		lastClientError = new DateTime( DateTimeZone.UTC );
	}
	
	/**
	 * Indicates an unsuccessful call occurred 
	 * due to some form of local problem.
	 */
	public void recordLocalError( ) {
		localErrors.incrementAndGet();
		localErrorRate.increment();
		lastLocalError = new DateTime( DateTimeZone.UTC );
	}

	/**
	 * Indicates the server indicated it was not available. 
	 */
	public void recordUnavailableError( ) {
		unavailableErrors.incrementAndGet();
		unavailableErrorRate.increment();
		lastUnavailableError = new DateTime( DateTimeZone.UTC );
	}

	/**
	 * Indicates an unsuccessful call occurred
	 * due to a dependent service failing in some way.
	 */
	public void recordDependentError( ) {
		dependentErrors.incrementAndGet();
		dependentErrorRate.increment();
		lastDependentError = new DateTime( DateTimeZone.UTC );
	}

	/**
	 * Returns the number of requests received by the contract.
	 * @return the number of received requests.
	 */
	@MonitorableStatusValue( name = "requests", description = "Total number of requests since the service was started." )
	public long getRequest( ) {
		return requests.get();
	}

	/**
	 * Returns the rate of the received requests.
	 * @return the rate of the received requests.
	 */
	@MonitorableStatusValue( name = "request_rate", description = "Rate of the number of requests per second as measured over a 10 second interval." )
	public double getRequestRate( ) {
		return requestRate.calculateRate();
	}

	/**
	 * Returns the last time a request occurred.
	 * @return the last request time
	 */
	@MonitorableStatusValue( name = "last_request_datetime", description = "The last date and time a request occurred." )
	public DateTime getLastRequest( ) {
		return lastRequest;
	}

	/**
	 * Returns the average execution time in milliseconds, as record in nanoseconds.
	 * @return the average execution time
	 */
	@MonitorableStatusValue( name = "average_execution_time", description = "Average execution time, in milliseconds, as meaured in nanoseconds.")
	public double getAvergeExecutionTime( ) {
		return executionTime.calculateAverage() / 1000000d;
	}
	
	
	/**
	 * Returns the number of successful calls.
	 * @return the number of successful calls
	 */
	@MonitorableStatusValue( name = "successes", description = "Total number of successful requests since the service was started." )
	public long getSuccesses( ) {
		return successes.get();
	}

	/**
	 * Returns the rate of the number of successful calls.
	 * @return the rate of the number of successful calls
	 */
	@MonitorableStatusValue( name = "success_rate", description = "Rate of the number of successful requests per second as measured over a 10 second interval." )
	public double getSuccessesRate( ) {
		return successRate.calculateRate();
	}
	
	/**
	 * Returns the last time a successful execution occurred.
	 * @return the last successful execution time
	 */
	@MonitorableStatusValue( name = "last_success_datetime", description = "The last date and time a successful execution occurred." )
	public DateTime getLastSuccess( ) {
		return lastSuccess;
	}

	/**
	 * Returns the number of client errors received
	 * since the contract was operational.
	 * @return the number of client errors
	 */
	@MonitorableStatusValue( name = "client_errors", description = "Total number of requests that failed since the service was started due to problems with the client data/communication." )
	public long getClientErrors( ) {
		return clientErrors.get();
	}

	/**
	 * Returns the rate of the number of client errors.
	 * @return the rate of the number of client errors.
	 */
	@MonitorableStatusValue( name = "client_error_rate", description = "Rate of the number of client-related failures per second as measured over a 10 second interval." )
	public double getClientErrorRate( ) {
		return clientErrorRate.calculateRate();
	}

	/**
	 * Returns the last time a client error occurred.
	 * @return the last client error time
	 */
	@MonitorableStatusValue( name = "last_client_error_datetime", description = "The last date and time a client error occurred." )
	public DateTime getLastClientError( ) {
		return lastClientError;
	}

	/**
	 * Returns the number of local errors received
	 * since the contract was operational.
	 * @return the number of local errors
	 */
	@MonitorableStatusValue( name = "local_errors", description = "Total number of requests that failed since the service was started due to problems within the service itself." )
	public long getLocalErrors( ) {
		return localErrors.get( );
	}

	/**
	 * Returns the rate of the number of local errors.
	 * @return the rate of the number of local errors.
	 */
	@MonitorableStatusValue( name = "local_error_rate", description = "Rate of the number of local-related failures per second as measured over a 10 second interval." )
	public double getLocalErrorRate( ) {
		return localErrorRate.calculateRate();
	}
	
	/**
	 * Returns the last time a local error occurred.
	 * @return the last local error time
	 */
	@MonitorableStatusValue( name = "last_local_error_datetime", description = "The last date and time a local error occurred." )
	public DateTime getLastLocalError( ) {
		return lastLocalError;
	}


	/**
	 * Returns the number of unavailable errors received
	 * since the contract was operational.
	 * @return the number of unavailable
	 */
	@MonitorableStatusValue( name = "unavailable_errors", description = "Total number of requests that failed since the service was unable to execute the request." )
	public long getUnavailableErrors( ) {
		return unavailableErrors.get( );
	}

	/**
	 * Returns the rate of the number of unavailable errors.
	 * @return the rate of the number of unavailable errors.
	 */
	@MonitorableStatusValue( name = "unavailable_error_rate", description = "Rate of the number of unavailable-related failures per second as measured over a 10 second interval." )
	public double getUnavailableErrorRate( ) {
		return unavailableErrorRate.calculateRate();
	}
	
	/**
	 * Returns the last time an unavailable error occurred.
	 * @return the last available error time
	 */
	@MonitorableStatusValue( name = "last_unavailable_error_datetime", description = "The last date and time an unavailable error occurred." )
	public DateTime getLastUnavailableError( ) {
		return lastUnavailableError;
	}

	/**
	 * Returns the number of dependent service errors received
	 * since the contract was operational.
	 * @return the number of dependent service errors
	 */
	@MonitorableStatusValue( name = "dependent_errors", description = "Total number of requests that failed since the service was started due to problems with a dependent service/process." )
	public long getDependentErrors( ) {
		return dependentErrors.get( );
	}		
	
	/**
	 * Returns the rate of the number of dependent errors.
	 * @return the rate of the number of dependent errors.
	 */
	@MonitorableStatusValue( name = "dependent_error_rate", description = "Rate of the number of dependent-related failures per second as measured over a 10 second interval." )
	public double getDependentErrorRate( ) {
		return dependentErrorRate.calculateRate();
	}
	
	/**
	 * Returns the last time a dependent error occurred.
	 * @return the last dependent error time
	 */
	@MonitorableStatusValue( name = "last_dependent_error_datetime", description = "The last date and time a dependent error occurred." )
	public DateTime getLastDependentError( ) {
		return lastDependentError;
	}
}