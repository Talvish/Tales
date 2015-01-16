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
package com.talvish.tales.system.configuration;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.parts.naming.NameManager;
import com.talvish.tales.parts.naming.NameValidator;
import com.talvish.tales.parts.naming.SegmentedLowercaseValidator;


/**
 * Represents an in-memory representation of a configuration setting.
 * Information is tracked about usage, request times and source.
 * @author jmolnar
 */
public class LoadedSetting {
	public static final String SETTING_NAME_VALIDATOR = "tales.configuration.setting_name";
	
	static {
		if( !NameManager.hasValidator( LoadedSetting.SETTING_NAME_VALIDATOR ) ) {
			NameManager.setValidator( LoadedSetting.SETTING_NAME_VALIDATOR, new SegmentedLowercaseValidator( ) );
		}
	}
	
	private final String name;
	private final Object value;
	private final String description;
	private final String stringValue;
	private final String source;
	private final boolean sensitive;
	private int requests;
	private final DateTime firstRequestTime;
	private DateTime lastRequestTime;

	public LoadedSetting( String theName, Object theValue, String theStringValue, String theSource ) {
		this( theName, theValue, theStringValue, null, false, theSource );
	}

	public LoadedSetting( String theName, Object theValue, String theStringValue, String theDescription, boolean isSensitive, String theSource ) {
		NameValidator nameValidator = NameManager.getValidator( LoadedSetting.SETTING_NAME_VALIDATOR );
		
		Preconditions.checkArgument( !Strings.isNullOrEmpty(theName), "missing a name");
		Preconditions.checkArgument( nameValidator.isValid( theName ), String.format( "Configuration name '%s' from source '%s' does not conform to validator '%s'.", theName, theSource, nameValidator.getClass().getSimpleName() ) );
		
		this.name = theName;
		this.value = theValue;
		this.stringValue = theStringValue;
		this.description = theDescription;
		this.source = theSource;
		this.sensitive = isSensitive;
		this.requests = 1;
		this.firstRequestTime = new DateTime( DateTimeZone.UTC );
		this.lastRequestTime = this.firstRequestTime;
	}
	
	/**
	 * The name of the configuration value that was used.
	 * @return the name of the configuration value
	 */
	public String getName( ) {
		return name;
	}
	
	/**
	 * The value that was requested.
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * The string representation of the value requested.
	 * @return
	 */
	public String getStringValue( ) {
		return stringValue;
	}
	
	/**
	 * Returns the description of the setting.
	 * @return
	 */
	public String getDescription( ) {
		return description;
	}
	
	/**
	 * Indicates whether the setting contains sensitive information and should not be logged nor shared.
	 * @return true if a sensitive setting, false otherwise
	 */
	public boolean isSensitive( ) {
		return sensitive;
	}
	
	/**
	 * The location where the configuration value was retrieved from.
	 * @return The location where the value was retrieved from
	 */
	public String getSource( ) {
		return source;
	}
	/**
	 * Returns the number of times a request has been made for the configuration value.
	 * @return the number of times a request has been made
	 */
	public int getRequests( ) {
		return this.requests;
	}
	
	/**
	 * Called to indicate more request for the configuration object.
	 * @return returns the updated request number
	 */
	public int indicateRequested( ) {
		this.requests += 1;
		this.lastRequestTime = new DateTime( DateTimeZone.UTC );
		return this.requests;
	}

	/**
	 * Returns the time the first request for configuration occurred.
	 * @return the first request time
	 */
	public DateTime getFirstRequestTime() {
		return firstRequestTime;
	}

	/**
	 * Returns the time the last request for configuration occurred.
	 * @return the lastRequestTime
	 */
	public DateTime getLastRequestTime() {
		return lastRequestTime;
	}
}