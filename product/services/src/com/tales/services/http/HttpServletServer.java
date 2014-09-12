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
package com.tales.services.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.tales.serialization.Readability;
import com.tales.services.OperationContext;
import com.tales.services.OperationContext.Details;
import com.tales.system.Status;

// NOTE: Given the overridden servlet holder to manage
//       the error handling, perhaps we could move this
//       functionality to the holder as well

/**
 * Class that overrides the Jetty Server to create 
 * OperationContext when requests are processed.
 * @author jmolnar
 */
class HttpServletServer extends Server {
    private static final Logger logger = LoggerFactory.getLogger( HttpServletServer.class ); // log against the id, so we can group up from anywhere
	
	private Details defaultDetails			= Details.NORMAL;
	private Readability defaultReadability	= Readability.MACHINE;
	
	private final HttpInterfaceBase boundInterface;
	
	/**
	 * The constructor taking the interface the servlet is bound to.
	 * @param theBoundInterface the interface this instance is bound to
	 */
	public HttpServletServer( HttpInterfaceBase theBoundInterface ) {
		Preconditions.checkNotNull( theBoundInterface, "must have an interface" );
		boundInterface = theBoundInterface;
	}
	
	/**
	 * Sets the default level used for showing details in responses.
	 * @param theDetails the new default level
	 */
	public void setDefaultResponseDetails( Details theDetails ) {
		this.defaultDetails = theDetails;
	}

	/**
	 * Sets the default target for readability in responses.
	 * @param theReadability the new default target
	 */
	public void setDefaultResponseReadability( Readability theReadability ) {
		this.defaultReadability = theReadability;
	}

	/**
	 * Override the handle method to create an OperationContext and safe
	 * that context in the request attributes.
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException ,ServletException {
		
		HttpRequestWrapper requestWrapper = new HttpRequestWrapper(request);
		
		// extract a parameter about how to show the response
		String stringReadability = requestWrapper.getParameter( ParameterConstants.OVERRIDE_RESPONSE_READABILITY );
		String stringDetails = requestWrapper.getParameter( ParameterConstants.OVERRIDE_RESPONSE_DETAILS );
		Readability readability = defaultReadability;
		Details details = defaultDetails;
		
		if( stringReadability != null ) {
			try {
				readability = Readability.valueOf( stringReadability );
				
			} catch( Exception e ) {
				// ignore the bad param
			}
		}

		if( stringDetails != null ) {
			try {
				details = Details.valueOf( stringDetails );
			} catch( Exception e ) {
				// ignore the bad param
			}
		}

		OperationContext operationContext = new OperationContext(
				requestWrapper.getHeader( HeaderConstants.ROOT_REQUEST_ID_HEADER ), 
				requestWrapper.getHeader( HeaderConstants.PARENT_REQUEST_ID_HEADER ), 
				readability,
				details );

		// TODO: consider storing the remote IP OR modifying the referrer/agent so we can track where it came from
		// TODO can we be explicit about this in the Wrapper and not use attributes ?
		requestWrapper.setAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT, operationContext );
		logger.info( 
				"Request received with the associated operation context information\n\troot request id = {}\n\tcalling request id = {}\n\tcurrent request id = {}", new Object[]{ 
				operationContext.getRootRequestId(),
				operationContext.getParentRequestId(),
				operationContext.getCurrentRequestId() } );
		super.handle(target, baseRequest, requestWrapper, response);
		// error handling here isn't possible (try/catch around super.handle) since 
		// jetty traps the exception prior to it coming back here

		
		// the following code is a safety net should the nothing be able to service the request
		// ideally this is called consistently, but can depend on the default servlet handling
		if( !baseRequest.isHandled() ) {
			ResponseHelper.writeFailure(requestWrapper, response, Status.CALLER_NOT_FOUND, FailureSubcodes.UNKNOWN_REQUEST, String.format( "Path '%s' has not been assigned a context.", request.getRequestURL().toString( ) ), null );
			this.boundInterface.getStatus().recordBadUrl();
			baseRequest.setHandled( true );
		}
	}

//	/**
//	 * Reconstructs the referrer URL from the requst.
//	 * @param theRequest the http servlet request to read from
//	 * @return the referrer URL
//	 */
//	private String getCurrentUrl( HttpServletRequest theRequest ) {
//		// TODO: this isnt' called anymore, but maybe useful to hold in a utility somewhere
//		String queryString = theRequest.getQueryString();
//		String returnValue = theRequest.getRequestURL().toString();
//		
//		if( !Strings.isNullOrEmpty( queryString ) ) {
//			returnValue = String.format( "%1$s?%2$s", returnValue, queryString );
//		}
//		return returnValue;
//	}
}