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

// NOTE: note implemented
public class IHealth {
	//if I can get the ability to pull configuration for caching from the configuration THEN perhaps I can
	//put throttling as a setting as well for the methods

	// performance
	// total operations
	// operations per second (over interval) ..
	// calls to apis a second (over interval) .. trending up/down
	// compared to expected values for indicating issues
	// exceptions
	// types of failures report
	// alerts
	// current up-time
	// start-time
	// end-time
	// retries
	//   retries per second
	// memory usage
	// cpu usage?
	
	
	// status
	// - good/bad/ugly
	// - whether we are suspended or not
	// - maybe introduce the status piece for this
	
	// for calculating rates, if we can do some form of efficient ring then we can have calculations 
	// occur automatically when something is added to hte list and when something is removed, question 
	// is how can you do this calculation quickly because we are hoping for a rolling window, essentially
	// ... items will feed in periodically, we have a rolling window time for the calculation and items die-off when we get too big

	
	// what about some form of auto-dependency handling, including (maybe) jars but also other services that are used by detecting outgoing
	// connections and if using contracts, the contracts we use
	
	// have configured rate limits
	// have other items like in facebook where if the queue amount is bigger than the processing amount, we have a problem (warning maybe, not evil)
	
	// for some of these use a facade ... something yhou can place in front and replace to increase loggings
	// ideally we do it dynamically
	// it would be nice to have a way to load characteristics up
	
	//"work.http_interface_core.state == RUNNING ^ 50 seconds @ 5 seconds"
	
//	public class HealthCheck {
//		private class HealthLevel {
//			private Rule rule;
//			private Period observationLength;		
//			private Severity level; // normal, warning, alert
//		}
//		
//		private HealthLevel warning;
//		private HealthLevel alert;
//	}
//	
//	public class HealthState {
//		private HealthCheck check;
//		private Severy
//	}
	
}
