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
package com.tales.parts.sites;

import com.google.common.base.Preconditions;
import com.tales.parts.translators.Translator;

/**
 * A type of {@link DataSite} that wraps another {@link DataSite} but translators
 * the get and set class using specified the {@link Translator}s.
 * @author jmolnar
 *
 */
public class TranslatedDataSite implements DataSite {
	private final Translator getterTranslator;
	private final Translator setterTranslator;
	private final DataSite dataSite;
	
	/**
	 * Constructor taking the need parameters.
	 * @param theDataSite the data site to wrap
	 * @param theGetterTranslator the translator used when data is retrieved from the wrapped data site
	 * @param theSetterTranslator the translator used when data is to be set on the wrapped data site
	 */
	public TranslatedDataSite( DataSite theDataSite, Translator theGetterTranslator, Translator theSetterTranslator ) {
		Preconditions.checkNotNull( theDataSite, "need a data site" );
		Preconditions.checkNotNull( theGetterTranslator, "need a getter translator" );
		Preconditions.checkNotNull( theSetterTranslator, "need a setter translator" );
		
		dataSite = theDataSite;
		getterTranslator = theGetterTranslator;
		setterTranslator = theSetterTranslator;
	}
	
	/**
	 * Returns the data site this data site wrap.
	 * @return the wrapped data site
	 */
	public DataSite getDataSite( ) {
		return dataSite;
	}
	
	/**
	 * The type of the data, which is the data type from the 
	 * wrapped data type
	 * @return class representing the type of data
	 */
	public Class<?> getType( ) {
		return this.dataSite.getType( );
	}

	
	/**
	 * Returns the translator used to translate the data that is returned from
	 * the attached data site.
	 * @return the getter translator
	 */
	public Translator getGetterTranslator( ) {
		return this.getterTranslator;
	}
	
	/**
	 * Returns the translator used to translate the data before setting on the 
	 * attached data site.
	 * @return the setter translator
	 */
	public Translator getSetterTranslator( ) {
		return this.setterTranslator;
	}
	
	/**
	 * Indicates if this particular data site is read-only.
	 * @return
	 */
	public boolean isReadOnly( ) {
		return dataSite.isReadOnly();
	}
	/**
	 * Gets data from a source object.
	 * @param theSource the source object to get data from.
	 * @return the value from the source
	 */
	public Object getData( Object theSource ) {
		return getterTranslator.translate( dataSite.getData( theSource ) );
	}
	
	/**
	 * Sets data on a sink object.
	 * @param theSink the object to set a value on
	 * @param theValue the value to set on the sink
	 */
	public void setData( Object theSink, Object theValue ) {
		dataSite.setData( theSink, setterTranslator.translate( theValue ) );
	}
}
