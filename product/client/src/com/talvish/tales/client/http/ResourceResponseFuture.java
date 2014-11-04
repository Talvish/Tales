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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.talvish.tales.communication.CommunicationException;
import com.talvish.tales.communication.HeaderConstants;

/**
 * A future for use in doing async requests to a Tales service. This 
 * implementation is based on the Jetty FutureResponseListener class 
 * and extends the BufferingResponseListener to process the service 
 * result and turn it into the ResourceResult aspected by clients 
 * @author jmolnar
 *
 * @param <T> the type of data that is part of the ResourceResult response
 */
public class ResourceResponseFuture<T> extends BufferingResponseListener implements Future<ResourceResult<T>> {
	private static final Logger logger = LoggerFactory.getLogger( ResourceRequest.class ); // going to leave this on the resource request

    private final CountDownLatch latch = new CountDownLatch(1);
    private final ResourceRequest request;
    private final boolean requestedAsync;
	private ResourceResult<T> objectResult;

    private Throwable failure;
    private volatile boolean cancelled;

	private long startTimestamp;

	/**
	 * Constructor for use by the ResourceRequest, to setup the Future/Listener.
	 * This defaults to 2 megabyte for maximum size of the response.
	 * @param theRequest all the request information
	 * @param requestedAsync indicates if the original caller wanted sync or async operation (which only impacts log entries)
	 */
    protected ResourceResponseFuture( ResourceRequest theRequest, boolean requestedAsync ) {
        this( theRequest, 2 * 1024 * 1024, requestedAsync );        
    }

	/**
	 * Constructor for use by the ResourceRequest, to setup the Future/Listener.
	 * @param theRequest all the request information
	 * @param maxLength the maximum length of the content that will be accepted
	 * @param requestedAsync indicates if the original caller wanted sync or async operation (which only impacts log entries)
	 */
    protected ResourceResponseFuture( ResourceRequest theRequest, int theMaxLength, boolean requestedAsync ) {
        super( theMaxLength );
        Preconditions.checkNotNull( theRequest, "the future needs a request to watch" );
        this.request = theRequest;
        this.requestedAsync = requestedAsync;
        startTimestamp = System.nanoTime(); 
    }

    /**
     * The method that process the response from the Tales service (or errors).
     */
    @SuppressWarnings("unchecked")
	@Override
    public void onComplete(Result result) {
    	// first, let's see what failures we had during the work
    	// (which, if we had, will impact the how we do error 
    	// handling below)
        failure = result.getFailure();
        
        // second, we do into the attempt to parse out
        // content that may or may not have come back
        // from the server
		String responseString = this.getContentAsString();
		if( responseString != null ) {
			try {
				logger.trace( "Service return '{}'.", responseString );
	
				// grab the response as a string, it should all be json, so let's interpret
				JsonElement jsonResult = request.getClient( ).jsonParser.parse( responseString );
				// now we need to convert what was returned as a result object ... BUT ..
				objectResult = ( ResourceResult<T> )request.getClient( ).getResultType().getFromJsonTranslator().translate( jsonResult );
				// the actual result is not interpreted since we don't the type at registration time so we deal with the result
				// value separately
				JsonElement jsonReturn = jsonResult.getAsJsonObject().get( "return" );
				// we need to make sure we have a result and if not then we 
				// assume we didn't get a response
				if( jsonReturn != null )  {
					objectResult.setResult( ( T )request.getClient( ).getJsonFacility().fromJsonElement( jsonReturn, request.getMethod( ).getReturn().getType() ) );
				} else {
					objectResult.setResult( null );
				}
				
				// TODO: need to do cookie support
				HttpFields headers = result.getResponse().getHeaders();
				String headerName;
				String headerValue;
				
				for( HttpField header : headers ) {
					headerName = header.getName( );
					headerValue = header.getValue( );
					if( objectResult.getHeaders().containsKey( headerName ) ) {
						// we shouldn't have two of the same headers, but if we do, at least warn
						logger.warn( 
								"Duplicate header '{}' found while processing result from resource method '{}' from contract '{}'.", 
								headerName,
								request.getMethod().getName(),
								request.getClient().contractRoot );
					} else {
						objectResult.setHeader( headerName, headerValue );
						switch( headerName ) {
						case HeaderConstants.CACHE_CONTROL:
							objectResult.setCacheControl( CacheControl.create( headerValue ) );
							break;
						default:
							// nothing else to do yet
							break;
						}
					}
				}
			} catch( Exception e ) {
				failure = e;
				
			} finally {
				// status block handling would go here
				long executionTime = System.nanoTime( ) - startTimestamp;
				logger.info( 
						"Executed, {}, resource method '{}' from contract '{}' in {} ms.", new Object[] {
								this.requestedAsync ? "asynchronously" : "synchronously",
								request.getMethod( ).getName(),
								request.getClient( ).contractRoot,
								( ( double )executionTime ) * 0.000001 } );
			}
			
		} else if( failure == null ) {
			// this means we don't have a failure recorded
			// but we also don't have any data, which really 
			// shouldn't be possible
			failure = new CommunicationException( String.format( "Have an emtpy result without a recorded failure from '%s'.", this.request.getMethod( ).getMethodUrl( ) ) );
		} // else failure is set, so nothing to do
        
		// third, we now mark that the result
		// is available for use.
        latch.countDown();
    }

