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
package com.tales.samples.userservice;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.tales.system.status.MonitorableStatusValue;
import com.tales.system.status.RatedLong;

/**
 * 
 * @author Joseph Molnar
 *
 */
public class UserEngineStatus {
	private AtomicLong createdUserCount = new AtomicLong( 0 );
	private RatedLong createdUserRate = new RatedLong( );
	private DateTime lastCreatedDate;

	private AtomicLong deletedUserCount = new AtomicLong( 0 );
	private RatedLong deletedUserRate = new RatedLong( );
	private DateTime lastDeletedDate;

	/**
	 * Indicates a user was created. 
	 */
	public void recordCreatedUser( ) {
		createdUserCount.incrementAndGet();
		createdUserRate.increment();
		lastCreatedDate = new DateTime( DateTimeZone.UTC );
	}
	
	/**
	 * The number of users created since last status reset.
	 */
	@MonitorableStatusValue( name = "created_user_count", description = "The total number of users created." )
	public long getCreateUserCount( ) {
		return createdUserCount.get( );
	}

	/**
	 * The rate of users created.
	 */
	@MonitorableStatusValue( name = "created_user_rate", description = "Rate of the number of users created per second as measured over a 10 second interval." )
	public double getCreatedUserRate( ) {
		return createdUserRate.calculateRate();
	}
	
	/**
	 * Returns the last time a user was created.
	 */
	@MonitorableStatusValue( name = "last_created_user", description = "The last date and time a user was created." )
	public DateTime getLastCreatedDate( ) {
		return lastCreatedDate;
	}
	
	
	/**
	 * Indicates a user was created. 
	 */
	public void recordDeletedUser( ) {
		deletedUserCount.incrementAndGet();
		deletedUserRate.increment();
		lastDeletedDate = new DateTime( DateTimeZone.UTC );
	}
	
	/**
	 * The number of users updated since last status reset.
	 */
	@MonitorableStatusValue( name = "deleted_user_count", description = "The total number of users deleted." )
	public long getDeletedUserCount( ) {
		return deletedUserCount.get( );
	}

	/**
	 * The rate of users deleted.
	 */
	@MonitorableStatusValue( name = "deleted_user_rate", description = "Rate of the number of users deleted per second as measured over a 10 second interval." )
	public double getDeletedUserRate( ) {
		return deletedUserRate.calculateRate();
	}
	
	/**
	 * Returns the last time a user was created.
	 */
	@MonitorableStatusValue( name = "last_deleted_user", description = "The last date and time a user was deleted." )
	public DateTime getLastDeletedDate( ) {
		return lastDeletedDate;
	}
}
