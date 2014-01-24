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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.tales.storage.DataContext;
import com.tales.storage.Filter;
import com.tales.storage.Query;
import com.tales.storage.RowFilter;
import com.tales.storage.StorageException;
import com.tales.storage.StorageField;
import com.tales.storage.StorageField.FieldMode;
import com.tales.storage.StorageStatus;
import com.tales.storage.StorageType;
import com.tales.storage.hbase.HBaseFamilyMap.ColumnReference;

public class HBaseDataContext implements DataContext<HBaseDataRepository, HBaseDataContext> {
    private static final Logger logger = LoggerFactory.getLogger( HBaseDataContext.class ); // log against the id, so we can group up from anywhere
	private final HBaseDataRepository repository;
	private final StorageStatus status; // we will cache this here so we don't keep going back to the repository
	
	HBaseDataContext( HBaseDataRepository theRepository ) {
		Preconditions.checkNotNull( theRepository, "need a repository" );
		repository = theRepository;
		status = theRepository.getStatus();
	}
	
	public HBaseDataRepository getRepository( ) {
		return repository;
	}
	
	@Override
	public <K, T> T getObject( K theKey, Class<T> theType  ) {
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkNotNull( theKey, "need a key" );

		T rootInstance = null;
		
		status.recordGet();
		long startTime = System.nanoTime();
		try {
			// we need to get the type
			HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
			StorageType storageType = tableMap.getStorageType();
			HBaseKeyMap keyMap = tableMap.getKey(); // make sure the keys work?
			
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			try {
				Get get = new Get( ( byte[] )keyMap.getKeyTranslator().translate( theKey ) ); // just looking up by key
				Result result = table.get( get );
				rootInstance = getHelper( result, tableMap, storageType );
				status.recordGetExecution( 1, System.nanoTime() - startTime );
			} finally {
				table.close( );
			}
		} catch( IOException e ) {
			status.recordGetError();
			throw new StorageException( e );
		}
		
		return rootInstance;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getObjects( Query<T> theQuery ) {
		Preconditions.checkNotNull( theQuery, "need a query" );
		
		status.recordGet();
		long startTime = System.nanoTime();

		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theQuery.getType() );
		StorageType storageType = tableMap.getStorageType();
		ArrayList<T> objects = new ArrayList<T>( );

		try {
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			try {
				Scan scan = generateScan( theQuery, tableMap );
				T object;
				ResultScanner scanner = table.getScanner( scan );
				try {
					for( Result result : scanner ) {
						object = ( T )this.getHelper( result, tableMap, storageType ); // TODO: really don't want to have to cast
						if( object != null ) {
							objects.add( object );
						}
					}
					status.recordGetExecution( objects.size(), System.nanoTime() - startTime );
				} finally {
					scanner.close( );
				}
			} finally {
				table.close();
			}
		} catch( IOException e ) {
			status.recordGetError();
			throw new StorageException( e );
		}
		
		return objects;
	}

