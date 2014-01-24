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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tales.parts.reflection.TypeDescriptor;

/**
 * This class represents a class which is to be serialized.
 * @author jmolnar
 */
public class StorageType extends TypeDescriptor<StorageType, StorageField> {
    private final StorageType baseType;
    private String family; // this isn't marked final because we may not be able to set until we have the parent field

    private final boolean supportsLifecycle; // supports lifecycle, so we should call lifecycle methods 
    private final boolean supportsValidation; // supports validation

    private StorageField parentField; // if this is set, then we are a facet
    
    private StorageField optimisticLockField; // if this is set we must be a root

    private CompoundIdType compoundIdType; // if this is set, we can or are a root, and have more than one field
    private Class<?> idType; // if this is set, we can or are a root but the id may be a single or compound field
    private Map<String, StorageField> idFields = Collections.unmodifiableMap( new HashMap<String, StorageField>( 0 ) ); // the fields making up the compound id
    
    private Map<String, StorageField> facetFields = Collections.unmodifiableMap( new HashMap<String, StorageField>( 0 ) ); // the fields that refer to other types

    private Map<String, StorageField> storedFields = Collections.unmodifiableMap( new HashMap<String, StorageField>( 0 ) ); // the fields that will be persisted (ignore optimizations)

    private Map<String, ArrayList<StorageType>> families = new HashMap<String, ArrayList<StorageType>>( 0 ); // the families and their associated types (includes the root)
    private Map<String, StorageType> children = new HashMap<String, StorageType>( ); // the children (facets) of this class (doesn't include the root)
    
    /**
     * Constructor taking the name to use and the underlying type represented.
     * This implementation does not construct the fields since it would rely
     * on the assumption that only annotations can be used to create the 
     * objects. 
     * @param theName the name to give the type
     * @param theType the underlying type
     * @param theBaseType the base class
     */
    StorageType( String theName, Class<?> theType, String theFamily, boolean lifecycleSupport, boolean validationSupport, CompoundIdType theCompoundIdType, StorageType theBaseType ) {
    	super( theName, theType );

        this.family = Strings.isNullOrEmpty( theFamily ) ? "" : theFamily; 
        this.supportsLifecycle = lifecycleSupport;
        this.supportsValidation = validationSupport;
        this.compoundIdType = theCompoundIdType;
        this.baseType = theBaseType;
    }
    
    /**
     * Indicates if this type is a root type, meaning the primary type
     * that other classes associate with. It means it has an id type
     * associated with it.
     * @return true if a root storage type, false otherwise
     */
    public boolean isRootStorageType( ) {
    	return this.idType != null;
    }
    
    /**
     * Get the family this class happens to be associated with.
     * @return
     */
    public String getFamily( ) {
    	return this.family;
    }
    
    /**
     * Returns all of the families if this is a root storage type.
     * @return all of the families (in map form)
     */
    public Map<String, ArrayList<StorageType>> getFamilies( ) {
    	return this.families;
    }

    /**
     * Gets the type representing the id. If there is an id for the class something will 
     * be returned regardless if it is a single item id, or a compound id.
     * @return the type representing the id, which can be null if there are no id members.
     */
    public Class<?> getIdType( ) {
    	return this.idType;
    }
    
    /**
     * The compound id type for the storage type.
     * @return returns null if there isn't a compound type associated, otherwise it returns the type
     */
    public CompoundIdType getCompoundIdType( ) {
    	return this.compoundIdType;
    }
    
    /**
     * Indicates if the types a facet class, meaning not directly
     * represent in the database, but as columns in another.
     * @return
     */
    public boolean isFacet( ) {
    	return this.getParentField() != null;
    }
    
    /**
     * Indicates the underlying class supports data lifecycle support.
     * @return
     */
    public boolean supportsLifecycle( ) {
    	return this.supportsLifecycle;
    }
    
    /**
     * Indicates the underlying class supports validation.
     * @return
     */
    public boolean supportsValidation( ) {
    	return this.supportsValidation;
    }
    
    /**
     * That fields on the current object that are intended to be persisted/serialized.
     */
    public Collection<StorageField> getStoredFields( ) {
    	return storedFields.values();
    }
    
    /**
     * Sets the fields that will be persisted/serialized. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theStoredFields the fields to use
     */
    private void setStoredFields( Map<String,StorageField> theStoredFields ) {
    	Preconditions.checkNotNull( theStoredFields, "need stored fields" );
    	Preconditions.checkState( storedFields.size() == 0, "stored fields are already set" );

    	storedFields = Collections.unmodifiableMap( theStoredFields );
    }
    
    /**
     * Returns the field that is acting as a pointer to the parent storage type.
     * If the value isn't null then this class is a facet and stored with the parent
     * but if it is null then it is a stand alone class that is saved on its own.
     * @return the storage field
     */
    public StorageField getParentField( ) {
    	return this.parentField;
    }
    
