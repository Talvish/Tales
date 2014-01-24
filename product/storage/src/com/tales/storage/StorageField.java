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
package com.tales.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.parts.reflection.ValueType;
import com.tales.parts.reflection.FieldDescriptor;
import com.tales.parts.sites.MemberSite;
import com.tales.parts.translators.Translator;
import com.tales.serialization.StringTranslationFacility;
import com.tales.serialization.TypeRegexFacility;

/**
 * This class represents a field that is meant to be serialized.
 * @author jmolnar
 */
public class StorageField extends FieldDescriptor<StorageType, StorageField> {
	
	/**
	 * An enum to represent the type of facet.
	 * @author jmolnar
	 *
	 */
	public enum FieldMode {
		/**
		 * Indicates this member is a standard stored member.
		 */
		STORED_MEMBER,
		/**
		 * Indicates this member is stored and used for optimistic concurrency handling,
		 */
		LOCK_MEMBER,
		/**
		 * Indicates this member is part of the id for the object.
		 */
		ID_MEMBER,
		/**
		 * Indicates this member points to a parent.
		 */
		FACET_PARENT,
		/**
		 * Indicates that for each member for each object is stored as separate columns.
		 */
		FACET_MEMBER,
		/**
		 * Indicates that each object is stored as a single column.
		 */
		FACET_OBJECT,
	}
	
	private static final String FIELD_REGEX = "(?:[_a-zA-Z][_a-zA-Z0-9]*)";
	private static final String OFFSET_REGEX = "\\#";
	private static final String REFERENCE_REGEX = String.format( "\\{\\s*(?:(%s|%s))\\s*\\}", FIELD_REGEX, OFFSET_REGEX );
	private static final Pattern REFERENCE_PATTERN = Pattern.compile( REFERENCE_REGEX );
	private static final String PREFIX_REGEX = String.format( "^(?:(?:[^\\{\\}])|(?:%s))*$", REFERENCE_REGEX );
	private static final Pattern PREFIX_PATTERN = Pattern.compile( PREFIX_REGEX );
	
    // TODO: figure out how we are storing data
    //       we store: collection, array, map, complex type, simple type, enum
	//       simple type is: int16, int32, int64, float32, float64, boolean, datetime, string, map, array, bytes, object
	//       if we to do it ourselves?: ordinal | type | [size] | value

	private final FieldMode fieldMode;
	
    private final String idComponentName;
    private CompoundIdField idComponentField; // this gets set after it has been analyzed
    
    private final List<FacetNameReference> facetNameReferences; // list of references to the pointed to object in the prefix name
    private final String specifiedFacetName; // the raw prefix string given
    private String facetNameRegex; // the regular expression version of the prefix name
    private StorageType facetReferenceType; // the type of object (or element type, if a collection) being pointed to

    private final StorageType parentType; 

    /**
     * Primary constructor used externally.
     * @param theName the name to give the field
     * @param theDeclaringTypeInfo the TypeInfo of the DataMemberType this field was declared in
     * @param thePersistedName the name to use during persistence
     * @param theField the Java reflection {@code Field} class 
     */
    public StorageField( 
    		String theName, 
    		ValueType<StorageType, StorageField> theFieldType, 
    		ValueType<StorageType, StorageField> theElementType, 
    		ValueType<StorageType, StorageField> theKeyType, 
    		ValueType<StorageType, StorageField> theValueType,
    		MemberSite theSite,
    		FieldMode theFieldMode,
    		StorageType theParentType,
    		String theFacetName,
    		String theIdComponentName,
    		StorageType theDeclaringType ) {
    	this( theName, 
    		  theFieldType,
    		  theElementType,
    		  theKeyType,
    		  theValueType,
    		  theSite,
    		  theFieldMode,
    		  theParentType,
    		  theFacetName,
    		  theIdComponentName,
    		  theDeclaringType, 
    		  theDeclaringType );
    }

    
    // TODO: need fields here
    
