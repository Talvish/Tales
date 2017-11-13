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
package com.talvish.tales.client.http;

import com.google.common.base.Strings;

import com.talvish.tales.client.http.ResourceConfigurationBase;
import com.talvish.tales.system.configuration.annotated.Setting;
import com.talvish.tales.system.configuration.annotated.Settings;
import com.talvish.tales.validation.Conditions;
import com.talvish.tales.validation.OnValidation;

/**
 * The configuration needed to setup the object id manager to retrieving object ids locally.
 * @author jmolnar
 *
 */
@SuppressWarnings("rawtypes")
@Settings( )
abstract public class ResourceConfigurationBase <T extends ResourceConfigurationBase> {
	
	@Setting( name="{prefix}.endpoint", required=true )
	private String endpoint;
	
	@Setting( name="{prefix}.allow_untrusted_ssl" )
	private boolean allowUntrustedSsl = false;
	
	/**
	 * Default constructor for serialization.
	 */
	public ResourceConfigurationBase( ) {
	}
	
	/**
	 * Returns the endpoint to communicate with.
	 */
	public String getEndpoint( ) {
		return endpoint;
	}
	
	/**
	 * Sets the endpoint to communicate with.
	 * @param theEndpoint the endpoint to communicate with
	 * @return the configuration object so setters can be chained
	 */
	@SuppressWarnings("unchecked")
	public T setEndpoint( String theEndpoint ) {
		endpoint = theEndpoint;
		return ( T )this;
	}
	
	/**
	 * Indicates if SSL must be fully trusted and verified. 
	 * If you are using self-signed certs then you must allow untrusted SSL.
	 */
	public boolean getAllowUntrustedSsl( ) {
		return allowUntrustedSsl;
	}
	
	/**
	 * Sets whether allowing communication over an untrusted/verified SSL connection 
	 * is allowed. If you are using self-signed certs, that you must allow untrusted
	 * SSL.
	 * @param allow true means to allow untrusted SSL, false means not
	 * @return the configuration object so setters can be chained
	 */
	@SuppressWarnings("unchecked")
	public T setAllowUntrustedSsl( boolean allow ) {
		allowUntrustedSsl = allow;
		return ( T )this;
	}
	
	@OnValidation
	public void validate( ) {
		Conditions.checkConfiguration( !Strings.isNullOrEmpty( endpoint ), "the endpoint is either null or empty" );
	}
}
