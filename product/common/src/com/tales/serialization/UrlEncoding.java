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
package com.tales.serialization;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.google.common.base.Strings;

/**
 * A simple class with common string methods.
 * @author jmolnar
 */
public final class UrlEncoding {
    /**
     * URL encodes a string and hides the needed to handle an exception that should never happen.
     */
    public static String encode( String theString ) {
    	return encode( "UTF-8", theString );
    }

    /**
     * URL encodes a string and hides the needed to handle an exception that should never happen.
     */
    public static String encode( String theEncoding, String theString ) {
        String encodedString = null;

        if( Strings.isNullOrEmpty( theEncoding ) ) {
    		theEncoding = "UTF-8";
    	}
        try {
            encodedString = URLEncoder.encode(theString, theEncoding );
        } catch( UnsupportedEncodingException e ) {
            throw new RuntimeException( String.format( "Didn't like '%s' encoding", theEncoding ), e );
        }
        return encodedString;
    }

    /**
     * URL encodes a string and hides the needed to handle an exception that should never happen.
     */
    public static String decode( String theString ) {
    	return decode( "UTF-8", theString );
    }

    /**
     * URL encodes a string and hides the needed to handle an exception that should never happen.
     */
    public static String decode( String theEncoding, String theString ) {
        String decodedString = null;

        if( Strings.isNullOrEmpty( theEncoding ) ) {
    		theEncoding = "UTF-8";
    	}
        try {
        	decodedString = URLDecoder.decode(theString, theEncoding );
        } catch( UnsupportedEncodingException e ) {
            throw new RuntimeException( String.format( "Didn't like '%s' encoding", theEncoding ), e );
        }
        return decodedString;
    }
}
