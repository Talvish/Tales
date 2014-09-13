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
package com.tales.contracts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class represents a version of a particular contract.
 * @author jmolnar
 *
 */
public class ContractVersion {
	private static final String BASE_VERSION_REGEX = "20\\d\\d[01]\\d[0-3]\\d"; // an approximation of a date in the years 20xx
	/**
	 * The regular expression used to find a contract version.
	 */
	public static final String VERSION_REGEX = "(" + BASE_VERSION_REGEX + ")"; 
	
	/**
	 * The compiled regular expression used to find a contract version.
	 */
	public static final Pattern VERSION_PATTERN = Pattern.compile( VERSION_REGEX );

	private static final String ONLY_VERSION_REGEX = String.format( "^%s$", VERSION_REGEX );
	private static final Pattern ONLY_VERSION_PATTERN = Pattern.compile( ONLY_VERSION_REGEX );

	/**
	 * The regular expression used to find a range of contract versions.
	 */
	public static final String VERSION_RANGE_REGEX = "(" + BASE_VERSION_REGEX + ")?(\\s*-\\s*(" + BASE_VERSION_REGEX + ")?)?";
	/**
	 * The compiled regular expression used to find a range of contract versions.
	 */
	public static final Pattern VERSION_RANGE_PATTERN = Pattern.compile( VERSION_RANGE_REGEX );

	private static final String ONLY_VERSION_RANGE_REGEX = String.format( "^%s$", VERSION_RANGE_REGEX );
	private static final Pattern ONLY_VERSION_RANGE_PATTERN = Pattern.compile( ONLY_VERSION_RANGE_REGEX );

	
	private static final SimpleDateFormat dateformatter = new SimpleDateFormat( "yyyyMMdd" );

	private final String versionString;	
	private final Date versionDate;

	/**
	 * Simple helper method that verifies the version is correctly formatted.
	 * @param theVersion the version string to verify
	 * @return returns true if valid, false if not (including if null or empty)
	 */
	public static boolean isValidVersion( String theVersion ) {
		boolean isValid = false;
		
		if( !Strings.isNullOrEmpty( theVersion ) ) {
			Matcher matcher = ONLY_VERSION_PATTERN.matcher( theVersion );
			if( matcher.matches( ) ) {
				isValid = true;
			}
		}
		return isValid;
	}
	
	/**
	 * Constructs a version string from the data and revision.
	 * @param theVersionDate the data of the contract
	 */
	public static String generateString( Date theVersionDate ) {
		Preconditions.checkNotNull( theVersionDate, "must provide a date" );
		
		String version = dateformatter.format( theVersionDate );
		return version;
	}

	/**
	 * A helper method that will turn collection of string versions 
	 * into a map of ContractVersion objects.
	 * @param theVersions the versions to generate into proper ContractVersion objects.
	 * @return the map of ContractVersion objects
	 */
	public static Map<String, ContractVersion> generateVersions( String[] theVersions ) {
		Preconditions.checkNotNull( theVersions, "must have version strings to turn into ContractVersion objects" );
		// get the versions extracted 
		Map<String, ContractVersion> versionMap = new TreeMap<String,ContractVersion>( );		
		ContractVersion version;
		
		for( String stringVersion : theVersions ) {
			version = new ContractVersion( stringVersion );
			versionMap.put( version.getVersionString( ), version );
		}	
		
		return versionMap;
	}

	/**
	 * A helper method that will turn collection of string versions 
	 * into a map of ContractVersion objects.
	 * @param theVersions the versions to generate into proper ContractVersion objects.
	 * @return the map of ContractVersion objects
	 */
	public static Map<String, ContractVersion> generateVersions( Collection<ContractVersion> theVersions ) {
		Preconditions.checkNotNull( theVersions, "must have version strings to turn into ContractVersion objects" );
		// get the versions extracted 
		Map<String, ContractVersion> versionMap = new TreeMap<String,ContractVersion>( );		
		
		for( ContractVersion version : theVersions ) {
			versionMap.put( version.getVersionString( ), version );
		}	
		
		return versionMap;
	}

