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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.tales.contracts.services.ServiceContract;
import com.tales.contracts.services.http.ServletContract;
import com.tales.parts.translators.BooleanToStringTranslator;
import com.tales.parts.translators.JavaTypeToLangAgnosticStringTranslator;
import com.tales.parts.translators.ObjectToStringTranslator;
import com.tales.parts.translators.PeriodToStringTranslator;
import com.tales.parts.translators.StringToStringTranslator;
import com.tales.parts.translators.Translator;
import com.tales.serialization.Readability;
import com.tales.services.Interface;
import com.tales.services.OperationContext;
import com.tales.services.Service;
import com.tales.services.OperationContext.Details;
import com.tales.services.http.AttributeConstants;
import com.tales.services.http.ResponseHelper;
import com.tales.system.status.StatusBlock;
import com.tales.system.status.StatusValue;

// NOTE: place to look for more: http://download.eclipse.org/jetty/stable-8/xref/org/eclipse/jetty/servlet/StatisticsServlet.html
/**
 * This is a simple servlet that shows the status exposed by the associated service.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.status", versions={"20111005"})
@SuppressWarnings("serial")
public class StatusServlet extends AdministrationServlet {
	private static final Logger logger = LoggerFactory.getLogger( StatusServlet.class );
	
	private static final Map< Class<?>, Translator> machineTranslators = new HashMap<Class<?>, Translator>( );
	private static final Map< Class<?>, Translator> humanTranslators = new HashMap<Class<?>, Translator>( );
	private static final Translator unknownTypeTranslator = new ObjectToStringTranslator( "" );
	private static final Translator stringTranslator = new StringToStringTranslator(false, "", "" );
	private static final JavaTypeToLangAgnosticStringTranslator typeTranslator = new JavaTypeToLangAgnosticStringTranslator( "" );
	
	static {
		// first setup the translators used for machine readability
		Translator commonTranslator = new ObjectToStringTranslator( "" );
		Translator booleanTranslator = new BooleanToStringTranslator( "" );
		Translator stringTranslator = new StringToStringTranslator( true, "", "" );
				
		machineTranslators.put( Integer.class, commonTranslator );
		machineTranslators.put( int.class, commonTranslator );

		machineTranslators.put( Long.class, commonTranslator );
		machineTranslators.put( long.class, commonTranslator );

		machineTranslators.put( Float.class, commonTranslator);
		machineTranslators.put( float.class, commonTranslator );

		machineTranslators.put( Double.class, commonTranslator );
		machineTranslators.put( double.class, commonTranslator );

		machineTranslators.put( Boolean.class, booleanTranslator );
		machineTranslators.put( boolean.class, booleanTranslator );

		machineTranslators.put( String.class, stringTranslator );
		machineTranslators.put( DateTime.class, commonTranslator );
		machineTranslators.put( Period.class, new PeriodToStringTranslator( "", null ) );
		
		// second, setup the translators used for human readability
		humanTranslators.put( Integer.class, commonTranslator );
		humanTranslators.put( int.class, commonTranslator );

		humanTranslators.put( Long.class, commonTranslator );
		humanTranslators.put( long.class, commonTranslator );

		humanTranslators.put( Float.class, commonTranslator);
		humanTranslators.put( float.class, commonTranslator );

		humanTranslators.put( Double.class, commonTranslator );
		humanTranslators.put( double.class, commonTranslator );

		humanTranslators.put( Boolean.class, booleanTranslator );
		humanTranslators.put( boolean.class, booleanTranslator );

		humanTranslators.put( String.class, stringTranslator );
		humanTranslators.put( DateTime.class, commonTranslator );
		humanTranslators.put( Period.class, new PeriodToStringTranslator( "", new PeriodFormatterBuilder()
	    .appendYears()
	    .appendSuffix( " year", " years" )
	    .appendSeparator( ", ", " and ")
	    .appendMonths()
	    .appendSuffix( " month", " months" )
	    .appendSeparator( ", ", " and ")
	    .appendDays()
	    .appendSuffix(" day", " days")
	    .appendSeparator( ", ", " and ")
	    .appendHours()
	    .appendSuffix(" hour", " hours")
	    .appendSeparator( ", ", " and ")
	    .appendMinutes()
	    .appendSuffix(" minute", " minutes")
	    .appendSeparator( ", ", " and ")
	    .appendSeconds()
	    .appendSuffix(" second", " seconds")
	    .appendSeparator( ", ", " and ")
	    .appendMillis()
	    .appendSuffix(" millisecond", " milliseconds")
	    .toFormatter() ) );
	}
    
    /**
     * Empty, default constructor.
     */
    public StatusServlet( ) {
    }
    
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
			logger.info( "Request made to list the status." );
			OperationContext operationContext = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
			JsonObject serviceObject = new JsonObject( );
			JsonArray interfacesArray = new JsonArray( );
			JsonObject interfaceObject;
			Service service = getService( );
			
			
			serviceObject.addProperty( "name", service.getCanonicalName( ) );
			JsonArray blockArray = new JsonArray( );
			
			for( StatusBlock statusBlock : service.getStatusManager().getStatusBlocks() ) {
				blockArray.add( jsonifyStatusBlock( statusBlock, operationContext.getResponseTarget(), operationContext.getResponseDetails() ) );
			}
			serviceObject.add( "status", blockArray );
			for( Interface serviceInterface : service.getInterfaceManager().getInterfaces() ) {
				interfaceObject = jsonifyInterface( serviceInterface, operationContext.getResponseTarget(), operationContext.getResponseDetails() );
				interfacesArray.add( interfaceObject );
			}
			serviceObject.add( "interfaces", interfacesArray );
			
			ResponseHelper.writeSuccess(theRequest, theResponse, serviceObject ); 
