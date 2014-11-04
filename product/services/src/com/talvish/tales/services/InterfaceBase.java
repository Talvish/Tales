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
package com.talvish.tales.services;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.contracts.services.ContractManager;
import com.talvish.tales.contracts.services.ServiceContract;
import com.talvish.tales.parts.naming.LowerCaseEntityNameValidator;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.services.Interface;
import com.talvish.tales.services.Service;
import com.talvish.tales.system.ExecutionLifecycleListener;
import com.talvish.tales.system.ExecutionLifecycleListeners;
import com.talvish.tales.system.ExecutionLifecycleState;
import com.talvish.tales.system.status.MonitorableStatusValue;
import com.talvish.tales.system.status.RatedLong;
import com.talvish.tales.system.status.StatusBlock;
import com.talvish.tales.system.status.StatusManager;

/**
 * This class represents a host/port that servlets can be bound to. 
 * @author jmolnar
 *
 */
public abstract class InterfaceBase implements Interface {
	public static final String INTERFACE_NAME_VALIDATOR = "interface_name";
	
	static {
		if( !NameManager.hasValidator( InterfaceBase.INTERFACE_NAME_VALIDATOR ) ) {
			NameManager.setValidator( InterfaceBase.INTERFACE_NAME_VALIDATOR, new LowerCaseEntityNameValidator() );
		}
	}
	
	/**
	 * Stored information regarding the status of the interface.
	 * @author jmolnar
	 *
	 */
	public class Status {
		private DateTime startTime				= null;
		private DateTime stopTime				= null;
		private DateTime suspendTime			= null;
		private AtomicLong suspends				= new AtomicLong( 0 );
		private RatedLong suspendRate			= new RatedLong( );
		
		/**
		 * Default empty constructor.
		 */
		public Status( ) {
		}

		/**
		 * Records the interface starting.
		 */
		public void recordStart( ) {
			startTime = new DateTime( DateTimeZone.UTC );
			stopTime = null;
		}

		/**
		 * Records the interface stopping.
		 */
		public void recordStop( ) {
			Preconditions.checkState( startTime != null, "Cannot record a start when a stop hasn't happend." );
			stopTime = new DateTime( DateTimeZone.UTC );
		}
		
		/**
		 * Records the interface being suspended.
		 */
		public void recordSuspended( ) {
			Preconditions.checkState( suspendTime == null, "Cannot record a suspend when a suspend has already happened." );
			suspendTime = new DateTime( DateTimeZone.UTC );
			suspends.incrementAndGet();
			suspendRate.increment();
		}
		
		/**
		 * Records the interface resuming.
		 */
		public void recordResumed( ) {
			Preconditions.checkState( suspendTime != null, "Cannot record a suspend when a suspend hasn't already happened." );
			suspendTime = null;
		}
		
		/**
		 * Returns the current execution state of the interface.
		 * @return the execution state
		 */
		@MonitorableStatusValue( name = "state", description = "The current execution state of the interface." )
		public ExecutionLifecycleState getState( ) {
			return InterfaceBase.this.lifecycleState;
		}
	
		/**
		 * Returns the start time that was recorded.
		 * @return the start time
		 */
		@MonitorableStatusValue( name = "start_running_datetime", description = "The date and time the interface started running." )
		public DateTime getStartTime( ) {
			return this.startTime;
		}
		
		/**
		 * Calculates the length of the time the interface has been running.
		 * @return the running time, or Period. ZERO if not currently running
		 */
		@MonitorableStatusValue( name = "elapsed_running_time", description = "The amount of time the interface has been running." )
		public Period calculateRunningTime( ) {
			if( stopTime == null  ) {
				return new Period( startTime, new DateTime( DateTimeZone.UTC ), PeriodType.standard( ) );
			} else {
				return Period.ZERO;
			}
		}
		/**
		 * Returns the stop time that was recorded.
		 * @return the stop time
		 */
		public DateTime getStopTime( ) {
			return this.stopTime;
		}
		
