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







import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.talvish.tales.communication.Status;
import com.talvish.tales.contracts.services.http.ServletContract;
import com.talvish.tales.services.http.FailureSubcodes;
import com.talvish.tales.services.http.ResponseHelper;


/**
 * This is a simple servlet that allows killing the service.
 * @author jmolnar
 *
 */
@ServletContract( name="com.tales.services.control", versions="20111005")
@SuppressWarnings("serial")
public class ControlServlet extends AdministrationServlet {
	private static final String KILL	= "/kill";
	private static final String STOP	= "/stop";
	private static final String SUSPEND	= "/suspend";
	private static final String RESUME	= "/resume";
	
	private static final String ALREADY_SUSPENED = "ALREADY_SUSPENDED";
	private static final String ALREADY_RUNNING = "ALREADY_RUNNING";
	
    private static final Logger logger = LoggerFactory.getLogger( ControlServlet.class );

    /**
     * Empty, default constructor.
     */
    public ControlServlet( ) {
    }
    
	/**
	 * Implementation of the get method to request closing the service..
	 */
	@Override
	protected void doGet(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}

	/**
	 * Implementation of the post method to request closing the service..
	 */
	@Override
	protected void doPost(HttpServletRequest theRequest, HttpServletResponse theResponse ) throws ServletException, IOException {
		doCall( theRequest, theResponse );
   	}
	
	/**
	 * Private, shared, implementation of the method used to shutdown the service.
	 */ 
	private void doCall( HttpServletRequest theRequest, HttpServletResponse theResponse ) {
		String operation = theRequest.getPathInfo( );

		if( Strings.isNullOrEmpty( operation ) ) {
			ResponseHelper.writeFailure( 
				theRequest,
				theResponse, 
				Status.CALLER_BAD_INPUT,
				FailureSubcodes.UNKNOWN_REQUEST,
				"Missing the operation.",
				null );
		} else {
		//		if( theRequest.getParameterMap().size() > 1 ) {
//			HttpService.writeFailure( 
//					theRequest,
//					theResponse, 
//					Failure.CALLER_BAD_DATA, 
//					"0 or 1 parameters are expected.",
//					null );
//		} else {
		
			if( operation.equals( STOP ) ) {
				close( );
				ResponseHelper.writeSuccess( theRequest, theResponse );
			} else if( operation.equals( KILL ) ) {
				ResponseHelper.writeSuccess( theRequest, theResponse );
				kill( );
			} else if( operation.equals( SUSPEND ) ) {
//				String period = theRequest.getParameter( "period" );
//				Integer integerPeriod = null;
//				DateTime dateTimePeriod = null;
//				
//				if( !Strings.isNullOrEmpty( period ) ) {
//					try {
//						integerPeriod = ( Integer )integerTranslator.translate( period );
//						dateTimePeriod = ( DateTime )dateTimeTranslator.translate( period );
//					} catch( TranslationException e) {
//						
//					}
//				}
				if( suspend( ) ) {
					ResponseHelper.writeSuccess( theRequest, theResponse );
				} else {
					ResponseHelper.writeFailure(
							theRequest,
							theResponse, 
							Status.CALLER_BAD_STATE,
							ALREADY_SUSPENED,
							"Not in a state to be able to suspend.",
							null );
				}
			} else if( operation.equals( RESUME ) ) {
				if( resume( ) ) {
					ResponseHelper.writeSuccess( theRequest, theResponse );
				} else {
					ResponseHelper.writeFailure(
							theRequest,
							theResponse,
							Status.CALLER_BAD_STATE,
							ALREADY_RUNNING,
							"Not in a state to be able to resume.",
							null );
				}
			} else {
				logger.info( "Unknown operation '{}'.", operation );
				ResponseHelper.writeFailure(
						theRequest,
						theResponse, 
						Status.CALLER_BAD_INPUT, 
						FailureSubcodes.UNKNOWN_REQUEST,
						String.format( "The operation '%s' is not valid.", operation ),
						null );
			}
		}
	}
	
	/**
	 * Method called to stop the service gracefully.
	 */
	private void close( ) {
		logger.info( "Request made to stop the service." );
		getService( ).signalStop();
	}
	
	/**
	 * Method called to stop the service ungracefully/instantly.
	 */
	private void kill( ) {
		logger.info( "Request made to KILL the service." );
		getService( ).signalKill();
	}
	
	/**
	 * Method called to suspend the service.
	 * @param theSuspendLength the length of time to suspend for
	 * @return Returns true if suspend was called, false if it was not.
	 */
	private boolean suspend( /*Integer theSuspendLength*/ ) {
		logger.info( "Request made to suspend the main interface." );
		if( getService( ).getInterfaceManager().canSuspend() ) {
			getService( ).getInterfaceManager().suspend( );
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Method called to resume a suspend service.
	 * @return Returns true if resume was called, false if it wsa not.
	 */
	private boolean resume( ) {
		logger.info( "Request made to resume the main interface." );
		if( getService( ).getInterfaceManager().canResume() ) {
			getService( ).getInterfaceManager().resume( );
			return true;
		} else {
			return false;
		}
	}
}
