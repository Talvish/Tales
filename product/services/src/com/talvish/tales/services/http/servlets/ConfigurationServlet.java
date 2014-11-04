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
package com.talvish.tales.services.http.servlets;

import java.io.IOException;
import java.util.Map;





import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.talvish.tales.contracts.services.http.ServletContract;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.services.Service;
import com.talvish.tales.services.OperationContext.Details;
import com.talvish.tales.services.http.AttributeConstants;
import com.talvish.tales.services.http.ResponseHelper;
import com.talvish.tales.system.configuration.LoadedSetting;


/**
 * This is a simple servlet that shows the configuration values used by the associated service.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.configuration", versions="20111005")
@SuppressWarnings("serial")
public class ConfigurationServlet extends AdministrationServlet {
    private static final Logger logger = LoggerFactory.getLogger( ConfigurationServlet.class );

    /**
     * Empty, default constructor.
     */
    public ConfigurationServlet( ) {
    }
    
	/**
	 * Implementation of the get method to get the configuration information.
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Implementation of the post method to get the configuration information.
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Private implementation of the request method which sends the list of configuration information.
	 */
	private void doCall( HttpServletRequest theRequest, HttpServletResponse theResponse ) {
//		if( theRequest.getParameterMap().size() != 0 ) {
//			ResponseHelper.writeFailure( 
//					theRequest,
//					theResponse, 
//					Failure.CALLER_BAD_DATA, 
//					"Parameters are not expected.",
//					null );
//		} else {
		
			logger.info( "Request made to list the configuration." );
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			Map<String, LoadedSetting> settings = getService().getConfigurationManager( ).getAllLoadedSettings();

			Service service = getService( );
			JsonObject serviceObject = new JsonObject( );
			JsonArray settingsArray = new JsonArray( );
			JsonObject settingObject;

			serviceObject.addProperty( "name", service.getCanonicalName( ) );

			for( LoadedSetting setting : settings.values() ) {
				settingObject = new JsonObject( );
				settingObject.addProperty( "name", setting.getName( ) );
				if( !setting.isSensitive() ) {
					settingObject.addProperty( "value", setting.getStringValue( ) );
				}
				settingObject.addProperty( "source", setting.getSource( ) );
				if( operationContext.getResponseDetails() == Details.ALL ) {
					settingObject.addProperty( "description", setting.getDescription() );
					settingObject.addProperty( "sensitive", setting.isSensitive() );
					settingObject.addProperty( "first_request_datetime", setting.getFirstRequestTime().toString() );
					settingObject.addProperty( "last_request_datatime", setting.getLastRequestTime().toString( ) );
					settingObject.addProperty( "requests", setting.getRequests() );
				}

				settingsArray.add( settingObject );
			}
			serviceObject.add( "settings", settingsArray );

			ResponseHelper.writeSuccess(theRequest, theResponse, serviceObject );
//		}
	}
}