    /**
     * Helper method that sets the parent field, 
     * @param theParentField
     */
    private void setParentField( StorageField theParentField ) {
    	Preconditions.checkNotNull( theParentField, "need a parent field" );
    	Preconditions.checkState( this.parentField == null, "parent field is already set" );
    	Preconditions.checkState( this.idFields.size() == 0, "cannot have a parent field on a class with id fields" );
    	Preconditions.checkState( this.containsField( theParentField.getName( ) ), "parent field must be in field list" );
    	
    	this.parentField = theParentField;
		
		// if we don't have the family, get it from the parent
    	if( Strings.isNullOrEmpty( this.family ) ) {
    		this.family = parentField.getParentType().getFamily();
    	}
    	// we add the child after to make sure that 
    	// the right family value is in use
    	parentField.getParentType().addChild( this );
    }

    /**
     * Returns the field acting as optimistic concurrency control. 
     * @return the storage field 
     */
    public StorageField getOptimisticLockField( ) {
    	return optimisticLockField;
    }

    /**
     * Helper method that sets the parent field, 
     * @param theParentField
     */
    private void setOptimisticLockField( StorageField theLockField ) {
    	Preconditions.checkNotNull( theLockField, "need a lock field" );
    	Preconditions.checkState( this.optimisticLockField == null, "lock field is already set" );
    	Preconditions.checkState( isRootStorageType(), "cannot have a lock field on a class that isn't the root" );
    	Preconditions.checkState( this.containsField( theLockField.getName( ) ), "lock field must be in field list" );
    	
    	this.optimisticLockField = theLockField;
    }


    /**
     * Gets an instance of the id for a particular instance of this storage type.
     * @param theInstance the instance of the object to get the id for
     * @return an instance of the id for the class
     */
    public Object getIdInstance( Object theInstance ) {
    	Preconditions.checkArgument( this.isRootStorageType( ), "need to be a root storage type" );
    	Preconditions.checkNotNull( theInstance, String.format("need an instance to create an id for '%s'", this.name ) );
    	
    	Object id = null;
    	if( this.compoundIdType != null ) {
    		id = compoundIdType.newInstance( );
    		for( StorageField storageField : this.idFields.values( ) ) {
    			// we set the field on the id instance
    			storageField.getIdComponentField().setData( id, storageField.getData( theInstance ) );
    		}
    	} else if( this.idType != null ) {
    		// this means we have the single field version of an id
    		// TODO: this expensive, should consider holding the field out separately in this case
    		for( StorageField storageField : this.idFields.values( ) ) {
    			// this will only have one value
    			id = storageField.getData( theInstance );
    		}
    	} else {
    		// this should never happen
    		throw new IllegalStateException( String.format( "cannot create an id for '%s'", this.name ) );
    	}
    	return id;
    }
    
    /**
     * Gets the fields that make up the id.
     * @return fields that make up the id
     */
    public Collection<StorageField> getIdFields( ) {
    	return this.idFields.values();
    }
    
    /**
     * Sets the key fields on this object. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theFields the fields to use
     */
    private void setIdFields( Map<String,StorageField> theIdFields ) {
    	Preconditions.checkNotNull( theIdFields, "need key fields" );
    	Preconditions.checkState( idFields.size() == 0, "key fields are already set" );
    	Preconditions.checkState( this.parentField == null, "cannot have id fields on a class with a parent" );
    	Preconditions.checkState( 
    			( this.compoundIdType == null && theIdFields.size( ) < 2 ) ||
    			( this.compoundIdType != null && theIdFields.size( ) == this.compoundIdType.getFields().size() ),  
    			"either specify a compount id and annotate the correct number of id fields, or don't have a compound id" );

    	// at this point, check the id type and make sure it is okay
    	
    	idFields = Collections.unmodifiableMap( theIdFields );
    	if( idFields.size() == 1 ) {
    		for( StorageField idField : idFields.values( ) ) {
    			// this will only run once
    			this.idType = idField.getFieldType().getType();
    		}
    	} else if( idFields.size( ) > 1 ) {
    		// now we validate the fields 
    		this.idType = compoundIdType.getType();
    		for( StorageField idField : idFields.values( ) ) {
    			CompoundIdField compoundIdField = compoundIdType.getField( idField.getIdComponentName() );
    			if( compoundIdField == null ) {
    				throw new IllegalStateException( String.format( "Id member '%s.%s' refers to id component '%s.%s', but the member could not be found.", getType().getName(),idField.getName( ), idType.getName( ),idField.getIdComponentName( ) ) );
    			} else {
    				// save the associated (and it has to be this way to the
    				// CompoundIdType can be StorageType agnostic)
    				// this speeds up runtime performance
    				idField.setIdComponentField( compoundIdField );
    			}
    		}
    	}
    	// at this point we are a confirmed root, so we add ourselves to the list
    	// of families (though not children) to make sure it is all recorded
    	addToFamily( this );
    }

    /**
     * That fields on the current object that represents facets.
     * @return
     */
    public Collection<StorageField> getFacetFields( ) {
    	return facetFields.values();
    }
    
