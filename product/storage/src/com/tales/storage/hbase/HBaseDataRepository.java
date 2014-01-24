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

import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;

import com.google.common.base.Preconditions;
import com.tales.parts.naming.LowerCaseEntityNameValidator;
import com.tales.parts.naming.NameValidator;
import com.tales.parts.naming.NopNameValidator;
import com.tales.serialization.Readability;
import com.tales.serialization.json.JsonTranslationFacility;
import com.tales.storage.DataRepository;
import com.tales.storage.StorageException;
import com.tales.storage.StorageStatus;
import com.tales.storage.StorageTypeFacility;
import com.tales.storage.StorageTypeSource;
import com.tales.storage.facilities.DefinitionFacility;
import com.tales.storage.facilities.LifecycleFacility;
import com.tales.storage.facilities.StandardLifecycleFacility;
import com.tales.system.Facility;
import com.tales.system.SimpleFacilityManager;

public class HBaseDataRepository implements DataRepository<HBaseDataRepository, HBaseDataContext> {
	private final SimpleFacilityManager facilityManager;
	// TODO: consider having a hard limit that this will return on queries, regardless of what anyone else says

	private final StorageStatus status = new StorageStatus();
	
	private final Configuration configuration;
	private final HTablePool tablePool;
	
	public HBaseDataRepository( Configuration theConfiguration, int theTablePoolsSize ) {
		Preconditions.checkNotNull( theConfiguration, "need configuration" );
		Preconditions.checkNotNull( theTablePoolsSize > 0, "need a pool size greater than 0" );
		
		configuration = theConfiguration;
		tablePool = new HTablePool( configuration, theTablePoolsSize );

		facilityManager = new SimpleFacilityManager();
		
		StorageTypeFacility storageTypeFacility = new StorageTypeFacility( );
		NameValidator typeNameValidator = new NopNameValidator( );
		NameValidator memberNameValidator = new LowerCaseEntityNameValidator( );
		JsonTranslationFacility jsonFacility = new JsonTranslationFacility( 
				new StorageTypeSource( storageTypeFacility ),
				Readability.MACHINE,
				typeNameValidator,
				memberNameValidator );
		HBaseTranslationFacility mapFacility = new HBaseTranslationFacility( storageTypeFacility, jsonFacility );

		facilityManager.addFacility( StorageTypeFacility.class, storageTypeFacility );
		facilityManager.addFacility( HBaseTranslationFacility.class, mapFacility );
		facilityManager.addFacility( DefinitionFacility.class, new HBaseDefinitionFacility( ) );
		facilityManager.addFacility( LifecycleFacility.class, new StandardLifecycleFacility<HBaseDataRepository, HBaseDataContext>( ) );
	}

	public HBaseDataRepository( Configuration theConfiguration, int theTablePoolsSize, HBaseTranslationFacility theTranslationFacility ) {
		Preconditions.checkNotNull( theConfiguration, "need configuration" );
		Preconditions.checkNotNull( theTablePoolsSize > 0, "need a pool size greater than 0" );
		Preconditions.checkNotNull( theTranslationFacility , "need a translation facility" );
		
		configuration = theConfiguration;
		tablePool = new HTablePool( configuration, theTablePoolsSize );

		facilityManager = new SimpleFacilityManager();
		
		facilityManager.addFacility( StorageTypeFacility.class, theTranslationFacility.getStorageTypeFacility() );
		facilityManager.addFacility( HBaseTranslationFacility.class, theTranslationFacility );
		facilityManager.addFacility( DefinitionFacility.class, new HBaseDefinitionFacility( ) );
		facilityManager.addFacility( LifecycleFacility.class, new StandardLifecycleFacility<HBaseDataRepository, HBaseDataContext>( ) );
	}
	
	public StorageStatus getStatus( ) {
		return status;
	}
	
	@Override
	public HBaseDataContext createContext() {
		return new HBaseDataContext( this );
	}
	
	public HBaseDefinitionFacility getDefinitionFacility( ) {
		return ( HBaseDefinitionFacility )facilityManager.getFacility( DefinitionFacility.class );
	}
	
	@SuppressWarnings("unchecked")
	public LifecycleFacility<HBaseDataRepository, HBaseDataContext> getLifecycleFacility( ) {
		return ( LifecycleFacility<HBaseDataRepository, HBaseDataContext> )facilityManager.getFacility( LifecycleFacility.class );
	}
	
	public HBaseTranslationFacility getMapFacility( ) {
		return this.facilityManager.getFacility( HBaseTranslationFacility.class );
	}
	
	public StorageTypeFacility getStorageTypeFacility( ) {
		return this.facilityManager.getFacility( StorageTypeFacility.class );
	}
	
	/**
	 * This is called to get a table interface so that calls can be made to
	 * get, put or delete from the table. This makes use of the table
	 * pool that was created.
	 * @param theTableName the byte version of the name of the table
	 * @return the table interface
	 */
	HTableInterface requestTable( byte[] theTableName ) {
		return tablePool.getTable( theTableName );
	}
	
	HBaseAdmin requestAdmin( ) {
		try {
			return new HBaseAdmin( configuration );
		} catch (MasterNotRunningException e) {
			throw new StorageException( e );
		} catch (ZooKeeperConnectionException e) {
			throw new StorageException( e );
		}
	}


	@Override
	public Collection<Facility> getFacilities() {
		return this.facilityManager.getFacilities();
	}


	@Override
	public <F extends Facility> F getFacility(Class<F> theFacilityType) {
		return this.facilityManager.getFacility(theFacilityType );
	}


	@Override
	public <F extends Facility> void addFacility(Class<F> theFacilityType,F theFacilityInstance) {
		this.facilityManager.addFacility(theFacilityType, theFacilityInstance);
	}


	@Override
	public <F extends Facility> boolean removeFacility(Class<F> theFacilityType) {
		return this.facilityManager.removeFacility(theFacilityType);
	}

}
