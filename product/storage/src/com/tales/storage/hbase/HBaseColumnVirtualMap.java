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

import com.google.common.base.Preconditions;

import com.tales.storage.StorageField;

/**
 * The class that represents a column that is not
 * back directly by a storage field. It represents
 * a facet that is being stored as a single column. 
 * @author jmolnar
 *
 */
public class HBaseColumnVirtualMap {
	private final HBaseFamilyPartMap containingFamilyPart;
	private final StorageField referringField;

	// TODO: need translators, unless we grab from the family part

	/**
	 * The constructor taking field and the translator to translate to/from the field.
	 * @param theFieldInfo the field
	 * @param theDataSite the site that translates the data
	 */
	public HBaseColumnVirtualMap( StorageField theReferringField, HBaseFamilyPartMap theContainingFamilyPart ) {
		Preconditions.checkNotNull( theReferringField, "need the referring field" );
		Preconditions.checkNotNull( theContainingFamilyPart, "need the column family part" );

		referringField = theReferringField;
		containingFamilyPart = theContainingFamilyPart;
	}
	
	
	/**
	 * The family this column is contained within.
	 * @return the family the column is contained within
	 */
	public HBaseFamilyPartMap getContainingFamilyPart( ) {
		return containingFamilyPart;
	}
	
	/**
	 * The referring field this virtual column represents.
	 * @return the referring field
	 */
	public StorageField getReferringField( ) {
		return referringField;
	}
}