    /**
     * Private constructed used by the clone.
     * @param theName the name to give the field
     * @param theDeclaringTypeInfo the TypeInfo of the DataMemberType this field was declared in
     * @param theKey indicates whether the field is considered the primary key or not for the type
     * @param theField the Java reflection {@code Field} class 
     */
    private StorageField( 
    		String theName, 
    		ValueType<StorageType, StorageField> theFieldType, 
    		ValueType<StorageType, StorageField> theElementType, 
    		ValueType<StorageType, StorageField> theKeyType, 
    		ValueType<StorageType, StorageField> theValueType, 
    		MemberSite theFieldSite, 
    		FieldMode theFieldMode,
    		StorageType theParentType,
    		String theFacetName,
    		String theIdComponentName,
    		StorageType theDeclaringType,
    		StorageType theContainingType ) {
    	super( theName, theFieldType, theElementType, theKeyType, theValueType, theFieldSite, theDeclaringType, theContainingType );
    	Preconditions.checkArgument( 
        		( theFieldMode == FieldMode.FACET_PARENT && theParentType != null ) || ( theFieldMode != FieldMode.FACET_PARENT && theParentType == null ), 
        		String.format( "on '%s.%s', if you have an parent field, a parent type is needed, if you don't, don't specify it is a parent", theContainingType.getName(), theName ) );
        Preconditions.checkArgument( 
        		( theFieldMode == FieldMode.ID_MEMBER && !Strings.isNullOrEmpty( theIdComponentName ) ) || ( theFieldMode != FieldMode.ID_MEMBER && Strings.isNullOrEmpty( theIdComponentName ) ), 
        		String.format( "on '%s.%s', if you have an id field, an id component name is needed, if you don't, don't specify a name", theContainingType.getName(), theName ) );
        Preconditions.checkArgument( 
        		( ( theFieldMode == FieldMode.FACET_MEMBER || theFieldMode == FieldMode.FACET_OBJECT ) && !Strings.isNullOrEmpty( theFacetName ) ) || 
        		( !( theFieldMode == FieldMode.FACET_MEMBER || theFieldMode == FieldMode.FACET_OBJECT ) && Strings.isNullOrEmpty( theFacetName ) ), 
        		String.format( "on '%s.%s', if you have a facet field, a prefix is needed, if you don't, don't specify a prefix", theContainingType.getName(), theName ) );
        // TODO: technically the facet prefix is only needed IF it is a different field

        this.fieldMode = theFieldMode;
        this.parentType = theParentType;
        this.specifiedFacetName = Strings.isNullOrEmpty( theFacetName ) ? "" : theFacetName;;
        if( theFieldMode == FieldMode.FACET_MEMBER || theFieldMode == FieldMode.FACET_OBJECT ) {
        	// if a facet field then we have a prefix, so see
        	// if we have references to data members
    		Matcher prefixMatcher = PREFIX_PATTERN.matcher( specifiedFacetName );
    		if( !prefixMatcher.matches() ) {
    			throw new IllegalArgumentException( String.format( "'%s.%s' has an invalid prefix '%s'", theContainingType.getName(), theName, this.specifiedFacetName ) );
    		} else {
    	        this.facetNameReferences = new ArrayList<FacetNameReference>( );
    			prefixMatcher = REFERENCE_PATTERN.matcher( specifiedFacetName );
    			while( prefixMatcher.find( ) ) {
    				// extract the locations
    				this.facetNameReferences.add( new FacetNameReference( prefixMatcher.start(), prefixMatcher.end(), prefixMatcher.group( 1 ) ) );
    			}
    		}
        } else {
        	this.facetNameReferences = null;
        }
        this.idComponentName = theIdComponentName;
    }

    /**
     * This class is used to track reference in the facet name
     * to the members of the facet class.
     * @author jmolnar
     *
     */
    private class FacetNameReference {
    	private int startIndex = 0;
    	private int length = 0;
    	private String reference;
    	private StorageField referenceField;
    	private Translator toStringTranslator;
    	private Translator fromStringTranslator;
    	
