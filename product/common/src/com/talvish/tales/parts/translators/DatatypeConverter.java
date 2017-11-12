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
package com.talvish.tales.parts.translators;

import com.google.common.io.BaseEncoding;

/**
 * Helper class since Java 9 removed the javax.xml.bind.DatatypeConverter. 
 * This abstracts the implementation so different versions can be tried.
 * @author jmolnar
 *
 */
public final class DatatypeConverter {
	// NOTE: this originally used the xml bind DatatypeConverter, we have options we could use directly such as
	//       https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java/11139098#11139098

	
	/**
	 * Takes a hex string and returns a byte array.
	 * @param theString the string to parse
	 * @return the byte array
	 */
	public static byte[] parseHexBinary( String theString ) {
		return BaseEncoding.base16( ).decode( theString );
	}
	
	/**
	 * Takes a byte array and returns a hex string.
	 * @param theData the byte array to convert
	 * @return the hex string representation of the byte array
	 */
	public static String printHexBinary( byte[] theData ) {
		return BaseEncoding.base16( ).encode( theData );
	}
}