	@SuppressWarnings("unchecked")
	private <T> T getHelper( Result theResult, HBaseTableMap theTable, StorageType theStorageType ) {
		//TODO: do not be strict regarding matching fields
		T rootInstance = null;
		HashMap<String, Object> referencedItems = null; // TODO: this is a problem, it stores items based on column name, not family/column, so collisions can occur
		
		KeyValue[] values = theResult.raw( );
		KeyValue value;
		String column;
		String family;
		
		HBaseFamilyMap familyMap;
		
		ColumnReference columnReference = null;
		Object facetInstance = null;
		StorageField facetReference = null;
		
		if( values.length > 0 ) {
			rootInstance = ( T )theStorageType.newInstance();

			for( int count = 0; count < values.length; count += 1  ) {
				value = values[ count ];
				family = HBaseTranslationFacility.translateName( value.getFamily() );
				column = HBaseTranslationFacility.translateName( value.getQualifier() );
				
				familyMap = theTable.getFamily( family );
				if( familyMap == null ) {
					// we skip fields we don't know about for compatibility reasons
					logger.debug( "Ignoring unknown family '{}' (during request for column '{}')", family, column );
					continue;
				}
				columnReference = familyMap.getColumn( column ); // TODO: lookup via bytes (regex for bytes?)
				if( columnReference == null ) {
					// we skip fields we don't know about for compatibility reasons
					logger.debug( "Ignoring unknown column '{}.{}'", family, column );
					continue;
				}
				facetReference = columnReference.getFacetReference();
				if( facetReference == null ) {
					// this means we are talking to the root instance itself
					// this is the easy case, this isn't a facet, so get the data
					// and slap it into the object
					columnReference.getRealColumn().setData( rootInstance, value.getValue( ) );

				} else {
					facetInstance = null;
					if( referencedItems == null ) {
						// so first time we have had a non-root instance referred to, so create the holder
						referencedItems = new HashMap<String, Object>( 2 );
					}
					if( columnReference.getRealColumn() != null ) {
						// for member-based facets, we have to see if we have already seen the
						// column containing parent already (due to a previous column)
						String referenceLookup = family + ":" + columnReference.getReferenceName( column );
						facetInstance = referencedItems.get( referenceLookup );
						if( facetInstance == null ) {
							// create the instance to use for this case
							facetInstance = facetReference.getFacetReferenceType().newInstance();
							// now set the parent field
							facetReference.getFacetReferenceType().getParentField().setData( facetInstance, rootInstance );
							// save for later lookup
							referencedItems.put( referenceLookup, facetInstance );
							// followed by saving for later lookup using the column name
							if( facetReference.isCollection( ) ) { // TODO: this doesn't handle array's (isCollection means both)
								// this is a collection of facet instances, so we ultimately need 
								// to save in collection and set the collection on the parent
								Collection<Object> facetCollection = ( Collection<Object> )facetReference.getData( rootInstance );
								// see if we previously created the collection
								if( facetCollection == null ) {
									// didn't create it yet, so create and save
									facetCollection = new ArrayList<Object>( );
									facetReference.setData( rootInstance, facetCollection );
								}
								// store in the collection
								facetCollection.add( facetInstance );
							} else {
								// then we need, since it is just created, on the root instance, the facet as it's value
								facetReference.setData( rootInstance, facetInstance );
							}
						}
						// for all member-based facet cases, we need to now set the value on the facet instance's field
						columnReference.getRealColumn().setData( facetInstance, value.getValue() );
					
					} else { 
						// for object-based facets (w/ virtual columns), we can always assume the reference coming
						// in is unique and doesn't need look ups since only one could be stored with that column
						
						// first translate the instance to use
						facetInstance = columnReference.getVirtualColumn().getContainingFamilyPart().fromBytes( value.getValue( ) );
						// and then set the parent field
						facetReference.getFacetReferenceType().getParentField().setData( facetInstance, rootInstance );
						// followed by saving for later lookup using the column name
						referencedItems.put( family + ":" + column, facetInstance );
						// afterwards we have differences depending on if collection, array or single instance
						if( facetReference.isCollection( ) ) { // TODO: this doesn't handle array's (isCollection means both)
							// this is a collection of facet instances, so we ultimately need 
							// to save in collection and set the collection on the parent
							Collection<Object> facetCollection = ( Collection<Object> )facetReference.getData( rootInstance );
							// see if we previously created the collection
							if( facetCollection == null ) {
								// we didn't create it yet, so create and save
								facetCollection = new ArrayList<Object>( );
								facetReference.setData( rootInstance, facetCollection );
							}
							// and then save the instance in the collection 
							facetCollection.add( facetInstance );
						} else {
							// for non-collections we need to save the instance on the root
							facetReference.setData( rootInstance, facetInstance );
						}
					}
				}
			}
			if( referencedItems != null ) {
				// now for each item loaded we hit the lifecycle handler
				for( Object facetObject : referencedItems.values() ) {
					//TODO: this is sending the wrong type in
					this.getRepository().getLifecycleFacility().postGetObject( facetObject, theStorageType, this );
				}
			}
			this.getRepository().getLifecycleFacility().postGetObject( rootInstance, theStorageType, this ); // TODO: pass in the lifecycle facility so we don't keep doing hash lookups
		}
		return rootInstance;
	}
	