    /**
     * Request to cancel the operation.
     * @param mayInterrupt is not used
     * @return returns if the cancel was successful or not
     */
    @Override
    public boolean cancel( boolean mayInterrupt ) {
        cancelled = true;
        return this.request.getRequest( ).abort( new CancellationException( ) );
    }

    /**
     * Indicates if this future was cancelled.
     * @return returns if this future was cancelled
     */
    @Override
    public boolean isCancelled( ) {
        return cancelled;
    }

    /**
     * Indicates if the future is done processing, regardless of whether
     * it was successful or not.
     * @return returns if the future is done 
     */
    @Override
    public boolean isDone( ) {
        return latch.getCount( ) == 0 || isCancelled( );
    }

    /**
     * Gets, waiting if needed, the result from the service.
     * @return the result from the service.
     * @throws ExecutionException thrown if an exception occurred while processing the request
     * @throws InterruptedException thrown if the thread running this was interrupted
     */
    @Override
    public ResourceResult<T> get() throws InterruptedException, ExecutionException {
        latch.await() ; // blocking call if the latch hasn't gone down
        return _get();
    }

    /**
     * Gets, waiting up to the specified time if needed, the result from the service.
     * @param theTimeout how long to wait (based on the units)
     * @param theUnit the unit of time to use for theTimeout
     * @return the result from the service.
     * @throws ExecutionException thrown if an exception occurred while processing the request
     * @throws InterruptedException thrown if the thread running this was interrupted
     * @throws TimeoutException thrown if the timeout period expires and a result isn't available yet
     */
    @Override
    public ResourceResult<T> get( long theTimeout, TimeUnit theUnit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean expired = !latch.await( theTimeout, theUnit );
        if( expired ) {
            throw new TimeoutException();
        } else {
        	return _get();
        }
    }

    /**
     * The underlying implementation of the get requests, which 
     * will throw exceptions if cancelled or exceptions had 
     * occurred earlier, otherwise returns the result from the
     * service
     * @return the result from the service
     * @throws ExecutionException an exception that encapsulates errors that occurred while running the request
     */
    private ResourceResult<T> _get( ) throws ExecutionException {
        if( isCancelled ( ) ) {
        	// if cancelled, we throw the cancellation exception with a potential related cause, if any
            throw ( CancellationException )new CancellationException ().initCause( failure );
        } else if (failure != null) {
        	// if not cancelled, but failed, we throw the execution exception with the original failure reason
            throw new ExecutionException( failure );
        } else {
        	// otherwise, we give our result
        	return objectResult;
        }
    }
}
