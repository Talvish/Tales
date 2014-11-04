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
package com.talvish.tales.system.alerts;
//
//import com.tales.services.status.RatedLong;
//
public class Alert {
//	// we dont' check the 'failure' side unless the warning also fires
//
	private final String name;
//	private final long monitorInterval;
//	private final AlertRule warningRule;
//	private final AlertRule failureRule;
//
//	private final RatedLong warningRate	= new RatedLong( );
//	private final RatedLong failureRate = new RatedLong( );
//	
//	// alerts can be driven by 'fire' or by 'read'
//
	
	/**
	 * Constructor for the alert.
	 * @param theName the name for the alert
	 */
	public Alert( String theName ) {
		name = theName;
	}
	
	public String getName( ) {
		return name;
	}
//	
//	public void fireWarning( ) {
//		
//	}
//	
//	public void fireFailure( ) {
//		
//	}
}