	/**
	 * Helper method that generates a scan object for
	 * use in a query
	 * @param theQuery the query to generate a scan for
	 * @param theTableMap the table the query is against
	 * @return the generated scan
	 */
	private <T> Scan generateScan( Query<T> theQuery, HBaseTableMap theTableMap ) {
		Scan scan = new Scan( );
		
		for( Filter filter : theQuery.getFilters( ) ) {
			if( filter instanceof RowFilter ) {
				RowFilter<?> rowFilter = ( RowFilter<?> )filter;
				HBaseKeyMap keyMap = theTableMap.getKey();
				
				if( rowFilter.getLimit() > 0 ) {
					scan.setFilter( new PageFilter( rowFilter.getLimit( ) ) );
				}
				if( rowFilter.getEndKey() != null ) {
					scan.setStopRow( ( byte[] )keyMap.getKeyTranslator().translate( rowFilter.getEndKey( ) ) );
				} 
				if( rowFilter.getStartKey() != null ) {
					scan.setStartRow( ( byte[] )keyMap.getKeyTranslator().translate( rowFilter.getStartKey( ) ) );						
				}
			}
		}
		return scan;
	}
	
	
	/**
	 * Puts a single object, of the specified type, into the system
	 * if, and only if, the optimistic lock value, as passed in, is the
	 * same as the value in the database.
	 * The method also ensures the lifecycle handling occurs.
	 * @param theLockCompareValue the value to compare against in the db to ensure there hasn't been a change
	 */
	public <T> boolean checkedPutObject( T theObject, Class<T> theType, Object theLockCompareValue ) {
		Preconditions.checkNotNull( theObject, "need an object" );
		Preconditions.checkNotNull( theType, "need a type" );
		boolean wasPut = false;

		status.recordPut();
		long startTime = System.nanoTime();

		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
		StorageType storageType = tableMap.getStorageType();
		StorageField lockField = storageType.getOptimisticLockField();
		if( lockField == null ) {
			throw new IllegalArgumentException( String.format( "Class '%s' does not have a optimistic lock field.", theType.getSimpleName( ) ) );
		} else {
			HBaseKeyMap keyMap = tableMap.getKey();
			HBaseFamilyPartMap lockFamilyPartMap = tableMap.getFamilyPart( storageType );
			HBaseColumnMap lockColumnMap = lockFamilyPartMap.getRealColumn( lockField.getName( ) );
			try {
				HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
				try {
					byte[] keyData = keyMap.getData( theObject );
					Put put = new Put( keyData ); 
					putHelper( theObject, null, tableMap, storageType, put ); // handles lifecycle here
					wasPut = table.checkAndPut(
							keyData,  // the row key is the same row as the put itself
							lockFamilyPartMap.getContainingFamily().getNameBytes(), // need the family name
							lockColumnMap.getNameBytes( null ), // need the column name
							( byte[] )lockColumnMap.getDataSite().getGetterTranslator( ).translate( theLockCompareValue ), // the value to compare is the old value passed in
							put );
					if( wasPut ) {
						status.recordCheckedPutSuccess( System.nanoTime() - startTime );
					} else {
						status.recordCheckedPutFailure( System.nanoTime() - startTime );
					}
					
					return wasPut;
				} finally {
					table.close();
				}
			} catch( IOException e ) {
				status.recordPutError();
				throw new StorageException( e );
			}
		}
	}
	
	/**
	 * Puts a single object, of the specified type, into the system
	 * and ensures the lifecycle handling occurs.
	 */
	@Override
	public <T> void putObject( T theObject, Class<T> theType ) {
		Preconditions.checkNotNull( theObject, "need an object" );
		Preconditions.checkNotNull( theType, "need a type" );

		status.recordPut();
		long startTime = System.nanoTime();

		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
		StorageType storageType = tableMap.getStorageType();
		HBaseKeyMap keyMap = tableMap.getKey();

		try {
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			try {
				Put put = new Put( keyMap.getData( theObject ) ); 
				putHelper( theObject, null, tableMap, storageType, put ); // handles lifecycle here
				table.put( put );
				status.recordPutExecution( 1, System.nanoTime() - startTime );
			} finally {
				table.close();
			}
		} catch( IOException e ) {
			status.recordPutError();
			throw new StorageException( e );
		}
	}
	
	/**
	 * Puts a collection of the objects, of the specified type, into the system
 	 * and ensures the lifecycle handling occurs.
	 */
	@Override
	public <T> void putObjects(Collection<T> theObjects, Class<T> theType ) {
		Preconditions.checkNotNull( theObjects, "need objects" );
		Preconditions.checkNotNull( theType, "need a type" );

		status.recordPut();
		long startTime = System.nanoTime();

		// this makes sure we have a root type
		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
		HBaseKeyMap keyMap = tableMap.getKey();
		StorageType storageType = tableMap.getStorageType();
		try {
					
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			try {
				List<Put> puts = new ArrayList<Put>( theObjects.size() ) ;
				Put put;
				for( T object : theObjects ) {
					put = new Put( keyMap.getData( object ) ); 
					putHelper( object, null, tableMap, storageType, put ); // handles lifecycle here
					puts.add( put );
				}
				table.put( puts );
				status.recordPutExecution( theObjects.size(), System.nanoTime() - startTime );
			} finally {
				table.close();
			}
		} catch( IOException e ) {
			status.recordPutError();
			throw new StorageException( e );
		}
	}
	
