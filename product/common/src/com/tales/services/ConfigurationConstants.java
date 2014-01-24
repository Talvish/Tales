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
	public static String SECURITY_KEY_STORES = "service.security.key_stores";
	public static String SECURITY_KEY_STORE_LOCATION_FORMAT = SECURITY_KEY_STORES + ".%s.location";
	public static String SECURITY_KEY_STORE_PASSWORD_FORMAT = SECURITY_KEY_STORES + ".%s.password";
	public static String SECURITY_KEY_STORE_TYPE_FORMAT = SECURITY_KEY_STORES + ".%s.type";
	public static String SECURITY_KEY_STORE_PROVIDER_FORMAT = SECURITY_KEY_STORES + ".%s.format";
	
	public static String HTTP_INTERFACES = "service.http_interfaces";
	public static String HTTP_INTERFACE_ENDPOINTS = HTTP_INTERFACES + ".%s.endpoints"; 
	public static String HTTP_INTERFACE_SSL_KEY_STORE = HTTP_INTERFACES + ".%s.ssl.key_store"; 
	public static String HTTP_INTERFACE_SSL_CERT_ALIAS = HTTP_INTERFACES + ".%s.ssl.cert_alias";
	public static String HTTP_INTERFACE_CONNECTOR = HTTP_INTERFACES + ".%s.connector";
	
	public static String HTTP_CONNECTORS = "service.http_connectors";
	public static String HTTP_CONNECTORS_ACCEPTORS = HTTP_CONNECTORS + ".%s.acceptors";
	public static String HTTP_CONNECTORS_ACCEPT_QUEUE_SIZE = HTTP_CONNECTORS + ".%s.accept_queue_size";
	public static String HTTP_CONNECTORS_IDLE_TIMEOUT = HTTP_CONNECTORS + ".%s.idle_timeout"; 
	
	public static String HTTP_CONNECTORS_HEADER_CACHE_SIZE = HTTP_CONNECTORS + ".%s.header_cache_size";
	public static String HTTP_CONNECTORS_REQUEST_HEADER_SIZE = HTTP_CONNECTORS + ".%s.request_header_size";
	public static String HTTP_CONNECTORS_RESPONSE_HEADER_SIZE = HTTP_CONNECTORS + ".%s.response_header_size";
	public static String HTTP_CONNECTORS_OUTPUT_BUFFER_SIZE = HTTP_CONNECTORS + ".%s.output_buffer_size"; 

	public static String HTTP_CONNECTORS_SELECTORS= HTTP_CONNECTORS + ".%s.selectors";
	
	
	public static String HTTP_CONNECTORS_MAX_FORM_CONTENT_SIZE = HTTP_CONNECTORS + ".%s.max_form_content_size";

	
	public static String HTTP_CONNECTORS_REQUEST_BUFFER_SIZE = HTTP_CONNECTORS + ".%s.request_buffer_size";
}
