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
package com.tales.storage;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.tales.system.status.AverageLong;
import com.tales.system.status.MonitorableStatusValue;
import com.tales.system.status.RatedLong;

/**
 * This class is used to track the status of a repository.
 * @author jmolnar
 *
 */
public class StorageStatus {
	private AtomicLong gets = new AtomicLong( 0 );
	private RatedLong getRate = new RatedLong( );
	private AtomicLong getItems = new AtomicLong( 0 );
	private AverageLong getItemAverage = new AverageLong( );
	private AverageLong getExecutionTime = new AverageLong( );
	private AtomicLong getErrors = new AtomicLong( 0 );
	private RatedLong getErrorRate = new RatedLong( );
	private DateTime lastGet = null;

	
	@MonitorableStatusValue( name="get_count", description="Total number of calls to get items from the store since the repository was initialized." )
	public long getGets( ) {
		return gets.get();
	}
	
	@MonitorableStatusValue( name="get_rate", description="Rate, in seconds, of calls to get items from the store as measured over a 10 second interval." )
	public double getGetRate( ) {
		return getRate.calculateRate();
	}

	public void recordGet( ) {
		gets.incrementAndGet();
		getRate.increment();
	}
	
	@MonitorableStatusValue( name="get_item_count", description="Total number of items returned from the store since the repository was initialized." )
	public long getGetItems( ) {
		return getItems.get( );
	}

	@MonitorableStatusValue( name="get_item_average", description="Average number of items returned from the store per call, as measured over a 20 second interval." )
	public long getGetItemAverage( ) {
		return getItemAverage.calculateAverage();
	}

	
	@MonitorableStatusValue( name="get_execution_time", description="Average time, in milliseconds as measured in nanoseconds, of the get call." )
	public double getGetExecutionTime( ) {
		return getExecutionTime.calculateAverage() / 1000000d;
	}
	
	@MonitorableStatusValue( name="last_get_datetime", description="The last time a get was executed." )
	public DateTime getLastGet( ) {
		return lastGet;
	}
	
	public void recordGetExecution( int theItemCount, long theExecutionTime ) {
		getItems.addAndGet( theItemCount );
		getItemAverage.add( theItemCount );
		getExecutionTime.add( theExecutionTime );
		lastGet = new DateTime( DateTimeZone.UTC );
	}

	
	@MonitorableStatusValue( name="get_error_count", description="Total number of failures while getting objects from the store since the repository was initialized." )
	public long getGetErrors( ) {
		return getErrors.get();
	}
	
	@MonitorableStatusValue( name="get_error_rate", description="Rate, in seconds, of failures while getting objects from the store as measured over a 10 second interval." )
	public double getGetErrorRate( ) {
		return getErrorRate.calculateRate();
	}

	public void recordGetError( ) {
		getErrors.incrementAndGet();
		getErrorRate.increment();
	}	


	private AtomicLong puts = new AtomicLong( 0 );
	private RatedLong putRate = new RatedLong( );
	private AtomicLong putItems = new AtomicLong( 0 );
	private AverageLong putItemAverage = new AverageLong( );
	private AverageLong putExecutionTime = new AverageLong( );
	private AtomicLong putErrors = new AtomicLong( 0 );
	private RatedLong putErrorRate = new RatedLong( );
	private DateTime lastPut = null;

	
	@MonitorableStatusValue( name="put_count", description="Total number of calls to put objects into the store since the repository was initialized." )
	public long getPuts( ) {
		return puts.get();
	}
	
	@MonitorableStatusValue( name="put_rate", description="Rate, in seconds, of calls to put objects into the store as measured over a 10 second interval." )
	public double getPutRate( ) {
		return putRate.calculateRate();
	}

	public void recordPut( ) {
		puts.incrementAndGet();
		putRate.increment();
	}
	
	@MonitorableStatusValue( name="put_item_count", description="Total number of items placed into the store since the repository was initialized." )
	public long getPutItems( ) {
		return putItems.get( );
	}

	@MonitorableStatusValue( name="put_item_average", description="Average number of items placed into the store per call, as measured over a 20 second interval." )
	public long getPutItemAverage( ) {
		return putItemAverage.calculateAverage();
	}

	@MonitorableStatusValue( name="put_execution_time", description="Average time, in milliseconds as measured in nanoseconds, of the put call." )
	public double getPutExecutionTime( ) {
		return putExecutionTime.calculateAverage() / 1000000d;
	}

	@MonitorableStatusValue( name="last_put_datetime", description="The last time a put was executed." )
	public DateTime getLastPut( ) {
		return lastPut;
	}

	public void recordPutExecution( int theItemCount, long theExecutionTime ) {
		putItems.addAndGet( theItemCount );
		putItemAverage.add( theItemCount );
		putExecutionTime.add( theExecutionTime );
		lastPut = new DateTime( DateTimeZone.UTC );
	}

	public void recordPutError( ) {
		putErrors.incrementAndGet();
		putErrorRate.increment();
	}

	private AverageLong checkedPutExecutionTime = new AverageLong( );
	private AtomicLong checkedPutSuccesses = new AtomicLong( 0 );
	private RatedLong checkedPutSuccessRate = new RatedLong( );
	private AtomicLong checkedPutFailures = new AtomicLong( 0 );
	private RatedLong checkedPutFailureRate = new RatedLong( );
	private DateTime lastCheckedPut = null;

	
	@MonitorableStatusValue( name="checked_put_execution_time", description="Average time, in milliseconds as measured in nanoseconds, of the checked put call (both success and failure)." )
	public double getCheckedPutExecutionTime( ) {
		return checkedPutExecutionTime.calculateAverage() / 1000000d;
	}

