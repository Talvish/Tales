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

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;

/**
 * This class is used to calculate the rate, in seconds, based on the
 * changes made to a long. The calculation is based on 
 * tracking two measurement intervals.
 * @author jmolnar
 *
 */
public class RatedLong {
	private final Object lock = new Object( );
	
	private final long resultRate;
	private final long intervalPeriod;
	
	/**
	 * Constructor which will set the total measurement interval to ten seconds.
	 */
	public RatedLong( ) {
		this( 10l * 1000l * 1000l * 1000l );
		
	}
	
	/**
	 * Constructor taking interval time span to use. 
	 * The interval time span used to calculate the rate.
	 * @param theTotalIntervalPeriod the total length of time for the interval to aid measuring. The interval is in nanoseconds.
	 */
	public RatedLong( long theTotalIntervalPeriod ) {
		Preconditions.checkArgument( theTotalIntervalPeriod > 0, "interval period must be a positive number" );
		intervalPeriod = theTotalIntervalPeriod / 2l;
		resultRate = intervalPeriod / ( 1000l * 1000l * 1000l );
	}
	
	/**
	 * Simple private class holding start/end timestamp and value being measured.
	 * @author jmolnar
	 *
	 */
	private class Interval {
		private long startTimestamp = 0;
		private long endTimestamp = 0;
		private final AtomicLong value = new AtomicLong( 0 );
	}
	
	private final Interval intervalOne = new Interval( );
	private final Interval intervalTwo = new Interval( );
	
	/**
	 * Calculates the current rate of the long as measured in seconds.
	 * @return the rate of the value in seconds
	 */
	public double calculateRate( ) {
		long currentNanoTime = System.nanoTime();

		long intervalTwoStartTimestamp;
		long intervalTwoEndTimestamp;

		double intervalOneValue;
		double intervalTwoValue;

		double rate;

		synchronized( lock ) {
			intervalTwoStartTimestamp = intervalTwo.startTimestamp;
			intervalTwoEndTimestamp = intervalTwo.endTimestamp;

			if( currentNanoTime > ( intervalTwoEndTimestamp + intervalPeriod ) ) {
				rate = 0.0d;
				
			} else {
				if( currentNanoTime > intervalTwoEndTimestamp ) {
					intervalTwoStartTimestamp = intervalTwoEndTimestamp;
					// we don't set the end time, because we don't use it
					intervalTwoValue = 0;
					intervalOneValue = intervalTwo.value.get( );
				} else {
					intervalTwoValue = intervalTwo.value.get( );
					intervalOneValue = intervalOne.value.get();
				}
				
				double periodTwoTimeOverlap = currentNanoTime - intervalTwoStartTimestamp;
				double periodOnePercentage =  1.0d - ( periodTwoTimeOverlap / (double ) intervalPeriod );
	
				rate = (  intervalTwoValue + periodOnePercentage * intervalOneValue ) / ( ( double )resultRate );
			} 
		}

		return rate;
	}

	/**
	 * Adds a value of one to the current long being rated.
	 */
	public void increment( ) {
		add( 1l );
	}

	/**
	 * Adds the specified value to the current long being rated.
	 * @param theValue the value to add
	 */
	public void add( long theValue ) {
		long currentNanoTime = System.nanoTime();
		synchronized( lock ) {
			long extendedPeriodTwoNanoTime = intervalTwo.endTimestamp + intervalPeriod;
			
			if( currentNanoTime > extendedPeriodTwoNanoTime ) {
				// if the current time is beyond second one +interval
				// we reset them both time-wise and add to period two counter
				intervalTwo.startTimestamp = System.nanoTime();
				intervalTwo.endTimestamp = intervalTwo.startTimestamp + intervalPeriod;
				intervalTwo.value.set( theValue );
				intervalOne.endTimestamp = intervalTwo.startTimestamp;
				intervalOne.startTimestamp = intervalOne.endTimestamp - intervalPeriod;
				intervalOne.value.set( 0 );

			} else if( currentNanoTime > intervalTwo.endTimestamp ) {
				// we are less than the interval two end + the interval length 
				// so we need to make the second interval become the first interval
				// reset the new second interval and increase the interval two value
				intervalOne.startTimestamp = intervalTwo.startTimestamp;
				intervalOne.endTimestamp = intervalTwo.endTimestamp;
				intervalOne.value.set( intervalTwo.value.get( ) );
				intervalTwo.startTimestamp = intervalOne.endTimestamp;
				intervalTwo.endTimestamp = intervalTwo.startTimestamp + intervalPeriod;
				intervalTwo.value.set( theValue );

			} else {
				// just increment the second interval
				intervalTwo.value.addAndGet( theValue );
			}
		}
	}
}