    	public FacetNameReference( int theStartIndex, int theLength, String theRefString ) {
    		startIndex = theStartIndex;
    		length = theLength;
    		reference = theRefString;
    	}
    	
    	public int getStartIndex( ) {
    		return startIndex;
    	}
    	
    	public int getLength( ) {
    		return length;
    	}
    	
    	public String getReference( ) {
    		return reference;
    	}
    	
    	public StorageField getReferenceField( ) {
    		return referenceField;
    	}
    	
    	public void setReferencedField( StorageField theReferenceField ) {
    		referenceField = theReferenceField;
    	}
    	
    	public void setTranslators( Translator theFromStringTranslator, Translator theToStringTranslator ) {
    		Preconditions.checkNotNull( theToStringTranslator, String.format( "Need a to-string translator for name handling on field '%s'.", reference ) );
    		Preconditions.checkNotNull( theFromStringTranslator, String.format( "Need a from-string translator for name handling on field '%s'.", reference ) );
    		toStringTranslator = theToStringTranslator;
    		fromStringTranslator = theFromStringTranslator;
    	}
    	
    	public Translator getFromStringTranslator( ) {
    		return this.fromStringTranslator;
    	}
    	
    	public Translator getToStringTranslator( ) {
    		return this.toStringTranslator;
    	}
    }
    
    /**
     * Clones the existing object but specifying a different current type.
     */
    public StorageField cloneForSubclass( StorageType theCurrentTypeInfo ) {
        return new StorageField( 
        		this.getName( ),
        		this.fieldType,
        		this.elementType,
        		this.keyType,
        		this.valueType,
        		this.getSite( ), 
        		this.fieldMode,
        		this.parentType,
        		this.specifiedFacetName,
        		this.idComponentName,
        		this.declaringType, 
        		theCurrentTypeInfo );
    }

    /**
     * Returns the current mode the field is setup for.
     * @return the mode of the field
     */
    public FieldMode getFieldMode( ) {
    	return this.fieldMode;
    }
    
    /**
     * Indicates the field represents the parent.
     * @return if the parent field
     */
    public boolean isParentField( ) {
    	return this.fieldMode == FieldMode.FACET_PARENT;
    }
    
    /**
     * Returns the parent type if this is a parent field.
     * @return parent type if a parent field, null otherwise
     */
    public StorageType getParentType( ) {
    	return this.parentType;
    }
    
    /**
     * Indicates the field is facet.
     * @return
     */
    public boolean isFacetField( ) {
    	return this.fieldMode == FieldMode.FACET_MEMBER || this.fieldMode == FieldMode.FACET_OBJECT;
    }

    /**
     * Indicates the field is facet that will store
     * object fields into separate columns.
     */
    public boolean isMemberFacetField( ) {
    	return this.fieldMode == FieldMode.FACET_MEMBER;
    }

    /**
     * Indicates the field is facet that will store
     * entire objects in columns.
     */
    public boolean isObjectFacetField( ) {
    	return this.fieldMode == FieldMode.FACET_OBJECT;
    }

    /**
     * Returns the facet prefix to use for this field
     * @return the facet prefix to use
     */
    // TODO: see if this is actually used after we have 'calc'
    public String getSpecifiedFacetName( ) {
    	return this.specifiedFacetName;
    }
    
    /**
     * Returns the calculated regex for the prefix for this field.
     * @return the regex for the prefix for the field
     */
    public String getRegexFacetName( ) {
    	return this.facetNameRegex;
    }
    
