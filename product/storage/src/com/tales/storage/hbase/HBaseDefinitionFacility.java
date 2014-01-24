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

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import com.tales.storage.StorageException;
import com.tales.storage.StorageType;
import com.tales.storage.facilities.DefinitionFacility;

public class HBaseDefinitionFacility implements DefinitionFacility<HBaseDataRepository, HBaseDataContext> {


	public <T> void createDefinition( Class<T> theType, HBaseDataContext theContext ) {
		try {
			StorageType storageType = getStorageType( theType, theContext );
			HBaseTableMap tableMap = theContext.getRepository().getMapFacility().generateTableMap( storageType );
			HBaseAdmin admin = theContext.getRepository().requestAdmin();
			
			// TODO: check if it exists
			//       if it exists, check for families that do not exist (this can happen from the table descriptor)
			
			HTableDescriptor tableDescriptor = new HTableDescriptor( tableMap.getNameBytes( ) );
			admin.createTable( tableDescriptor );
			admin.disableTable( tableMap.getName( ) );
			
			HColumnDescriptor columnDescriptor;
			
			for( HBaseFamilyMap family : tableMap.getFamilies( ) ) {
				columnDescriptor = new HColumnDescriptor( family.getNameBytes( ) );
				admin.addColumn( tableMap.getName( ), columnDescriptor );
			}
			admin.enableTable( tableMap.getName( ) );
			
		} catch (IOException e) {
			throw new StorageException( String.format( "error creating definition for '%s'.", theType.getName( ) ), e );
		}
	}

	public <T> void deleteDefinition( Class<T> theType, HBaseDataContext theContext) {
		throw new UnsupportedOperationException( );
	}

	public <T> boolean definitionExists( Class<T> theType, HBaseDataContext theContext) {
		try {
			StorageType storageType = getStorageType( theType, theContext );
			HBaseTableMap tableMap = theContext.getRepository().getMapFacility().generateTableMap( storageType );
			HBaseAdmin admin = theContext.getRepository().requestAdmin();
			return admin.tableExists( tableMap.getName( ) );
		} catch (IOException e) {
			throw new StorageException( String.format( "unable to check for definition of '%s'.", theType.getName( ) ), e );
		}
	}
	
	private <T> StorageType getStorageType( Class<T> theType, HBaseDataContext theContext ) {
		StorageType storageType = theContext.getRepository().getStorageTypeFacility().generateType( theType );
		while( storageType.isFacet() ) {
			storageType = storageType.getParentField().getParentType();
		}
		return storageType;
	}
}
