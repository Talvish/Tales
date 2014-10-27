// ***************************************************************************
// *  Copyright 2012 Joseph Molnar
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
package com.tales.services;

/**
 * Class containing constants frequently used as configuration keys.
 * @author jmolnar
 *
 */
public final class ConfigurationConstants {

	// basic service settings
	
	public static final String SERVICE_TYPE = "service.type";
	
	// key store settings
	
	public static final String SECURITY_KEY_STORES = "service.security.key_stores";
	
	public static final String SECURITY_KEY_STORE_LOCATION_FORMAT = SECURITY_KEY_STORES + ".%s.location";
	public static final String SECURITY_KEY_STORE_PASSWORD_FORMAT = SECURITY_KEY_STORES + ".%s.password";
	public static final String SECURITY_KEY_STORE_TYPE_FORMAT = SECURITY_KEY_STORES + ".%s.type";
	public static final String SECURITY_KEY_STORE_PROVIDER_FORMAT = SECURITY_KEY_STORES + ".%s.format";
	
	// interface settings

	public static final String INTERFACES = "service.interfaces";
	
	public static final String INTERFACE_TYPE = INTERFACES + ".%s.type";	
	public static final String HTTP_INTERFACE_ENDPOINTS = INTERFACES + ".%s.endpoints"; 
	public static final String HTTP_INTERFACE_SSL_KEY_STORE = INTERFACES + ".%s.ssl.key_store"; 
	public static final String HTTP_INTERFACE_SSL_CERT_ALIAS = INTERFACES + ".%s.ssl.cert_alias";
	public static final String HTTP_INTERFACE_CONNECTOR = INTERFACES + ".%s.connector";
	
	// connector settings
	
	public static final String HTTP_CONNECTORS = "service.http_connectors";
	
	public static final String HTTP_CONNECTORS_ACCEPTORS = HTTP_CONNECTORS + ".%s.acceptors";
	public static final String HTTP_CONNECTORS_ACCEPT_QUEUE_SIZE = HTTP_CONNECTORS + ".%s.accept_queue_size";
	public static final String HTTP_CONNECTORS_IDLE_TIMEOUT = HTTP_CONNECTORS + ".%s.idle_timeout"; 
	
	public static final String HTTP_CONNECTORS_HEADER_CACHE_SIZE = HTTP_CONNECTORS + ".%s.header_cache_size";
	public static final String HTTP_CONNECTORS_REQUEST_HEADER_SIZE = HTTP_CONNECTORS + ".%s.request_header_size";
	public static final String HTTP_CONNECTORS_RESPONSE_HEADER_SIZE = HTTP_CONNECTORS + ".%s.response_header_size";
	public static final String HTTP_CONNECTORS_OUTPUT_BUFFER_SIZE = HTTP_CONNECTORS + ".%s.output_buffer_size"; 

	public static final String HTTP_CONNECTORS_SELECTORS= HTTP_CONNECTORS + ".%s.selectors";
	
	public static final String HTTP_CONNECTORS_MAX_FORM_CONTENT_SIZE = HTTP_CONNECTORS + ".%s.max_form_content_size";
	
	public static final String HTTP_CONNECTORS_REQUEST_BUFFER_SIZE = HTTP_CONNECTORS + ".%s.request_buffer_size";
	
	// thread pool settings
	
	public static final String THREAD_POOLS = "service.thread_pools";

	public static final String THREAD_POOL_THREAD_NAME_PREFIX = THREAD_POOLS + ".%s.thread_name_prefix";
	public static final String THREAD_POOL_THREAD_PRIORITY = THREAD_POOLS + ".%s.thread_priority";
	public static final String THREAD_POOL_THREAD_IS_DAEMON = THREAD_POOLS + ".%s.is_daemon";
	
	public static final String THREAD_POOL_CORE_SIZE = THREAD_POOLS + ".%s.core_size";
	public static final String THREAD_POOL_MAX_SIZE = THREAD_POOLS + ".%s.max_size";
	public static final String THREAD_POOL_KEEP_ALIVE_TIME = THREAD_POOLS + ".%s.keep_alive_time";
	public static final String THREAD_POOL_PRESTART_CORE = THREAD_POOLS + ".%s.prestart_core";
}