    /**
     * Sets the facet fields on this object. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theFields the fields to use
     */
    private void setFacetFields( Map<String,StorageField> theFacetFields ) {
    	Preconditions.checkNotNull( theFacetFields, "need facet fields" );
    	Preconditions.checkState( facetFields.size() == 0, "facet fields are already set" );

    	facetFields = Collections.unmodifiableMap( theFacetFields );
    }

    /**
     * The type info for the superclass.
     * @return
     */
    public StorageType getBaseType( ) {
    	return this.baseType;
    }

    /**
     * Sets the fields on this object. This should only be called by the 
     * helpers and occurs within close proximity to this classes
     * creation.
     * @param theFields the fields to use
     */
    @Override
	public void setFields( Collection<StorageField> theFields ) {
    	Preconditions.checkNotNull( theFields, "need fields" );
    	Preconditions.checkState( fields.size() == 0, "fields are already set" );
    	
    	HashMap<String,StorageField> newFields = new HashMap<String, StorageField>( theFields.size() );
    	HashMap<String,StorageField> newStoredFields = new HashMap<String, StorageField>( theFields.size() );
    	HashMap<String,StorageField> newIdFields = new HashMap<String, StorageField>( theFields.size() );
    	HashMap<String,StorageField> newFacetFields = new HashMap<String, StorageField>( theFields.size() );
    	StorageField newParentField = null;
    	StorageField newLockField = null;
    	
    	for( StorageField field : theFields ) {
    		if( newFields.containsKey( field.getName( ) ) ) {
    			throw new IllegalStateException( String.format( "The storage type with name '%s' and type '%s' is attempting to add more than one field called '%s'.", this.getName(), this.getType().getName(), field.getName( ) ) );
    		} else if( field.getContainingType() != this ) {
    			throw new IllegalStateException( String.format( "The storage type with name '%s' and type '%s' is attempting to add a field called '%s', but the member is associated to the type '%s'.", this.getName( ), this.getType( ).getName(), field.getName( ), field.getContainingType().getType().getName() ) );
    		} else {
    			// we go through all of the fields and 
    			// save them in the properly categorized
    			// collections, as well as the global list
    			newFields.put( field.getName( ), field );
    			if( field.isIdField( ) ) {
    				newIdFields.put( field.getName(), field );
    				newStoredFields.put( field.getName(), field );
    			} else if( field.isFacetField( ) ) {
    				newFacetFields.put( field.getName( ), field );
    			} else if( field.isLockField() ) {
    				// we have a parent field, make sure we only have one
    				if( newLockField != null ) {
    	    			throw new IllegalStateException( String.format( "The storage type with name '%s' and type '%s' is attempting to add more than one optimistic lock field.", this.getName( ), this.getType( ).getName( ) ) );
    				} else {
    					newLockField = field;
    				}
    			} else if( field.isParentField( ) ) {
    				// we have a parent field, make sure we only have one
    				if( newParentField != null ) {
    	    			throw new IllegalStateException( String.format( "The storage type with name '%s' and type '%s' is attempting to add more than one parent field.", this.getName( ), this.getType( ).getName( ) ) );
    				} else {
        				newParentField = field;
    				}
    			} else {
    				// this is a non id, non facet, non parent field (i.e. it is a normal field)
    				newStoredFields.put( field.getName(), field );
    			}
    		}
    	}
    	// save to 'all' collection
    	setFields( newFields );
    	
    	// now check to see if we have others to save
    	if( newStoredFields.size( ) > 0 ) {
    		setStoredFields( newStoredFields );
    	}
    	if( newIdFields.size( ) > 0 ) {
    		setIdFields( newIdFields );
    	}
    	if( newFacetFields.size( ) > 0 ) {
    		setFacetFields( newFacetFields );
    	}
    	if( newLockField != null ) {
    		setOptimisticLockField( newLockField );
    	}
    	if( newParentField != null ) {
    		setParentField( newParentField );
    	}
    }
    
    /**
     * Adds the child type to this class, meaning this is a root.
     * @param theChild the child type to add
     */
    private void addChild( StorageType theChild ) {
    	Preconditions.checkNotNull( theChild, "need a child" );
    	// TODO: I removed below because I changed how things were order, and this should go back, to be safe, when I fix up type facility to process forward references
    	//Preconditions.checkArgument( !this.isFacet() && this.idType != null, "can only add children to items that are a root with an id type" );
    	Preconditions.checkArgument( !this.children.containsKey( theChild.getName() ), "cannot add a child twice" );
    	
    	this.children.put( theChild.getName( ), theChild );
    	addToFamily( theChild );
    }
    
    /**
     * Adds the type, which is a child, and can be itself,
     * to the appropriate family list.
     * @param theType the type to added to the right family
     */
    private void addToFamily( StorageType theType ) {
    	ArrayList<StorageType> oneFamily = this.families.get( theType.getFamily( ) );
    	if( oneFamily == null ) {
    		oneFamily = new ArrayList<StorageType>( 2 );
    		oneFamily.add( theType );
    		this.families.put( theType.getFamily(), oneFamily );
    	} else {
    		oneFamily.add( theType );
    	}
    }
}
