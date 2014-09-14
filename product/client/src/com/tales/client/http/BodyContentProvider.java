package com.tales.client.http;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.jetty.client.util.StringContentProvider;


import com.tales.serialization.UrlEncoding;

/**
 * This is a Content Provider for Jetty client requests. It takes a set of 
 * parameters, makes sure they are encoded for use in a POST body with
 * content type 'application/x-www-form-urlencoded'.
 * @author jmolnar
 *
 */
public class BodyContentProvider extends StringContentProvider {

	/**
	 * The constructor taking the parameters to encode.
	 * @param theParameters the parameters to encode
	 */
	public BodyContentProvider( Map<String,String> theParameters) {
		super( "application/x-www-form-urlencoded", encode( theParameters ), StandardCharsets.UTF_8 );
	}
	

	/**
	 * A helper method that takes the parameters and creates the form-style
	 * string for the POST body.
	 * @param theParameters the parameters to encode
	 * @return the string containing the encoded parameters
	 */
	public static String encode( Map<String,String> theParameters ) {
        StringBuilder content = new StringBuilder( theParameters.size( ) * 20 * 2 );
        
        for( Map.Entry<String,String> parameter : theParameters.entrySet() ) {
            if( content.length() > 0 ) {
            	content.append( '&' );
            }
            content.append( UrlEncoding.encode( parameter.getKey( ) ) );
            content.append( '=' );
            content.append( UrlEncoding.encode( parameter.getValue( ) ) );
        }
        return content.toString();
	}

}