	@MonitorableStatusValue( name="checked_put_success_count", description="Total number of successful checked put calls since the repository was initialized. This is essentially the number of unconflicted row updates." )
	public long getSuccessfulCheckedPuts( ) {
		return checkedPutSuccesses.get();
	}
	
	@MonitorableStatusValue( name="checked_put_success_rate", description="Rate, in seconds, of successful checked put calls as measured over a 10 second interval." )
	public double getSuccessfulCheckedPutRate( ) {
		return checkedPutSuccessRate.calculateRate();
	}

	@MonitorableStatusValue( name="last_checked_put_datetime", description="The last time a checked put was executed." )
	public DateTime getLastCheckedPut( ) {
		return lastCheckedPut;
	}

	public void recordCheckedPutSuccess( long theExecutionTime ) {
		putItems.addAndGet( 1 ); 
		putItemAverage.add( 1 );  
		checkedPutSuccesses.addAndGet( 1 );
		checkedPutSuccessRate.increment( );
		checkedPutExecutionTime.add( theExecutionTime );
		lastCheckedPut = new DateTime( DateTimeZone.UTC );
	}

	@MonitorableStatusValue( name="checked_put_fail_count", description="Total number of unsuccessful checked put calls since the repository was initialized. This is essentially the number of concurrent row updates." )
	public long getFailedCheckedPuts( ) {
		return checkedPutFailures.get();
	}
	
	@MonitorableStatusValue( name="checked_put_fail_rate", description="Rate, in seconds, of unsuccessful checked put calls as measured over a 10 second interval." )
	public double getFailedCheckedPutRate( ) {
		return checkedPutFailureRate.calculateRate();
	}

	public void recordCheckedPutFailure( long theExecutionTime ) {
		checkedPutFailures.addAndGet( 1 );
		checkedPutFailureRate.increment( );
		checkedPutExecutionTime.add( theExecutionTime );
	}

	@MonitorableStatusValue( name="put_error_count", description="Total number of failures while putting objects into the store since the repository was initialized." )
	public long getPutErrors( ) {
		return putErrors.get();
	}
	
	@MonitorableStatusValue( name="put_error_rate", description="Rate, in seconds, of failures while putting objects into the store as measured over a 10 second interval." )
	public double getPutErrorRate( ) {
		return putErrorRate.calculateRate();
	}


	private AtomicLong deletes = new AtomicLong( 0 );
	private RatedLong deleteRate = new RatedLong( );
	private AtomicLong deleteItems = new AtomicLong( 0 );
	private AverageLong deleteItemAverage = new AverageLong( );
	private AverageLong deleteExecutionTime = new AverageLong( );
	private AtomicLong deleteErrors = new AtomicLong( 0 );
	private RatedLong deleteErrorRate = new RatedLong( );
	private DateTime lastDelete = null;
	
	@MonitorableStatusValue( name="delete_count", description="Total number of calls to delete objects from the store since the repository was initialized." )
	public long getDeletes( ) {
		return deletes.get();
	}
	
	@MonitorableStatusValue( name="delete_rate", description="Rate, in seconds, of calls to delete items from the store as measured over a 10 second interval." )
	public double getDeleteRate( ) {
		return deleteRate.calculateRate();
	}

	public void recordDelete( ) {
		deletes.incrementAndGet();
		deleteRate.increment();
	}
	
	@MonitorableStatusValue( name="delete_item_count", description="Total number of items deleted from the store since the repository was initialized." )
	public long getDeleteItems( ) {
		return deleteItems.get( );
	}

	@MonitorableStatusValue( name="delete_item_average", description="Average number of items deleted from the store per call, as measured over a 20 second interval." )
	public long getDeleteItemAverage( ) {
		return deleteItemAverage.calculateAverage();
	}

	@MonitorableStatusValue( name="delete_execution_time", description="Average time, in milliseconds as measured in nanoseconds, of the delete call." )
	public double getDeleteExecutionTime( ) {
		return deleteExecutionTime.calculateAverage() / 1000000d;
	}
	
	@MonitorableStatusValue( name="last_delete_datetime", description="The last time a delete was executed." )
	public DateTime getLastDelete( ) {
		return lastDelete;
	}
	
	public void recordDeleteExecution( int theItemCount, long theExecutionTime ) {
		deleteItems.addAndGet( theItemCount );
		deleteItemAverage.add( theItemCount );
		deleteExecutionTime.add( theExecutionTime );
		lastDelete = new DateTime( DateTimeZone.UTC );
	}

	@MonitorableStatusValue( name="delete_error_count", description="Total number of failures while deleting objects from the store since the repository was initialized." )
	public long getDeleteErrors( ) {
		return deleteErrors.get();
	}
	
	@MonitorableStatusValue( name="delete_error_rate", description="Rate, in seconds, of failures while deleting objects from the store as measured over a 10 second interval." )
	public double getDeleteErrorRate( ) {
		return deleteErrorRate.calculateRate();
	}

	public void recordDeleteError( ) {
		deleteErrors.incrementAndGet();
		deleteErrorRate.increment();
	}
}
