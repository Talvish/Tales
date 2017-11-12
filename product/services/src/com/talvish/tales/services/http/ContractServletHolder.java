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
package com.talvish.tales.services.http;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.talvish.tales.communication.HttpStatus;
import com.talvish.tales.communication.Status;
import com.talvish.tales.contracts.services.http.HttpContract;
import com.talvish.tales.services.OperationContext;
import com.talvish.tales.system.ExecutionLifecycleState;

/**
 * This is a servlet holder that has a contract
 * associated with a servlet and has a mechanism
 * to allow subclasses to filter based on versioned
 * requestes.
 * @author jmolnar
 *
 */
public abstract class ContractServletHolder extends ServletHolder {
	private static final Logger logger = LoggerFactory.getLogger( ContractServletHolder.class );
	private final HttpContract	contract;
	private final HttpInterfaceBase httpInterface;
	
	/**
	 * This is a simple async listener to attempt to make sure we track
	 * the success and failures of async operations.
	 */
	private final AsyncListener asyncListener = new AsyncListener() {
		@Override
		public void onTimeout(AsyncEvent theEvent) throws IOException {
		}
		
		@Override
		public void onStartAsync(AsyncEvent theEvent) throws IOException {
		}
		
		@Override
		public void onError(AsyncEvent theEvent) throws IOException {
		}
		
		@Override
		public void onComplete(AsyncEvent theEvent) throws IOException {
			// if we have an async operation we will want to try to track the success
			updateStatus( ( HttpServletRequest )theEvent.getSuppliedRequest(), ( HttpServletResponse )theEvent.getSuppliedResponse(), true );
		}
	};
	
	/**
	 * The constructor taking the contract and the servlet it is associated with.
	 * @param theContract the contract associated with the servlet
	 * @param theServlet the servlet the contract is to be bound to
	 * @param theInterface the interface the servlet is running on
	 */
	public ContractServletHolder( HttpContract theContract, Servlet theServlet, HttpInterfaceBase theInterface ) {
		super( theServlet ); // super() could throw a null pointer exception, but I prefer that to passing in both contract and servlet
		Preconditions.checkNotNull( theInterface, String.format( "Contract '%s' is attempting to bind to a null http interface.", theContract.getName() ) );
		contract = theContract;
		httpInterface = theInterface;
	}

	/**
	 * The HttpContract bound into this holder.
	 * @return the HttpContract bound into this holder. 
	 */
	public HttpContract getContract( ) {
		return contract;
	}
	
	/**
	 * The HttpInterface this holder is bound to.
	 * @return the HttpInterface this holder is bound to.
	 */
	public HttpInterfaceBase getInterface( ) {
		return this.httpInterface;
	}
	
