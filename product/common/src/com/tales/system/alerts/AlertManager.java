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
package com.tales.system.alerts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

/**
 * This is a manager for alerts.
 * @author jmolnar
 *
 */
public class AlertManager {
	private final Object lock = new Object( );
	private Collection<Alert> alerts = new ArrayList<Alert>( 0 );
	
	/**
	 * Default constructor.
	 */
	public AlertManager( ) {
	}

	/**
	 * This method is called to register an alert.
	 * @param theAlert the alert to register
	 */
	public void register( Alert theAlert ) {
		Preconditions.checkNotNull( theAlert, "must have an alert" );
		
		synchronized( lock ) {
			ArrayList<Alert> newAlerts = new ArrayList<Alert>( this.alerts );

			newAlerts.add( theAlert );
           	this.alerts = Collections.unmodifiableCollection( newAlerts );
		}
	}
	

	/**
	 * Returns the alerts managed by this manager.
	 * @return the alerts
	 */
	public Collection<Alert> getAlerts( ) {
		return this.alerts;
	}
}