    /**
     * Given an instance of the type being pointed to, this will 
     * calculate the name to use if an object facet or the prefix
     * to use if a 
     * @return calculates the facet name to use if an object facet, or prefix if a member facet.
     */
    public String calculateFacetName( Object theInstance ) {
    	// need to iterate over the prefix references
    	// grabbing the values off the instance passed in
    	Preconditions.checkState( isFacetField( ), String.format( "Attempting to use '%s.%s' as a facet field", this.containingType.getName(), this.getName( ) ) );
    	if( this.facetNameReferences.size( ) == 0 ) {
    		return this.specifiedFacetName;
    	} else {
    		StringBuilder prefixBuilder = new StringBuilder();
        	int lastEnd = 0;
        	String helper;
    		for( FacetNameReference reference : this.facetNameReferences ) {
        		if( lastEnd < reference.startIndex ) {
    				// get some of the leading 
    				helper = this.specifiedFacetName.substring( lastEnd, reference.startIndex );
    				prefixBuilder.append( helper );  // we dont' need to escape this because it is the actual value, not regex
    			}
        		prefixBuilder.append( reference.getToStringTranslator().translate( reference.getReferenceField().getData( theInstance ) ) );
        		lastEnd = reference.length;
        	}
        	if( lastEnd < this.specifiedFacetName.length( ) ) {
    			// if we have more text to save, we save it and escape it as well to have a safe regex
    			helper = this.specifiedFacetName.substring( lastEnd, this.specifiedFacetName.length() );
    			prefixBuilder.append( helper ); // we dont' need to escape this because it is the actual value, not regex
        	}
        	return prefixBuilder.toString();
    	}
    }
    
    public StorageType getFacetReferenceType( ) {
    	return this.facetReferenceType;
    }
    
    /**
     * This method stores the reference to the type and setups the name handling.
     * @param theReferenceType the type being referred to by this facet.
     * @param theRegexFacility the regex facility that can help match types
     * @param theStringFacility the string facility to help convert types to strings, for use in the name
     */
    void setFacetReferenceType( StorageType theReferenceType, TypeRegexFacility theRegexFacility, StringTranslationFacility theStringFacility ) {
    	Preconditions.checkNotNull( theReferenceType, String.format( "Requires a reference type for '%s.%s'", this.containingType.getName(), this.getName() ) );
    	Preconditions.checkState( this.isFacetField( ), String.format( "'%s.%s' must be a facet field", this.containingType.getName(), this.getName() ) );
    	Preconditions.checkState( this.facetReferenceType == null, String.format( "Facet reference field already set on '%s.%s'", this.containingType.getName(), this.getName() ) );
    	Preconditions.checkNotNull( theRegexFacility, String.format( "'%s.%s' needs a facility to get type expressions", this.containingType.getName(), this.getName() ) );
    	this.facetReferenceType = theReferenceType;
    
    	// now at this point we look at what was saved for potential 
    	// references into the type for the prefix value
    	// we also need to set the regular expression
    	StringBuilder regexBuilder = new StringBuilder();
    	
    	int lastEnd = 0;
    	String helper;
    	
    	// so now we go through the list of references
    	// storing the reference storage fields and 
    	// creating a regex
    	for( FacetNameReference reference : this.facetNameReferences ) {
    		// first, let's get the referenced field 
    		StorageField referenceField = theReferenceType.getField( reference.getReference( ) );
    		if( referenceField == null ) {
    			throw new IllegalArgumentException( String.format( "Prefix '%s' on '%s.%s' refers to a field '%s.%s' could not be found.", this.specifiedFacetName, this.containingType.getName(), this.getName( ), theReferenceType.getName( ), reference.getReference( ) ) );
    		}
			// and then safe the field for later use
			reference.setReferencedField( referenceField );
			reference.setTranslators( 
					theStringFacility.getFromStringTranslator( referenceField.getFieldType().getType( ) ),
					theStringFacility.getToStringTranslator( referenceField.getFieldType().getType( ) ) );
			// now collect up the text to put together a matching regex
    		if( lastEnd < reference.startIndex ) {
				// get some of the leading 
				helper = this.specifiedFacetName.substring( lastEnd, reference.startIndex );
				regexBuilder.append( Pattern.quote( helper ) );
			}
    		String typeRegex = theRegexFacility.getRegex( referenceField.getFieldType().getType( ) );
    		if( typeRegex == null ) {
    			typeRegex = theRegexFacility.getRegex( String.class );
    		}
    		regexBuilder.append( '(' ); // we put a capturing group around this since it can match to the prefix reference index
			regexBuilder.append( escapeRegExGroups( typeRegex ) ); 
    		regexBuilder.append( ')' );
    		lastEnd = reference.length;
    	}
    	if( lastEnd < this.specifiedFacetName.length( ) ) {
			// if we have more text to save, we save it and escape it as well to have a safe regex
			helper = this.specifiedFacetName.substring( lastEnd, this.specifiedFacetName.length() );
			regexBuilder.append( Pattern.quote( helper ) );
    	}
    	// now we need to create a regex for the expectation
    	this.facetNameRegex = regexBuilder.toString();
    }
   
