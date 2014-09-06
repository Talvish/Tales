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
package com.tales.system.build;
//NOTE: not implemented (Java doesn't support cleanly)
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import com.google.common.base.Preconditions;
//import com.google.common.base.Strings;
//
///**
// * Class representing the internal build information of a resource such as a file.
// * @author jmolnar
// *
// */
public final class InternalBuild {
//	private static SimpleDateFormat dateformatter = new SimpleDateFormat( "yyyyMMdd" );
//	
//	private String buildString;
//	private String resourceName;
//	private Date versionDate;
//	private int revision;
//	
//	/**
//	 * Method taking the string that is formatted such as 'FILENAME.YYMMDD.REVISION'.
//	 * @param theBuildString A string of the format 'FILENAME.YYMMDD.REVISION'.
//	 */
//	public static InternalBuild generateBuild( String theBuildString ) {
//		Preconditions.checkArgument( !Strings.isNullOrEmpty( theBuildString ) );
//		return null;
//	}
//	
//	/**
//	 * Generates the build object from the component pieces.
//	 * @param theResourceName The name of the file or other resource
//	 * @param theVersionDate The version date of the build
//	 * @param theRevision The revision number of the build.
//	 */
//	public static InternalBuild generateBuild( String theResourceName, Date theVersionDate, int theRevision ) {
//		return new InternalBuild( 
//				generateString( theResourceName, theVersionDate, theRevision ),
//				theResourceName, 
//				theVersionDate, 
//				theRevision );
//	}
//
//	/**
//	 * Creates a string from the components of a build.
//	 * @param theResourceName The name of the file or other resource
//	 * @param theVersionDate The version date of the build
//	 * @param theRevision The revision number of the build.
//	 */
//	public static String generateString( String theResourceName, Date theVersionDate, int theRevision ) {
//		Preconditions.checkArgument( !Strings.isNullOrEmpty( theResourceName ) );
//		Preconditions.checkNotNull( theVersionDate );
//		Preconditions.checkArgument( theRevision >= 0 );
//		
//		return theResourceName + "." +  dateformatter.format( theVersionDate ) + "." + theRevision;
//	}
//	
//	/**
//	 * Private constructor used by static methods.
//	 * @param theBuildString A string of the format 'FILENAME.YYMMDD.REVISION'.
//	 * @param theResourceName The name of the file/resource.
//	 * @param theVersionDate The version date of the build.
//	 * @param theRevision The revision number of the build.
//	 */
//	private InternalBuild( String theBuildString, String theResourceName, Date theVersionDate, int theRevision ) {
//		this.buildString = theBuildString;
//		this.resourceName = theResourceName;
//		this.versionDate = theVersionDate;
//		this.revision = theRevision;
//	}
//	
//	/**
//	 * The name of the file/resource.
//	 * @return The name of the file/resource.
//	 */
//	public String getResourceName( ) {
//		return resourceName;
//	}
//	
//	/**
//	 * Returns the raw build string.
//	 * @return the raw build string
//	 */
//	public String getBuildString( ) {
//		return buildString;
//	}
//	
//	/**
//	 * The version date of the build.
//	 * @return The version date of the build.
//	 */
//	public Date getVersionDate( ) {
//		return versionDate;
//	}
//	
//	/**
//	 * The revision number of the build. This represents, effectively
//	 * the number of build changes in a given day.
//	 * @return The revision number of the build.
//	 */
//	public int getRevision( ) {
//		return revision;
//	}
//	
//	/**
//	 * Returns the full build string formatted as 'FILENAME.YYMMDD.REVISION'.
//	 */
//	@Override
//	public String toString() {
//		return buildString;
//	}
}
