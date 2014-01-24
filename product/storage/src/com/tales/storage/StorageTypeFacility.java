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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.tales.serialization.StringTranslationFacility;
import com.tales.serialization.TypeRegexFacility;
import com.tales.storage.StorageField;
import com.tales.storage.StorageField.FieldMode;
import com.tales.storage.StorageType;
import com.tales.storage.decorators.IdComponent;
import com.tales.storage.decorators.LockMember;
import com.tales.storage.decorators.StoredMember;
import com.tales.storage.decorators.FacetParent;
import com.tales.storage.decorators.StoredType;
import com.tales.storage.decorators.FacetMember;
import com.tales.storage.decorators.IdMember;
import com.tales.storage.decorators.IdType;
import com.tales.system.Facility;
import com.tales.parts.ValidationSupport;
import com.tales.parts.reflection.ValueType;
import com.tales.parts.sites.FieldSite;
import com.tales.parts.translators.Translator;


/**
 * Manager class that creates info objects for types that represent what fields are 
 * meant to be serialize for the type.
 * @author jmolnar
 *
 */
public class StorageTypeFacility implements Facility {
	private final StringTranslationFacility stringFacility = new StringTranslationFacility();
	private final TypeRegexFacility regexFacility;
	
    private final Map<Class<?>, CompoundIdType> compoundIdTypes = new ConcurrentHashMap< Class<?>, CompoundIdType>( 16, 0.75f, 1 );
    private final Map<Class<?>, StorageType> storageTypes = new ConcurrentHashMap< Class<?>, StorageType>( 16, 0.75f, 1 );

    private final Object lock = new Object( );
    
    public StorageTypeFacility( ) {
    	this( new TypeRegexFacility( ), new StringTranslationFacility( ) );
    }

    public StorageTypeFacility( TypeRegexFacility theRegexFacility, StringTranslationFacility theStringFacility ) {
    	Preconditions.checkNotNull( theRegexFacility, "need a regex facility" );
    	Preconditions.checkNotNull( theStringFacility, "need a string translation facility" );
    	regexFacility = theRegexFacility;
    }
    
	/***
	 * This method is used to add translators into the system for ensuring proper conversion.
	 * The translators are expected to get or set string representations for use in name 
	 * prefixes for field names (ultimately column names)
	 * @param theClass the class being mapped
	 * @param fromStringTranslator the translator that takes a string, sourced from the name, and creates the proper type
	 * @param toStringTranslator the translator that takes a proper type and creates a string to place into the name
	 * @param theRegex the regular expression used to find the value in the prefix name 
	 */
	public void registerNameTranslators( Class<?> theClass, Translator fromStringTranslator, Translator toStringTranslator, String theRegex ) {
		stringFacility.registerTranslators( theClass, fromStringTranslator, toStringTranslator ); // this will validate parameters
		regexFacility.registerRegex( theClass, theRegex ); // this will validate parameters
	}

    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
    public StorageType generateType( Class<?> theType ) {
        Preconditions.checkNotNull( theType, "theType" );

        synchronized( lock ) { // TODO: this should really be the symbol table mechanism
	        StoredType StoredTypeAnnotation = theType.getAnnotation( StoredType.class );
	        
	        if( StoredTypeAnnotation == null ) {
	            throw new IllegalArgumentException( String.format( "Data type '%s' does not have the storage annotation.", theType.getName( ) ) );
	        } else {
	        	return this._generateType( theType );
	        }
        }
    }
	