	/**
	 * A helper method that will turn collection of string versions 
	 * into a map of ContractVersion objects.
	 * @param theVersions the versions to generate into proper ContractVersion objects.
	 * @return the map of ContractVersion objects
	 */
	public static Map<String, ContractVersion> generateVersions( String[] theVersions, Contract theParent ) {
		Preconditions.checkNotNull( theVersions, "must have version strings to turn into ContractVersion objects" );
		Preconditions.checkNotNull( theParent, "must have a parent to turn the versions into ContractVersion objects" );

		Map<String,ContractVersion> versionMap = new TreeMap<String, ContractVersion>( );

		for( String stringVersion : theVersions ) {
			Matcher matcher = ONLY_VERSION_RANGE_PATTERN.matcher( stringVersion );
			
			if( !matcher.matches() ) {
				throw new IllegalArgumentException( String.format( "Version string '%s' is not valid.", stringVersion ) );
			}
			ContractVersion startVersion = null;
			ContractVersion endVersion = null;
			Collection<ContractVersion> parentVersions = theParent.getSupportedVersions();
			
			String startStringVersion = matcher.group( 1 );
			if( !Strings.isNullOrEmpty( startStringVersion ) ) {
				startVersion = new ContractVersion( startStringVersion );
				if( !theParent.supports( startVersion ) ) {
					throw new IllegalArgumentException( String.format( "Start version string '%s' is not supported by the parent.", startStringVersion ) );
				}
			}

			if( matcher.groupCount() == 3 ) {
				String endStringVersion = matcher.group( 3 );
				if( !Strings.isNullOrEmpty( endStringVersion ) ) {
					// we have an end string
					endVersion = new ContractVersion( endStringVersion );
					if( !theParent.supports( endVersion ) ) {
						throw new IllegalArgumentException( String.format( "End version string '%s' is not supported by the parent.", endStringVersion ) );
					}
				} else if( Strings.isNullOrEmpty( matcher.group( 2 ) ) ) {
					// if we don't have an end string nor a range indicator, 
					// then start and end are the same value
					endVersion = startVersion;
				}
			}
			
			// make sure, the start version is not greater than the end version
			if( startVersion != null && endVersion != null && startVersion.isAfter( endVersion ) ) {
				throw new IllegalArgumentException( String.format( "Start version '%s' is later than end version '%s' in version string '%s'.", startVersion.getVersionString(), endVersion.getVersionString( ), stringVersion ) );
			}
			
			int added = 0;
			for( ContractVersion parentVersion : parentVersions ) {
				// we now check to see if the version being compare will fit within the start,end range
				// if so, then we add it to the list of supported versions
				if( ( startVersion == null || parentVersion.compareTo( startVersion ) >= 0 ) && ( endVersion == null || parentVersion.compareTo( endVersion ) <= 0 ) ) {
					versionMap.put( parentVersion.versionString, parentVersion );
					added += 1;
				}
			}
			if( added == 0 ) {
				throw new IllegalArgumentException( String.format( "None of the versions in '%s' were found in the parent.", stringVersion ) );
			}
		}	
		
		return versionMap;
	}
	
	/**
	 * Constructs a version from a string. The string must 
	 * match the regular expression: ^(20\d\d[01]\d[0-3]\d)(\.\d?\d)?$
	 * @param theString the version string
	 */
	public ContractVersion( String theString ) {
		// first we make sure we have a proper string
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theString ), "the version cannot be null/missing" );
		Matcher matcher = ONLY_VERSION_PATTERN.matcher( theString );
		Preconditions.checkArgument( matcher.matches( ), "the version string does not follow the format yyyyMMdd" );

		String stringDate = matcher.group( 1 );
		
		// next we make sure we have a proper date
		try {
			versionDate = dateformatter.parse( stringDate );
		} catch( ParseException e ) {
			throw new IllegalArgumentException( "the version could not be parsed", e );
		}
		Preconditions.checkArgument( versionDate != null, "the version could not be parsed" );
		
		// finally we store the string, but regenerate based on our rules
		versionString = generateString( versionDate );
		
	}
	
	/**
	 * Constructor that takes the date and revision information.
	 * @param theVersionDate
	 * @param theRevision
	 */
	public ContractVersion( Date theVersionDate ) {
		versionString = generateString( theVersionDate );
		versionDate = theVersionDate;
	}
	
	/**
	 * The full version string for the contract.
	 * @return the full version string
	 */
	public String getVersionString( ) {
		return versionString;
	}
	
	/**
	 * The version date of the build.
	 * @return The version date of the build.
	 */
	public Date getVersionDate( ) {
		return versionDate;
	}
	
	/**
	 * Implementation of equals method, which ensures the values are the same.
	 */
	@Override
	public boolean equals(Object theObject ) {
		if( theObject == null || theObject.getClass().equals( this.getClass( ) ) ) {
			return false;
		} else {
			return this.versionDate.equals( ( ( ContractVersion )theObject ).versionDate );
		}
	}

	/**
	 * Helper method for determining how two versions compare.
	 * If the current version is less than the parameter/other version then -1 is returned.
	 * If the current version is greater than the parameter/other version then +1 is returned.
	 * If the two are equal then 0 is returned.
	 * @param theOtherVersion the version to compare against
	 */
	public int compareTo( ContractVersion theOtherVersion ) {
		return this.versionDate.compareTo( theOtherVersion.versionDate );
	}
	
	/**
	 * Helper method to check if one contract version comes after another.
	 * @param theOtherVersion the version to compare against.
	 * @return true if the current version is after the parameter
	 */
	public boolean isAfter( ContractVersion theOtherVersion ) {
		return this.versionDate.after( theOtherVersion.versionDate );
	}
	
	/**
	 * Helper method to check if one contract version comes before another.
	 * @param theOtherVersion the version to compare against
	 * @return true if the current version is before the parameter
	 */
	public boolean isBefore( ContractVersion theOtherVersion ) {
		return this.versionDate.before( theOtherVersion.versionDate );
	}
	
	/**
	 * Implementation of the hashcode method.
	 */
	@Override
	public int hashCode() {
		return this.versionDate.hashCode();
	}
	
	/**
	 * Returns the version string representation.
	 */
	@Override
	public String toString() {
		return versionString;
	}
}