//		}
	}
	
	/**
	 * A helper method to get information from the interface and turn into a json object.
	 * @param theInterface the interface to json-ify
	 * @param theArray the array to play the json objects info
	 */
	private JsonObject jsonifyInterface( Interface theInterface, Readability theReadability, Details theDetails ) {
		JsonObject interfaceObject;
		
		// first get the interface blocks
		interfaceObject = new JsonObject( );
		interfaceObject.addProperty( "name", theInterface.getName( ) );
		interfaceObject.addProperty( "type", theInterface.getClass().getSimpleName() );
		JsonArray blockArray = new JsonArray( );
		
		for( StatusBlock statusBlock : theInterface.getStatusBlocks() ) {
			blockArray.add( jsonifyStatusBlock( statusBlock, theReadability, theDetails ) );
		}
		interfaceObject.add( "status", blockArray );
		
		
		// next get the interface's contracts' blocks
		JsonObject contractObject;
		JsonArray contractsArray = new JsonArray( );


		for( ServiceContract contract : theInterface.getBoundContracts( ) ) {
			contractObject = new JsonObject( );
			contractObject.addProperty( "name", contract.getName( ) );
			blockArray = new JsonArray( );
			
			for( StatusBlock statusBlock : contract.getStatusBlocks() ) {
				blockArray.add( jsonifyStatusBlock( statusBlock, theReadability, theDetails ) );
			}
			contractObject.add( "status", blockArray );
			contractsArray.add( contractObject );
		}
		// save all the contracts
		interfaceObject.add( "contracts", contractsArray );
		return interfaceObject;
	}
	
	/**
	 * Helper method that generates a json object for the status block.
	 * @param theStatusBlock the status block to jsonify
	 * @return the jsonified status block
	 */
	private JsonObject jsonifyStatusBlock( StatusBlock theStatusBlock, Readability theReadability, Details theDetails ) {
		Map< Class<?>, Translator> targetTranslators = theReadability == Readability.HUMAN ? humanTranslators : machineTranslators;
		JsonObject blockObject = new JsonObject( );
		JsonArray valueArray = new JsonArray( );
		JsonObject valueObject;
		Translator valueTranslator;

		blockObject.addProperty( "name", theStatusBlock.getName( ) );		
		for( StatusValue statusValue : theStatusBlock.getStatusValues() ) {
			valueObject = new JsonObject( );
			valueTranslator = targetTranslators.get( statusValue.getType( ) );
			if( valueTranslator == null ) {
				valueTranslator = unknownTypeTranslator;
			}
			valueObject.addProperty( "name", statusValue.getName( ) );
			valueObject.addProperty( "value", ( String )valueTranslator.translate( statusValue.getValue( ) ) );
			valueObject.addProperty( "type", ( String )typeTranslator.translate( statusValue.getType( ) ) );
			if( theDetails == Details.ALL ) {
				valueObject.addProperty( "description", ( String )stringTranslator.translate( statusValue.getDescription( ) ) );
			}
			valueArray.add( valueObject );
		}
		blockObject.add( "values", valueArray );	
	
		return blockObject;
	}
}