	/**
	 * A private helper class for putting data into Hbase. It ensures that we have facets
	 * properly taken care of and that lifecycle is taken care of.
	 * @param theInstance the object being saved
	 * @param theColumnPrefix the text value prefix for all columns of the object being saved
	 * @param theTableMap the table being saved into
	 * @param theStorageType the storage type of the object being saved
	 * @param thePut the HBase put object to save fields into
	 */
	private void putHelper( Object theInstance, String theColumnPrefix, HBaseTableMap theTableMap, StorageType theStorageType, Put thePut ) {
		Collection<HBaseColumnMap> columns = theTableMap.getFamilyPart( theStorageType ).getRealColumns();

		this.getRepository().getLifecycleFacility().prePutObject( theInstance, theStorageType, this );
		// first we save all of the immediate columns
		for( HBaseColumnMap column : columns ) {
			thePut.add( 
					column.getContainingFamilyPart( ).getContainingFamily().getNameBytes(), // the family // TODO: could pass in the family here
					column.getNameBytes( theColumnPrefix ), // the column // TODO: consider a way to get the column prefix as bytes, and add that to other bytes
					column.getData( theInstance ) ); // the value
		}
		this.getRepository().getLifecycleFacility().postPutObject( theInstance, theStorageType, this ); // TODO: pass in the facility so no hash lookups each time

		// next we save we have any facets that were referred to that 
		// should therefore be saved at this time.
		for( StorageField facetField : theStorageType.getFacetFields( ) ) {
			Object facetInstance = facetField.getData( theInstance );
			
			if( facetField.getFieldMode() == FieldMode.FACET_OBJECT ) {
				HBaseFamilyPartMap facetFamilyPart = theTableMap.getFamilyPart( facetField.getFacetReferenceType( ) ); // TODO: don't like this look up here
				// we have a facet object field
				if( facetField.isCollection( ) ) { // TODO: handle arrays
					if( facetInstance != null ) {
						for( Object elementInstance : ( Collection<?> )facetInstance ) {
							// each element instance becomes a column where
							// a) the family is based on the referenced type's family
							// b) the column name is calculated the the reference
							// c) the value is translated 
							thePut.add(
									facetFamilyPart.getContainingFamily().getNameBytes(),
									HBaseTranslationFacility.translateName( facetField.calculateFacetName( elementInstance ) ),
									facetFamilyPart.toBytes( elementInstance) );
						}
					} // if null, cannot do anything
					
				} else {
					// this will handle null and non-null
					thePut.add(
							facetFamilyPart.getContainingFamily().getNameBytes(),
							HBaseTranslationFacility.translateName( facetField.calculateFacetName( facetInstance ) ),
							facetFamilyPart.toBytes( facetInstance) );
				}
				
			} else {
				// we have a facet member field
				// this supports being recursive (facets referring to facets, 
				// but the rest of the system doesn't support it yet
				if( facetInstance != null ) {
					if( facetField.isCollection( ) ) { // TODO: this doesn't handle array's (isCollection means both)
						for( Object elementInstance : ( Collection<?> )facetInstance ) {
							putHelper( elementInstance, facetField.calculateFacetName( elementInstance ), theTableMap, facetField.getFacetReferenceType( ),thePut );
						}
					//} else if( facetField.isArray ) {
					//	//Arrays.
					} else {
						putHelper( facetInstance, facetField.calculateFacetName( facetInstance ), theTableMap, facetField.getFacetReferenceType( ),thePut );
					}
				} // we do not do anything if the facet instance is null in this case
			}
		}
	}

