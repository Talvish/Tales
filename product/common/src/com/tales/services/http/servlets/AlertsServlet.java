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
package com.tales.services.http.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.tales.contracts.services.http.ServletContract;
import com.tales.parts.translators.Translator;
import com.tales.serialization.Readability;
import com.tales.services.Interface;
import com.tales.services.OperationContext;
import com.tales.services.Service;
import com.tales.services.OperationContext.Details;
import com.tales.services.http.AttributeConstants;
import com.tales.services.http.ResponseHelper;
import com.tales.system.alerts.Alert;
import com.tales.system.status.StatusBlock;
import com.tales.system.status.StatusValue;
/**
 * This is a simple servlet that shows the alerts exposed by the associated service.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.alerts", versions="20111005")
@SuppressWarnings("serial")
public class AlertsServlet extends AdministrationServlet {
	private static final Logger logger = LoggerFactory.getLogger( AlertsServlet.class );

	/**
	 * Implementation of the get method to get contract information.
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Implementation of the post method to get contract information.
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Private implementation of the request method which gets the contract information.
	 */ 
	private void doCall( HttpServletRequest theRequest, HttpServletResponse theResponse ) {
//		if( theRequest.getParameterMap().size() != 0 ) {
//			HttpService.writeFailure( 
//					theRequest,
//					theResponse, 
//					Failure.CALLER_BAD_DATA, 
//					"Parameters are not expected.",
//					null );
//		} else {
		logger.info( "Request made to list the alerts." );
		OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
		JsonObject serviceObject = new JsonObject( );
		Service service = getService( );
		
		
		serviceObject.addProperty( "name", service.getCanonicalName( ) );
		JsonArray alertArray = new JsonArray( );
//		
//		for( Alert alert : service.getAlertManager().getAlerts() ) {
//			alertArray.add( jsonifyAlert( alert, operationContext.getResponseTarget(), operationContext.getResponseDetails() ) );
//		}
		serviceObject.add( "alerts", alertArray );
		
		ResponseHelper.writeSuccess(theRequest, theResponse, serviceObject );
//		}
	}

	/**
	 * Helper method that generates a json object for the alert.
	 * @param theAlert the alert to jsonify
	 * @return the jsonified alert
	 */
	private JsonObject jsonifyAlert( Alert theAlert, Readability theReadability, Details theDetails ) {
		//Map< Class<?>, Translator> targetTranslators = theReadability == Readability.HUMAN ? humanTranslators : machineTranslators;
		JsonObject alertObject = new JsonObject( );

		alertObject.addProperty( "name", theAlert.getName( ) );		
	
		return alertObject;
	}
}