    /**
     * Analyzes the type and makes it available for serialization.
     * It analyzes the annotations on the class.
     * @param theType the type 
     */
    private StorageType _generateType( Class<?> theType ) {
        Preconditions.checkNotNull( theType, "theType" );

        StoredType storedTypeAnnotation = theType.getAnnotation( StoredType.class );
        if( storedTypeAnnotation == null ) {
        	// if no annotation, then not a contract data type, so return null
        	// and the caller can decide what to do
        	return null;
        } else if( storageTypes.containsKey( theType ) ) {
        	// if we have it already, return it
            return storageTypes.get( theType );
        } else {
            // let's get the base class
            Class<?> baseType = theType.getSuperclass( );
            StorageType baseStorageType = null;
            Collection<StorageField> baseFields = null;
            
            // if we have a base class, we analyze it
            if( baseType != null && !baseType.equals( Object.class ) ) {
            	baseStorageType = generateType( baseType );
                baseFields = baseStorageType.getFields( );
            }
            
            // make sure we get the name information for everything
            String typeName = Strings.isNullOrEmpty( storedTypeAnnotation.name() ) ? theType.getSimpleName( ): storedTypeAnnotation.name();
            String familyName = storedTypeAnnotation.family();
            
            // see if we have a key field, and if so, get it
            IdType idTypeAnnotation = theType.getAnnotation( IdType.class );
            CompoundIdType compoundIdType = null;
            if( idTypeAnnotation != null ) {
            	if( idTypeAnnotation.type( ) == null ) {
                    throw new IllegalArgumentException( String.format( "Data Type '%s' indicates it needs an id type, but doesn't provide one.", theType.getName( ) ) );
            	} else {
            		compoundIdType = generateIdType( idTypeAnnotation.type( ) );
            	}
            }

            // now create the storage type
            StorageType storageType = new StorageType( 
            		typeName, 
            		theType,
            		familyName,
            		StorageLifecycleSupport.class.isAssignableFrom( theType ),
            		ValidationSupport.class.isAssignableFrom( theType ),
            		compoundIdType,
            		baseStorageType );

            // saving this here so that during the recursive look-ups below
            // there shouldn't be a problem
            storageTypes.put( theType, storageType );

            ArrayList<StorageField> fields = new ArrayList<StorageField>( );

            // and put the base type fields into what will be our field collection
            if( baseFields != null ) {
                for( StorageField fieldInfo : baseFields ) {
                    StorageField fieldClone = ( StorageField )fieldInfo.cloneForSubclass( storageType );
                    fields.add( fieldClone ); // now we add a copy to the new type (even if the base has it)
                }
            }

            // now grab the directly declared fields and process them
            for( Field field : theType.getDeclaredFields( ) ) {
            	StorageField storageField = generateField( storageType, field );
            	if( storageField != null ) {
            		fields.add( storageField );
            	}
            }
            // now save the list of fields on the type
            // this will setup the parent field and the key fields
            storageType.setFields( fields );

            // now at this point we grab the facets and try to analyze
            for( StorageField facetField : storageType.getFacetFields( ) ) {
            	if( facetField.isCollection() ) { // indicates this is an array or collections (TODO: arrays aren't fully done)
                	facetField.setFacetReferenceType( facetField.getElementType().getReflectedType(), regexFacility, stringFacility );
            	} else {
					// if none of the above, we have a complex type
                	facetField.setFacetReferenceType( facetField.getFieldType().getReflectedType(), regexFacility, stringFacility );
				}
            }
            
             // TODO: consider a 'validate' method at this point
            return storageType;
        }
    }
    