	/**
	 * Deletes a single object of the specified type
	 * and ensures the lifecycle handling occurs.
	 */
	@Override
	public <O> void deleteObject( O theObject, Class<O> theType ) {
		Preconditions.checkNotNull( theObject, "need an object" );
		Preconditions.checkNotNull( theType, "need a type" );

		status.recordDelete();
		long startTime = System.nanoTime();

		// this call will guarantee it is a root type
		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
		HBaseKeyMap keyMap = tableMap.getKey();
		StorageType storageType = tableMap.getStorageType();
		try {
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			Delete delete = new Delete( keyMap.getData( theObject ) );;
			try {
				this.getRepository().getLifecycleFacility().preDeleteObject( theObject, storageType, this );
				table.delete( delete );
				// TODO: (need a helper) pull the facets out and do pre/post delete on them (before the main object ideally as well, so runs backward to put)
				status.recordDeleteExecution( 1, System.nanoTime() - startTime );
				this.getRepository().getLifecycleFacility().postDeleteObject( theObject, storageType, this );
			} finally {
				table.close();
			}
		} catch( IOException e ) {
			status.recordDeleteError();
			throw new StorageException( e );
		}
	}

	/**
	 * Deletes the collection of objects of the specified type 
	 * and ensures the lifecycle handling occurs.
	 */
	@Override
	public <O> void deleteObjects(Collection<O> theObjects, Class<O> theType ) {
		Preconditions.checkNotNull( theObjects, "need objects" );
		Preconditions.checkNotNull( theType, "need a type" );

		status.recordDelete();
		long startTime = System.nanoTime();

		// this call will guarantee it is a root type
		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
		HBaseKeyMap keyMap = tableMap.getKey();
		StorageType storageType = tableMap.getStorageType();
		try {
			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
			
			List<Delete> deletes = new ArrayList<Delete>( theObjects.size( ) );
			Delete delete;
			try {
				for( O object : theObjects ) {
					// ensure we aren't trying to delete a null
					if( object != null ) {
						this.getRepository().getLifecycleFacility().preDeleteObject( object, storageType, this );
						delete = new Delete( keyMap.getData( object ) );
						// TODO: (need a helper) pull the facets out and do pre/post delete on them (before the main object ideally as well, so runs backward to put)
						deletes.add( delete );
						this.getRepository().getLifecycleFacility().postDeleteObject( object, storageType, this );
					}
				}
				table.delete( deletes );
				status.recordDeleteExecution( theObjects.size(), System.nanoTime() - startTime );
			} finally {
				table.close();
			}
		} catch( IOException e ) {
			status.recordDeleteError();
			throw new StorageException( e );
		}
	}
	
//	private void deleteHelper( Object theInstance, HBaseTableMap theTableMap, StorageType theStorageType, Put thePut ) {
//		for( StorageField facetField : theStorageType.getFacetFields( ) ) {
//			Object facetInstance = facetField.getData( theInstance );
//			
//			// get the value
//			// if a single value then call the pre/post delete
//			// if a collection, then iterate and pre/post delete them
//		}
//		
//	}
	

//	/**
//	 * Deletes a single object of the specified type
//	 * and ensures the lifecycle handling occurs.
//	 */
//	@Override
//	public <O> void deleteObject( O theObject, Class<O> theType, String theParentMember ) {
//		Preconditions.checkNotNull( theObject, "need an object" );
//		Preconditions.checkNotNull( theType, "need a type" );
//		Preconditions.checkArgument( !Strings.isNullOrEmpty( theParentMember ), "need a parent member if trying to delete a facet object" );
//
//		status.recordDelete();
//		long startTime = System.nanoTime();
//
//		// this call will guarantee it is a root type
//		HBaseTableMap tableMap = this.repository.getMapFacility().generateTableMap( theType );
//		HBaseKeyMap keyMap = tableMap.getKey();
//		StorageType storageType = tableMap.getStorageType();
//		try {
//			HTableInterface table = this.repository.requestTable( tableMap.getNameBytes( ) );
//			Delete delete = new Delete( keyMap.getData( theObject ) );;
//			try {
//				this.getRepository().getLifecycleFacility().preDeleteObject( theObject, storageType, this );
//				table.delete( delete );
//				status.recordDeleteExecution( 1, System.nanoTime() - startTime );
//				this.getRepository().getLifecycleFacility().postDeleteObject( theObject, storageType, this );
//			} finally {
//				table.close();
//			}
//		} catch( IOException e ) {
//			status.recordDeleteError();
//			throw new StorageException( e );
//		}
//	}
	
	// TODO:
	// get/put/delete facets
	// the problem is, the facet is defined by 
	//    - the key of the parent
	//    - the field in the parent
	// so you can image it could
	//    - take the parent; OR
	//    - take key/field
}
