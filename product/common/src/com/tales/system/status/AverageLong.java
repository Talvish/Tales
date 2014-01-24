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

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;

/**
 * This class is used to calculate an average of a set of values that are calculated
 * within a specified time ranges. The calculation is based on 
 * tracking two measurement intervals.
 * @author jmolnar
 *
 */
public class AverageLong {
	private final Object lock = new Object( );
	
	private final long intervalPeriod;
	
	/**
	 * Constructor which will set the measurement over 20 seconds.
	 */
	public AverageLong( ) {
		this( 20l * 1000l * 1000l * 1000l );
		
	}
	
	/**
	 * Constructor taking interval time span to use. 
	 * The total interval time span used to calculate the average.
	 * @param theIntervalPeriod the length of time for the total measuring period. The interval is in nanoseconds.
	 */
	public AverageLong( long theTotalIntervalPeriod ) {
		Preconditions.checkArgument( theTotalIntervalPeriod > 0, "interval period must be a positive number" );
		intervalPeriod = theTotalIntervalPeriod / 2l;
	}
	
	/**
	 * Simple private class holding start/end timestamp and value being measured.
	 * @author jmolnar
	 *
	 */
	private class Interval {
		private long startTimestamp = 0;
		private long endTimestamp = 0;
		private final AtomicLong count = new AtomicLong( 0 );
		private final AtomicLong value = new AtomicLong( 0 );
	}
	
	private final Interval intervalOne = new Interval( );
	private final Interval intervalTwo = new Interval( );
	
	/**
	 * Calculates the average of the longs based in, as a long
	 * @return the average of the values
	 */
	public long calculateAverage( ) {
		long currentNanoTime = System.nanoTime();

		long intervalTwoStartTimestamp;
		long intervalTwoEndTimestamp;

		double intervalOneValue;
		double intervalOneCount;
		double intervalTwoValue;
		double intervalTwoCount;

		long average;

		synchronized( lock ) {
			intervalTwoStartTimestamp = intervalTwo.startTimestamp;
			intervalTwoEndTimestamp = intervalTwo.endTimestamp;

			if( currentNanoTime > ( intervalTwoEndTimestamp + intervalPeriod ) ) {
				average = 0;
				
			} else {
				if( currentNanoTime > intervalTwoEndTimestamp ) {
					intervalTwoStartTimestamp = intervalTwoEndTimestamp;
					// we don't set the end time, because we don't use it
					intervalTwoValue = 0;
					intervalTwoCount = 0; // to ensure it divides right
					intervalOneValue = intervalTwo.value.get( );
					intervalOneCount = intervalTwo.count.get( );

				} else {
					intervalTwoValue = intervalTwo.value.get( );
					intervalTwoCount = intervalTwo.count.get( );
					intervalOneValue = intervalOne.value.get();
					intervalOneCount = intervalOne.count.get( );
				}
	
				
				double periodTwoTimeOverlap = currentNanoTime - intervalTwoStartTimestamp;
				double periodOnePercentage =  1.0d - ( periodTwoTimeOverlap / (double ) intervalPeriod );
				double divisor = intervalTwoCount + periodOnePercentage * intervalOneCount;
				
				average = ( long ) (divisor == 0.0d ? 0.0d :  ( intervalTwoValue + periodOnePercentage * intervalOneValue ) / divisor );
			} 
		}

		return average;
	}

	/**
	 * Adds a new item to add to the average.
	 * @param theValue the value to add to the average
	 */
	public void add( long theValue ) {
		long currentNanoTime = System.nanoTime();
		synchronized( lock ) {
			long extendedPeriodTwoNanoTime = intervalTwo.endTimestamp + intervalPeriod;
			
			if( currentNanoTime > extendedPeriodTwoNanoTime ) {
				// if the current time is beyond second one +interval
				// we reset them both time-wise and add to interval
				intervalTwo.startTimestamp = System.nanoTime();
				intervalTwo.endTimestamp = intervalTwo.startTimestamp + intervalPeriod;
				intervalTwo.value.set( theValue );
				intervalTwo.count.set( 1 );
				intervalOne.endTimestamp = intervalTwo.startTimestamp;
				intervalOne.startTimestamp = intervalOne.endTimestamp - intervalPeriod;
				intervalOne.value.set( 0 );
				intervalOne.count.set( 0 );
			} else if( currentNanoTime > intervalTwo.endTimestamp ) {
				// we are less than the interval two end + the interval length 
				// so we need to make the second interval become the first interval
				// reset the new second interval and add to the interval two
				intervalOne.startTimestamp = intervalTwo.startTimestamp;
				intervalOne.endTimestamp = intervalTwo.endTimestamp;
				intervalOne.value.set( intervalTwo.value.get( ) );
				intervalOne.count.set( intervalTwo.count.get( ) );
				intervalTwo.startTimestamp = intervalOne.endTimestamp;
				intervalTwo.endTimestamp = intervalTwo.startTimestamp + intervalPeriod;
				intervalTwo.value.set( theValue );
				intervalTwo.count.set( 1 );
			} else {
				// just add to second interval
				intervalTwo.value.addAndGet( theValue );
				intervalTwo.count.incrementAndGet();
			}
		}
	}
}