		/**
		 * Returns the time the interface was suspended, if currently suspended.
		 * @return the suspended time, or null if not currently suspended
		 */
		@MonitorableStatusValue( name = "start_suspend_datetime", description = "The date and time the interface was suspend." )
		public DateTime getSuspendTime( ) {
			return this.suspendTime;
		}
		
		/**
		 * Calculates the length of the time the interface has been suspended.
		 * @return the length of time suspended, or Period.ZERO if not suspended.
		 */
		@MonitorableStatusValue( name = "elapsed_suspend_time", description = "The amount of time the interface has been suspended." )
		public Period calculateSuspendTime( ) {
			if( suspendTime != null  ) {
				return new Period( suspendTime, new DateTime( DateTimeZone.UTC ), PeriodType.standard( ) );
			} else {
				return Period.ZERO;
			}
		}

		/**
		 * Returns the number of times the interface has been suspended.
		 * @return the total number of suspends
		 */
		@MonitorableStatusValue( name = "suspends", description = "The total number of times the interface has been suspended since the interface was started." )
		public long getSuspends( ) {
			return this.suspends.get( );
		}

		/**
		 * Returns the rate of suspends on the interface.
		 * @return the suspend ate
		 */
		@MonitorableStatusValue( name = "suspend_rate", description = "The rate, in seconds, of the number of suspends as measured over 10 seconds." )
		public double getSuspendRate( ) {
			return this.suspendRate.calculateRate();
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger( InterfaceBase.class );

	private final Service service;
	private final String name;
	private final ContractManager contractManager;

	private final Status status = new Status( );
	private final StatusManager statusManager = new StatusManager( );
	
	private ExecutionLifecycleListeners  listeners 	= new ExecutionLifecycleListeners( );
	private ExecutionLifecycleState lifecycleState	= ExecutionLifecycleState.CREATED;

	/**
	 * Constructor taking the items needed for the interface to start.
	 * @param theName the name given to the interface
	 * @param theService the service the interface will be bound to
	 */
	public InterfaceBase( String theName, Service theService ) {
		NameValidator nameValidator = NameManager.getValidator( InterfaceBase.INTERFACE_NAME_VALIDATOR );
		
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "the interface must have a name" );
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Interface name '%s' does not conform to validator '%s'.", theName, nameValidator.getClass().getSimpleName() ) );
		Preconditions.checkNotNull( theService, "need a service" );
		
		service = theService;
		name = theName;
		// make sure we have a contract manager
		contractManager = new ContractManager();
		
		// get the status blocks setup
		statusManager.register( "interface", status );
	}
	
	/**
	 * Gets the name given to the interface.
	 * @return the name of the interface
	 */
	public final String getName( ) {
		return name;
	}
	
	/**
	 * Returns the service this interface is part of.
	 * @return the service this interface is part of.
	 */
	public final Service getService( ) {
		return this.service;
	}
	
	/**
	 * Returns the contract manager used by the interface.
	 * @return the underlying contract manager
	 */
	protected final ContractManager getContractManager( ) {
		return this.contractManager;
	}
	
	/**
	 * Returns the status manager used by the interface.
	 * @return the underlying status manager
	 */
	protected final StatusManager getStatusManager( ) {
		return this.statusManager;
	}
	
	/**
	 * Returns the status information for the interface.
	 * @return
	 */
	public final Status getStatus( ) {
		return this.status;
	}

	/**
	 * Returns the set of status blocks for the interface
	 * @return the interface status blocks
	 */
	public final Collection<StatusBlock> getStatusBlocks( ) {
		return this.statusManager.getStatusBlocks();
	}
	
	/**
	 * Returns the contracts bound to the interface.
	 * @return the contracts bound to the interface
	 */
	public final Collection<ServiceContract> getBoundContracts( ) {
		return this.contractManager.getContracts();
	}
	
	/**
	 * Adds an object interested in getting execution state updates.
	 * @param theListener the listener to add
	 */
	public final void addListener( ExecutionLifecycleListener theListener ) {
		listeners.addListener( theListener );
	}
	