    /**
     * Analyzes the field to recursively go through the field type
     * looking at element types if a collection, key/value types
     * if a map and the fields if a complex type.
     * @param theContainingType the type the field is a member of
     * @param theField the field we are to look at 
     * @return returns the generated data contract field, or null if the field isn't suitable
     */
	private StorageField generateField( StorageType theContainingType, Field theField ) {
		StorageField storageField = null;

        StoredMember dataMemberAnnotation = theField.getAnnotation( StoredMember.class );
        LockMember lockMemberAnnotation = theField.getAnnotation( LockMember.class );
        IdMember idMemberAnnotation = theField.getAnnotation( IdMember.class );
        FacetParent facetParentAnnotation = theField.getAnnotation( FacetParent.class );
        FacetMember facetMemberAnnotation = theField.getAnnotation( FacetMember.class );

        // if we have at least one of the above, we will create the storage field
        if( dataMemberAnnotation != null || lockMemberAnnotation != null || idMemberAnnotation != null || facetParentAnnotation != null || facetMemberAnnotation != null ) {
			Class<?> fieldType = theField.getType();
			Type fieldGenericType = theField.getGenericType();
			
			// this is used for all calls and represents thef ield
			ValueType<StorageType, StorageField> reflectedFieldType = null;
			// these are used for elements in collections
			Class<?> elementType = null;
			ValueType<StorageType, StorageField> reflectedElementType = null;
			// these are used for maps only
			Class<?> keyType = null;  
			ValueType<StorageType, StorageField> reflectedKeyType = null;
			// these are used for both the value parts for maps and elements for collections
			Class<?> valueType = null;
			ValueType<StorageType, StorageField> reflectedValueType = null;
			
			// we try to get the types we are dealing with
			if( Map.class.isAssignableFrom( fieldType ) && ( fieldGenericType instanceof ParameterizedType ) ) {
				// if we have a map (hashtable, treemap, etc)
				// we need to get type information for the keys and values
				reflectedFieldType = new ValueType<StorageType, StorageField>( fieldType, fieldGenericType );

				keyType = ( Class<?> )( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ];
	            reflectedKeyType = new ValueType<StorageType, StorageField>( keyType, null, _generateType( keyType ) );
	            valueType = ( Class<?> )( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 1 ];
	            reflectedValueType = new ValueType<StorageType, StorageField>( valueType, null, _generateType( keyType ) );
	            
			} else if( ( Collection.class.isAssignableFrom( fieldType ) && ( fieldGenericType instanceof ParameterizedType ) ) ) {
				// if we have a collection (e.g list, set, collection itself, etc)
				// we need to get the type information for the collection element
				reflectedFieldType = new ValueType<StorageType, StorageField>( fieldType, fieldGenericType );

				elementType = ( Class<?> )( ( ( ParameterizedType ) fieldGenericType ).getActualTypeArguments( )[ 0 ] );
	            reflectedElementType = new ValueType<StorageType, StorageField>( elementType, null, _generateType( elementType ) );

	    	} else if( fieldType.isArray( ) ) {
	    		// if we have an array we basically do the same thing as a collection which means
	    		// we need to get the type information for the array element
				reflectedFieldType = new ValueType<StorageType, StorageField>( fieldType, fieldGenericType );

				elementType = fieldType.getComponentType();
	            reflectedElementType = new ValueType<StorageType, StorageField>( elementType, null, _generateType( elementType ) );

	    	} else {
	    		// so we have either a simple type, primitive type, enum or non-collection complex type
				reflectedFieldType = new ValueType<StorageType, StorageField>( fieldType, fieldGenericType, _generateType( fieldType ) );
	    	}

			// the field site (for getting/setting data)
			FieldSite fieldSite = new FieldSite( theField );
	        // make sure the field is accessible (we use it later)
	        theField.setAccessible( true ); 
	        
	        // now, given we have the types, we see what we can do with this
            if( dataMemberAnnotation != null ) {
            	if( idMemberAnnotation != null || facetParentAnnotation != null || facetMemberAnnotation != null || lockMemberAnnotation != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as a member but also either a key, parent, lock or facet.", theContainingType.getName( ), theField.getName( ) ) );
            	} else {
                    // set the proper names for the field
                    String fieldName = Strings.isNullOrEmpty( dataMemberAnnotation.name( ) ) ? theField.getName( ) : dataMemberAnnotation.name( );
                    storageField = new StorageField( 
                    		fieldName,
                    		reflectedFieldType,
                    		reflectedElementType,
                    		reflectedKeyType,
                    		reflectedValueType,
                    		fieldSite,
                    		FieldMode.STORED_MEMBER,
                    		null, 
                    		null, 
                    		null, 
                    		theContainingType );                
            	}
            } else if( lockMemberAnnotation != null ) {
            	if( idMemberAnnotation != null || facetParentAnnotation != null || facetMemberAnnotation != null || dataMemberAnnotation != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as a lock member but also either a standard member, key, parent, lock or facet.", theContainingType.getName( ), theField.getName( ) ) );
            	} else {
                    // set the proper names for the field
                    String fieldName = Strings.isNullOrEmpty( lockMemberAnnotation.name( ) ) ? theField.getName( ) : lockMemberAnnotation.name( );
                    storageField = new StorageField( 
                    		fieldName,
                    		reflectedFieldType,
                    		reflectedElementType,
                    		reflectedKeyType,
                    		reflectedValueType,
                    		fieldSite,
                    		FieldMode.LOCK_MEMBER,
                    		null, 
                    		null, 
                    		null, 
                    		theContainingType );                
            	}
            } else if( idMemberAnnotation != null ) {
            	if( dataMemberAnnotation != null || facetParentAnnotation != null || facetMemberAnnotation != null || lockMemberAnnotation != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as an id member but also either a standard member, paren, lock or facet.", theContainingType.getName( ), theField.getName( ) ) );                		
            	} else if( keyType != null || elementType != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as an id member but using a map or collection for its type.", theContainingType.getName( ), theField.getName( ) ) );
            	} else {
                    // set the proper names for the field
                    String fieldName = Strings.isNullOrEmpty( idMemberAnnotation.name( ) ) ? theField.getName( ) : idMemberAnnotation.name( );
                    String idFieldName = Strings.isNullOrEmpty( idMemberAnnotation.component( ) ) ? theField.getName( ) : idMemberAnnotation.component( );
                    // now save the field 
                    storageField = new StorageField( 
                    		fieldName, 
                    		reflectedFieldType,
                    		null,
                    		null,
                    		null,
                    		fieldSite, 
                    		FieldMode.ID_MEMBER,
                    		null, 
                    		null, 
                    		idFieldName, 
                    		theContainingType );
            	}
            	
            } else if( facetParentAnnotation != null ) {
            	if( dataMemberAnnotation != null || idMemberAnnotation != null || facetMemberAnnotation != null || lockMemberAnnotation != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as a parent member but also either a standard member, id member, lock or facet.", theContainingType.getName( ), theField.getName( ) ) );                		
            	} else if( keyType != null || elementType != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as a parent member but using a map or collection for its type.", theContainingType.getName( ), theField.getName( ) ) );
            	} else {
                    // set the proper names for the field
                    String fieldName = theField.getName( );
                    // so we need to keep the parent type, just in case
                    StorageType parentType = this.generateType( theField.getType( ) );
                    // now save the field 
                    storageField = new StorageField( 
                    		fieldName,
                    		reflectedFieldType,
                    		null,
                    		null,
                    		null,
                    		fieldSite, 
                    		FieldMode.FACET_PARENT,
                    		parentType,
                    		null, 
                    		null, 
                    		theContainingType );
            	}                	

            } else if( facetMemberAnnotation != null ) {
            	if( dataMemberAnnotation != null || idMemberAnnotation != null || facetParentAnnotation != null || lockMemberAnnotation != null ) {
                    throw new IllegalStateException( String.format( "Field '%s.%s' is listed as a facet but also either a standard member, id member, lock or parent.", theContainingType.getName( ), theField.getName( ) ) );
            	} else {
                    String fieldName = theField.getName( );

                    // if we have a facet then we need to see what kind of facet
                    // a member (meaning all fields stored in separate columns) or
                    // an object (meaning the object is in a single column)
                    String facetPrefix = facetMemberAnnotation.prefix( );
                    boolean hasPrefix = !Strings.isNullOrEmpty( facetPrefix );
                    String facetName = facetMemberAnnotation.name( );
                    boolean hasName = !Strings.isNullOrEmpty( facetName );

            		if( !hasPrefix && !hasName ) {
            			throw new IllegalStateException( String.format( "Field '%s.%s' doesn't have a prefix nor a name.", theContainingType.getName( ), theField.getName( ) ) );
            		} else if( hasPrefix && hasName ) {
            			throw new IllegalStateException( String.format( "Field '%s.%s' has a prefix and a name.", theContainingType.getName( ), theField.getName( ) ) );
            		}

                    FieldMode fieldMode = hasPrefix ? FieldMode.FACET_MEMBER : FieldMode.FACET_OBJECT; 

                    // now save the field 
                    storageField = new StorageField( 
                    		fieldName,
                    		reflectedFieldType,
                    		reflectedElementType,
                    		reflectedKeyType,
                    		reflectedValueType,
                    		fieldSite, 
                    		fieldMode,
                    		null, 
                    		hasPrefix ? facetPrefix : facetName, 
                    		null, 
                    		theContainingType );
            	}                	
            }
        }
        return storageField;
	}
	
    /**
     * Helper method that generates the compound id type.
     * @param theIdType the id type to generate an id type for
     * @return the generated compound id type
     */
    private CompoundIdType generateIdType( Class<?> theIdType ) {
        if( compoundIdTypes.containsKey( theIdType ) ) {
            return compoundIdTypes.get( theIdType );
        } else {
	    	String name = theIdType.getSimpleName();
	    	CompoundIdType compoundIdType = new CompoundIdType( name, theIdType );
	        ArrayList<CompoundIdField> fields = new ArrayList<CompoundIdField>( );
	
	        // now grab the directly declared fields
	        for( Field field : theIdType.getDeclaredFields( ) ) {
	            IdComponent idAnnotation = field.getAnnotation( IdComponent.class );
	            
	            if( idAnnotation != null ) {
		            // make sure the field is accessible
		            field.setAccessible( true );
		            fields.add( new CompoundIdField( field.getName( ), idAnnotation.order(), new FieldSite( field ), compoundIdType ) );
	            } // else we ignore since we aren't sure if we should use it or not
	        }
	        compoundIdType.setFields( fields );
	        
	        // save for later reference
	        compoundIdTypes.put( theIdType, compoundIdType );
	        return compoundIdType;
        }
    }
}
