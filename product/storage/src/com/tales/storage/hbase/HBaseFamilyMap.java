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
package com.tales.storage.hbase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.RegularExpressionHelper;
import com.tales.storage.StorageField;
import com.tales.storage.StorageType;

public class HBaseFamilyMap {
	private Map<StorageType, HBaseFamilyPartMap> familyParts = Collections.unmodifiableMap( new HashMap<StorageType, HBaseFamilyPartMap>( ) );
	
	private final String name;
	private final byte[] nameBytes;
	private final HBaseTableMap containingTable;
	
	public HBaseFamilyMap( String theName, HBaseTableMap theContainingTable ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "must have a name" );
		Preconditions.checkNotNull( theContainingTable, "need the containing table" );
		
		name = theName;
		nameBytes = Bytes.toBytes( name );
		containingTable = theContainingTable;
	}
	
	public String getName( ) {
		return name;
	}
	
	public byte[] getNameBytes( ) {
		return nameBytes;
	}
	
	public HBaseTableMap getContainingTable( ) {
		return containingTable;
	}
	
	public HBaseFamilyPartMap getFamilyPart( StorageType theStorageType ) {
		return familyParts.get( theStorageType );
	}

	public Collection<HBaseFamilyPartMap> getFamilyParts( ) {
		return familyParts.values();
	}
	
    /**
     * Sets the families on this table. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theFamilies the families to use
     */
    void setFamilyParts( Collection<HBaseFamilyPartMap> theFamilyParts ) {
    	Preconditions.checkNotNull( theFamilyParts, "need family parts" );
    	Preconditions.checkState( familyParts.size() == 0, "columns are already set" );

    	HashMap<StorageType,HBaseFamilyPartMap> newParts = new HashMap<StorageType, HBaseFamilyPartMap>( theFamilyParts.size() );
    	
    	for( HBaseFamilyPartMap familyPart : theFamilyParts ) {
    		// TODO: naming for columns is actually pretty complicated so
    		//       below isn't accurate at all since it gets calculated at runtime, and even the prefix
    		//       isn't considered . . . need to fix
    		if( newParts.containsKey( familyPart.getStorgeType( ) ) ) {
    			throw new IllegalStateException( String.format( "The family with name '%s' is attempting to add more than part for storage type '%s'.", this.name, familyPart.getStorgeType().getName( ) ) );
    		} else if( familyPart.getContainingFamily() != this ) {
    			throw new IllegalStateException( String.format( "The family with name '%s' is attempting to add a family part for '%s', but the part is associated with the family '%s'.", this.name, familyPart.getStorgeType().getName( ), familyPart.getContainingFamily().getName() ) );
    		} else {
    			newParts.put( familyPart.getStorgeType(), familyPart );
    		}
    	}
    	this.familyParts = Collections.unmodifiableMap( newParts );
    	
    	// so maybe there is an idea of knowing how it is used
    	// if a root, then it is only called by column
    	// everythign else is prefix
    	
    	// don't want everything, just what is accessible
    	// and for those facets that live externally (dont' have facet, member), maybe we can do it dynamically based
    	// on what it passed in on the reqeust to datacontext
    }
    
    private List<ColumnReference> columnReferences = new ArrayList<ColumnReference>( );
    
    public ColumnReference getColumn( String theFullName ) {
    	ColumnReference returnReference = null;
    	
    	Matcher matcher = familyPattern.matcher( theFullName );
    	
    	if( matcher.matches() ) {
	    	for( int index = 0; index < columnReferences.size(); index += 1 ) {
	    		if( matcher.group( index + 1 ) != null) {
	    			returnReference = columnReferences.get( index );
	    			break;
	    		}
	    	}
    	}
    	
    	// TODO: this doesn't help the dynamic name
    	//       side since we need to extract from the
    	//       regex match (the capture group) the reference
    	//       so we can then return it here (the first part) 
    	//       for lookup after
//    	for( ColumnReference reference : columnReferences ) {
//    		if( reference.getColumnPattern().matcher( theFullName ).matches() ) {
//    			returnReference = reference;
//    			break;
//    		}
//    	}
    	return returnReference;
    }
    
    public static class ColumnReference {
    	private final String columnRegex;
    	private final Pattern columnPattern;
    	private final StorageField facetReference;
    	private final HBaseColumnMap realColumn;
    	private final HBaseColumnVirtualMap virtualColumn;
    	
    	public ColumnReference( StorageField theFacetReference, HBaseColumnVirtualMap theVirtualColumnMap, String theColumnRegex ) {
    		// TODO: this is a virtual column based on a FACET_OBJECT
    		//       so we need to figure out how to handle this exactly
    		Preconditions.checkArgument( !Strings.isNullOrEmpty( theColumnRegex ), "need a column prefix" );
    		// we don't check for facet reference being null
    		// because if it is null we have a root column
    		facetReference = theFacetReference;
    		realColumn = null;
    		virtualColumn = theVirtualColumnMap;
    		columnRegex = theColumnRegex;
    		columnPattern = Pattern.compile( columnRegex );
    	}

    	public ColumnReference( StorageField theFacetReference, HBaseColumnMap theColumn, String theColumnRegex ) {
    		Preconditions.checkNotNull( theColumn, "need a column" );
    		Preconditions.checkArgument( !Strings.isNullOrEmpty( theColumnRegex ), "need a column prefix" );
    		// we don't check for facet reference being null
    		// because if it is null we have a root column
    		facetReference = theFacetReference;
    		realColumn = theColumn;
    		virtualColumn = null;
    		columnRegex = theColumnRegex;
    		columnPattern = Pattern.compile( columnRegex );
    	}

    	public String getReferenceName( String theColumnName ) {
    		if( realColumn != null ) {
    			if( facetReference.isCollection() ) {
    				return theColumnName.substring( 0, theColumnName.length( ) - realColumn.getName( ).length() );
    			} else {
    				return facetReference.getSpecifiedFacetName();
    			}
    		} else {
    			return theColumnName;
    		}
    	}
    	
    	public StorageField getFacetReference( ) {
    		return facetReference;
    	}
    	
    	public HBaseColumnMap getRealColumn( ) {
    		return realColumn;
    	}
    	
    	public HBaseColumnVirtualMap getVirtualColumn( ) {
    		return virtualColumn;
    	}
    	
    	public Pattern getColumnPattern( ) {
    		return columnPattern;
    	}
    }
    
    private String familyRegex = null;
    private Pattern familyPattern;
    
    void prepareNameLookup( ) {
    	int rootsFound = 0;
		Collection<StorageField> referringMemberFacetFields;
		Collection<StorageField> referringObjectFacetFields;
		
    	for( HBaseFamilyPartMap familyPart : this.familyParts.values() ) {
    		referringMemberFacetFields = familyPart.getReferringMemberFacetFields();
    		referringObjectFacetFields = familyPart.getReferringObjectFacetFields();
    		if( referringMemberFacetFields.size() == 0 && referringObjectFacetFields.size() == 0) {
    			// so no referring parts, which should mean this is the root
    			if( !familyPart.getStorgeType().isRootStorageType() ) {
    				// if this isn't the root, then we somehow got in a bad
    				// state, which shoudldn't be possible, no references
    				throw new IllegalStateException( String.format( "Table '%s' on family '%s' has a storage type '%s' that has no referring facets, but is not the storage root.", this.containingTable.getName(), this.getName(), familyPart.getStorgeType().getName() ) );
    			
    			} else if( rootsFound > 0 ) {
    				// so somehow we have more than one storage type that thinks
    				// it is the root type, which shouldn't be possible.
    				// TODO: technically there should only be one for the entire table.
    				throw new IllegalStateException( String.format( "Table '%s' on family '%s' has a storage type '%s' that has no referring facets, and is considered the storage root.", this.containingTable.getName(), this.getName(), familyPart.getStorgeType().getName() ) );

    			} else {
    				rootsFound += 1;
    				for( HBaseColumnMap column : familyPart.getRealColumns() ) {
    					if( familyRegex == null ) {
    						familyRegex = "";
    					} else {
    						familyRegex += "|";
    					}
    					String columnRegex = "(" + Pattern.quote( column.getName( ) ) + ")";
    					familyRegex += columnRegex;
    					columnReferences.add( new ColumnReference( null, column, columnRegex ) );
    				}
    			}
    		} else {
    			// TODO: look for overlaps in names

    			// we have referring fields, so shouldn't be the root, so let's work them up
    			// at this point I need to take the referring field
    			// create a regular expression based on the prefix I already have
    			// which means I need to escape the column name and append and
    			// then put that in the name mapping and then change the name 
    			// mapping to be the 

	    		for( StorageField referringField : referringMemberFacetFields ) {	    			
    				for( HBaseColumnMap column : familyPart.getRealColumns() ) {
    					if( familyRegex == null ) {
    						familyRegex = "";
    					} else {
    						familyRegex += "|";
    					}
    					String columnRegex = "(" + RegularExpressionHelper.toNoncapturingExpression( referringField.getRegexFacetName() + Pattern.quote( column.getName( ) ) ) + ")";
    					familyRegex += columnRegex;
    					columnReferences.add( new ColumnReference( referringField, column, columnRegex ) );
    				}
	    		}

	    		for( StorageField referringField : referringObjectFacetFields ) {
	    			// TODO: if this is an MEMBER_FACET then we need to do as below outlines
	    			//       we need to go over each columns and create a mapping
	    			//       if this is an OBJECT_FACET then we have a single column
	    			//       that represents the entire object, so maybe we have two 
	    			//       different type of columns, we have a type for each 
	    			//       well defined type as outlined in the object itself
	    			//       and then we have the other type, which is a single summary
	    			//       field, not related to the others . . . maybe we want to
	    			//       create a separate family part for the object facet types
	    			//       the problem is that the storage types currently map to ONE
	    			//       storage map, and that is no longer true in this circumstance
	    			//       OR they could, but they represent a particular column and the 
	    			//       the column has to be properly marked
	    			//       SO it is going to create a column that has a type
	    			//       that is the SAME as the family part type, with no default name
	    			//       but derived off the regex?? . . . other way to do this, is to
	    			//       consider that column map's storage field is the storage field
	    			//       from the FACET side, so it could point to it, though ideally
	    			//       the column is marked specially to make sure it handles the 
	    			//       case properly, looks like no references, so this may work, 
	    			// 		 though we will wnat to make that it is different, we will also
	    			//       also need to watch that columns are only created for things 
	    			//       that are actually saved (check what happens when you save
	    			//       if only one of these odd references exist)
	    			
					if( familyRegex == null ) {
						familyRegex = "";
					} else {
						familyRegex += "|";
					}
					String columnRegex = "(" + RegularExpressionHelper.toNoncapturingExpression( referringField.getRegexFacetName() ) + ")";
					familyRegex += columnRegex;
					columnReferences.add( new ColumnReference( referringField,familyPart.getVirtualColumn( referringField ), columnRegex ) );
	    		}
    		}
    	}
    	familyPattern = Pattern.compile( familyRegex );
    }
}