	/**
	 * Removes an object that was once interested in getting execution state updates.
	 * @param theListener the listener to remove
	 */
	public final void removeListener( ExecutionLifecycleListener theListener ) {
		listeners.removeListener( theListener );
	}

	
	/**
	 * Returns the current lifecycle state of the interface.
	 * @return the current lifecycle state
	 */
	public final ExecutionLifecycleState getState( ) {
		return this.lifecycleState;
	}
	
	/**
	 * Starts the interface.
	 * @throws Exception
	 */
	public final void start( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.CREATED, "Cannot start an interface when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STARTING;
		logger.info( "Starting interface '{}'.", this.name );
		this.listeners.onStarting( this, this.lifecycleState );

		this.onStart();
		
		this.lifecycleState = ExecutionLifecycleState.STARTED;
		status.recordStart();
		this.listeners.onStarted( this, this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Mechanism that allows subclasses to provide start support.
	 */
	protected void onStart( ) {		
	}
	
	/**
	 * Stops the interface.
	 * @throws Exception
	 */
	public final void stop( ) {
		Preconditions.checkState( this.lifecycleState == ExecutionLifecycleState.STARTED || this.lifecycleState == ExecutionLifecycleState.RUNNING || this.lifecycleState == ExecutionLifecycleState.SUSPENDED, "Cannot stop an interface when the status is '%s'.", this.lifecycleState );
		this.lifecycleState = ExecutionLifecycleState.STOPPING;
		logger.info( "Stopping interface '{}'." );
		this.listeners.onStopping( this, this.lifecycleState );

		this.onStop( );
		
		this.lifecycleState = ExecutionLifecycleState.STOPPED;
		status.recordStop();
		this.listeners.onStopped( this, this.lifecycleState );
	}
	
	/**
	 * Mechanism that allows subclasses to provide stop support.
	 */
	protected void onStop( ) {		
	}
	
	/**
	 * This is call to suspend an interface, which means requests to
	 * contracts on this interface will return a Failure.LOCAL_UNAVAILABLE.
	 * This will not pause any operations in progress.
	 */
	public final void suspend( ) {
		suspend( null );
	}
	
	/**
	 * This is call to suspend an interface, which means requests to
	 * contracts on this interface will return a Failure.LOCAL_UNAVAILABLE.
	 * This will not pause any operations in progress.
	 * The parameter it takes is the length of time we will report that
	 * the suspend will be running for. This does not mean the suspend
	 * will automatically resume, it is just a notification to callers.
	 * @param theLength the length,in seconds, of how long the delay is expected
	 */
	private void suspend( Integer theLength ) {
		Preconditions.checkState( canSuspend( ), "Cannot suspend an interface when the status is '%s'.", this.lifecycleState );
		Preconditions.checkArgument( theLength == null || theLength >= 0, "The delay length cannot be negative." );
		
		this.onSuspend( );
		
		this.lifecycleState = ExecutionLifecycleState.SUSPENDED;
		//this.suspendLength =  theLength;
		status.recordSuspended();
		this.listeners.onSuspended( this, this.lifecycleState );
	}
	
	/**
	 * Mechanism that allows subclasses to provide suspend support.
	 */
	protected void onSuspend( ) {		
	}

	/**
	 * This is called to resume a previously suspended interface.
	 */
	public final void resume( ) {
		Preconditions.checkState( canResume( ), "Cannot resume an interface when the status is '%s'.", this.lifecycleState );
		
		this.onResume();
		
		this.lifecycleState = ExecutionLifecycleState.RUNNING;
		status.recordResumed();
		this.listeners.onRunning( this, this.lifecycleState );
	}
	
	/**
	 * Mechanism that allows subclasses to provide resume support.
	 */
	protected void onResume( ) {		
	}
	
	/**
	 * Helper method that indicates if the interface is in a state that will
	 * allow suspending.
	 * @return return true if suspendable, false otherwise
	 */
	public boolean canSuspend( ) {
		return this.lifecycleState == ExecutionLifecycleState.RUNNING;
	}
	
	/**
	 * Helper method that indicates if the interface is suspended so that
	 * it can be resumed.
	 * @return return true if resumable, false otherwise
	 */
	public boolean canResume( ) {
		return this.lifecycleState == ExecutionLifecycleState.SUSPENDED;
	}
}