	/**
	 * This is the method called to handling a request.
	 * This implementation traps exceptions to report failures
	 * in a consistent fashion and ensures that proper versions
	 * are being used and that the interface isn't suspended.
	 */
	@Override
	public void handle(
			Request theRequest, 
			ServletRequest theServletRequest,
			ServletResponse theServletResponse ) throws ServletException, UnavailableException, IOException {
		
		HttpServletRequest httpRequest = ( HttpServletRequest )theServletRequest;
		HttpServletResponse httpResponse = ( HttpServletResponse )theServletResponse;

		try {
    		// TODO:
			//   - come up with a way to manage system parameters to servlets so they don't count in the count

			// record that we had a request into the contract
			contract.getStatus().recordReceivedRequest();
			
			// NOTE: I would like to make the contract validation piece more sophisticated at some point
			//       where it could be managed by source location, something from the caller, certain 
			//       contracts are more lax, etc  ... even then it could be that a front-end could see 
			//       the missing version and at that time bind to the latest known good, so the servlets
			//       still require it, but front-end picks latest
			String version = theServletRequest.getParameter( ParameterConstants.VERSION_PARAMETER );

			if( this.httpInterface.getState( ) == ExecutionLifecycleState.SUSPENDED ) {
				ResponseHelper.writeFailure( 
						httpRequest, 
						httpResponse, 
						Status.LOCAL_UNAVAILABLE,
						FailureSubcodes.INTERFACE_SUSPENDED,
						"bound interface is currently suspended", 
						null );
				logger.warn( "Not executing an operation on contract '{}' since interface '{}' is suspended.", contract.getName(), this.httpInterface.getName( ) );
			} else if( !filterContract( httpRequest, httpResponse, version ) ) {
				// not filtered, so we can do default handling, which 
				// ultimately means let the bound servlet handle it

				// let's log some items if we have info enabled
				if( logger.isInfoEnabled( ) ) {
					logger.info( "Attempting a request for contract '{}/{}'.", this.contract.getName(), version );
					
					Enumeration<?> names = theRequest.getHeaderNames();
					
					while( names.hasMoreElements() ) {
						String name = ( String )names.nextElement();
						logger.info( "Found header '{}' with value '{}'", name, theRequest.getHeader( name ) );
					}
				}
				
				// now let the servlet bound do the work
				super.handle( theRequest, theServletRequest, theServletResponse );
			}
		} catch( Exception e ) {
			// if an exception comes in this far, then we assume we don't know the problem
			// and report it as a server error 
			ResponseHelper.writeFailure( httpRequest, httpResponse, Status.LOCAL_ERROR, FailureSubcodes.UNHANDLED_EXCEPTION, "Unknown problem occurred.", e );
			logger.error( "An error occurred while attempting to handle a request", e );
		}
		
		// now see what kind of response we are sending back so we can track
		try {
			// if we aren't async then we simply update the status
			// if we are async then we attach a listener which will update the status
			if( !theRequest.isAsyncStarted( ) ) {
				updateStatus( httpRequest, httpResponse, false );
			} else {
				theRequest.getAsyncContext().addListener( asyncListener );
			}
		} catch( Exception e ) {
			// ignore, but log
			logger.warn( "An error occurred while attempting to categorize the response type", e );
		}
	} 
	
	/**
	 * A method that can be used to filter contracts.
	 * @param theHttpRequest the request being responded to
	 * @param theHttpResponse the response we can write out to
	 * @param theVersion the version of the contract being requested, which may be null/empty
	 * @return return true if filtered/handled, false otherwise
	 */
	protected abstract boolean filterContract( HttpServletRequest theHttpRequest, HttpServletResponse theHttpResponse, String theVersion );
	
	/**
	 * Private helper method use to track the success or failure of call.
	 * This is called both for sync and async operations
	 * @param theRequest the request being responded to
	 * @param theResponse the response to track
	 * @param wasAsync indicates if the call was run async or not
	 */
	private void updateStatus( HttpServletRequest theRequest, HttpServletResponse theResponse, boolean wasAsync ) {
		int status = theResponse.getStatus( );
		
		if( !HttpStatus.isError( status ) ) {
			contract.getStatus( ).recordSuccess();
			
		} else if( HttpStatus.isClientError( status ) ) {
			contract.getStatus( ).recordClientError();
			
		} else if( HttpStatus.isDependentError( status ) ) {
			contract.getStatus( ).recordDependentError();
			
		} else if( HttpStatus.isUnavailableError( status ) ) {
			contract.getStatus( ).recordUnavailableError();
			
		} else { // presume local error for all others
			contract.getStatus( ).recordLocalError();
		}

		// now record how long it took to calculate
		final OperationContext context = ( OperationContext )theRequest.getAttribute( AttributeConstants.OPERATION_REQUEST_CONTEXT );
		final long executionTime = context.calculateElapsedTime();
		contract.getStatus().recordExecutionTime( executionTime );

		if( logger.isInfoEnabled( ) ) {
			logger.info( 
					"Processed, {}, a request for contract '{}/{}' in {} ms resulting in http status {}.", new Object[]{
					wasAsync ? "non-blocking" : "blocking",
					this.contract.getName(), 
					theRequest.getParameter( "version" ), 
					( ( double )executionTime ) * 0.000001, 
					status } );
		}
	}
}