    // TODO: a copy of this is in REsourceMethod AND HbaseFamilyMap, need to find a better spot
	/**
	 * Helper method that makes sure a user supplied
	 * regex doesn't contain capturing groups, which
	 * would otherwise cause problems with the path
	 * matching
	 * @param theRegEx the regex to escape
	 * @return the non-capturing regex
	 */
	private static String escapeRegExGroups( String theRegEx ) {
		// I feel there is a better way, but this is good enough for now ...
		StringBuilder builder = new StringBuilder();
		
		int charClassCount = 0;
		
		int startOffset = 0;
		char currentChar;
		
		for( int offset = 0; offset < theRegEx.length(); offset += 1 ) {
			currentChar = theRegEx.charAt( offset );
			if( currentChar == '\\' ) { // ignore escaping character
				offset += 1;
			} else if( currentChar == '[' ) { // we are in a capturing group (java supports some nesting, though I don't fully here)
				charClassCount += 1;
			} else if( currentChar == ']' ) { // we are leaving one
				charClassCount -= 1;
			} else if( currentChar == '(' && charClassCount == 0 ) {
				if( ( offset == theRegEx.length( ) - 1 ) || ( theRegEx.charAt( offset + 1 ) != '?' ) ) { // found at the end or next character isn't a quote/meaning non-capturing already
					builder.append( theRegEx.substring( startOffset, offset + 1 ) );
					builder.append( "?:" ); // turn into a non capturing group
					startOffset = offset + 1;
				}
			}
		}
		builder.append( theRegEx.substring( startOffset, theRegEx.length()));
		return builder.toString();
	}
	
	/**
	 * Indicates if this field is meant to be used for 
	 * optimistic concurrency control.
	 * @return true if the occ lock, false otherwise
	 */
	public boolean isLockField( ) {
		return this.fieldMode == FieldMode.LOCK_MEMBER;
	}
	
    /**
     * Indicates if field is part of an id.
     * @return if part of an id
     */
    public boolean isIdField( ) {
    	return this.fieldMode == FieldMode.ID_MEMBER;
    }

    /**
     * Returns the name of the id component it is part of.
     * This may be null if compound ids aren't used or
     * if this isn't an id field.
     * @return the name of the id component.
     */
    public String getIdComponentName( ) {
    	return this.idComponentName;
    }
    
    /**
     * Returns the compound id field for the class that
     * is associated with this storage field. 
     * This may be null if compound ids aren't used or
     * if this isn't an id field.
     * @return the compound id field, or null.
     */
    public CompoundIdField getIdComponentField( ) {
    	return this.idComponentField;
    }
    
    /**
     * Called by someone else in the same package to ensure
     * we have the field associated, which makes runtime
     * id creation faster.
     * @param theIdComponentField
     */
    void setIdComponentField( CompoundIdField theIdComponentField ) {
    	Preconditions.checkNotNull( theIdComponentField, String.format( "Requires an id component field for '%s.%s'", this.containingType.getName(), this.getName() ) );
    	Preconditions.checkState( this.isIdField( ), String.format( "'%s.%s' must be an id field", this.containingType.getName(), this.getName() ) );
    	Preconditions.checkState( this.idComponentField == null, String.format( "Id component field already set on '%s.%s'", this.containingType.getName(), this.getName() ) );
    	this.idComponentField = theIdComponentField;
    }
}
