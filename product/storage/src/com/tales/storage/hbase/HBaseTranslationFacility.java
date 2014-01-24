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
package com.tales.storage.hbase;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;

import com.google.common.base.Preconditions;

import com.tales.parts.sites.TranslatedDataSite;
import com.tales.parts.translators.Translator;
import com.tales.serialization.json.JsonTranslationFacility;
import com.tales.serialization.json.translators.ChainToJsonElementToStringTranslator;
import com.tales.serialization.json.translators.StringToJsonElementToChainTranslator;
import com.tales.storage.CompoundIdField;
import com.tales.storage.StorageField;
import com.tales.storage.StorageType;
import com.tales.storage.StorageTypeFacility;
import com.tales.storage.hbase.translators.BooleanToBytesTranslator;
import com.tales.storage.hbase.translators.ByteArrayToBytesTranslator;
import com.tales.storage.hbase.translators.BytesToBooleanTranslator;
import com.tales.storage.hbase.translators.BytesToByteArrayTranslator;
import com.tales.storage.hbase.translators.BytesToDateTimeTranslator;
import com.tales.storage.hbase.translators.BytesToDoubleTranslator;
import com.tales.storage.hbase.translators.BytesToEnumTranslator;
import com.tales.storage.hbase.translators.BytesToFloatTranslator;
import com.tales.storage.hbase.translators.BytesToIntegerTranslator;
import com.tales.storage.hbase.translators.BytesToLongTranslator;
import com.tales.storage.hbase.translators.BytesToStringToChainTranslator;
import com.tales.storage.hbase.translators.BytesToStringTranslator;
import com.tales.storage.hbase.translators.DateTimeToBytesTranslator;
import com.tales.storage.hbase.translators.DoubleToBytesTranslator;
import com.tales.storage.hbase.translators.EnumToBytesTranslator;
import com.tales.storage.hbase.translators.FloatToBytesTranslator;
import com.tales.storage.hbase.translators.IntegerToBytesTranslator;
import com.tales.storage.hbase.translators.KeyToBytesTranslator;
import com.tales.storage.hbase.translators.LongToBytesTranslator;
import com.tales.storage.hbase.translators.ChainToStringToBytesTranslator;
import com.tales.storage.hbase.translators.StringToBytesTranslator;
import com.tales.system.Facility;

/**
 * This is a class that manages table definitions for hbase as well as
 * translators for converting data to and from hbase.
 * @author jmolnar
 *
 */
public class HBaseTranslationFacility implements Facility {
	private final StorageTypeFacility storageFacility;
	private final JsonTranslationFacility jsonTranslationFacility; // TODO: this is temporary
    private final Map<StorageType, HBaseTableMap> storageToTableMaps = new ConcurrentHashMap< StorageType, HBaseTableMap>( 16, 0.75f, 1 );
    private final Map<Class<?>, HBaseTableMap> typeToTableMaps = new ConcurrentHashMap<Class<?>, HBaseTableMap>( 16, 0.75f, 1 );

	private final Map< Class<?>, Translator> fromByteTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );
	private final Map< Class<?>, Translator> toByteTranslators = new ConcurrentHashMap<Class<?>, Translator>( 16, 0.75f, 1 );

	public HBaseTranslationFacility( StorageTypeFacility theStorageFacility, JsonTranslationFacility theJsonTranslationFacility ) { //TODO: temp passing in the translation facility so that we can use to create translators in byte translators
		Preconditions.checkNotNull( theStorageFacility, "need a storage facility ");
		storageFacility = theStorageFacility;
		jsonTranslationFacility = theJsonTranslationFacility; // TODO: this is temporary
		
		// translators for coming from hbase
		Translator fromByteIntegerTranslator = new BytesToIntegerTranslator( null );
		fromByteTranslators.put( Integer.class, fromByteIntegerTranslator );
		fromByteTranslators.put( int.class, fromByteIntegerTranslator );

		Translator fromByteLongTranslator = new BytesToLongTranslator( null );
		fromByteTranslators.put( Long.class, fromByteLongTranslator );
		fromByteTranslators.put( long.class, fromByteLongTranslator );

		Translator fromByteFloatTranslator = new BytesToFloatTranslator( null );
		fromByteTranslators.put( Float.class, fromByteFloatTranslator );
		fromByteTranslators.put( float.class, fromByteFloatTranslator );

		Translator fromByteDoubleTranslator = new BytesToDoubleTranslator( null );
		fromByteTranslators.put( Double.class, fromByteDoubleTranslator );
		fromByteTranslators.put( double.class, fromByteDoubleTranslator );

		Translator fromByteBooleanTranslator = new BytesToBooleanTranslator( null );
		fromByteTranslators.put( Boolean.class, fromByteBooleanTranslator );
		fromByteTranslators.put( boolean.class, fromByteBooleanTranslator );

		Translator fromByteDateTimeTranslator = new BytesToDateTimeTranslator( true, null );
		fromByteTranslators.put( DateTime.class, fromByteDateTimeTranslator );

		Translator fromByteStringTranslator = new BytesToStringTranslator( null );
		fromByteTranslators.put( String.class, fromByteStringTranslator );
		
		Translator fromByteByteArrayTranslator = new BytesToByteArrayTranslator( null ); 
		fromByteTranslators.put( byte[].class, fromByteByteArrayTranslator );
		
		need to add the big decimal support for storage here
		
		// translators for going to hbase
		
		Translator toByteIntegerTranslator = new IntegerToBytesTranslator( null );
		toByteTranslators.put( Integer.class, toByteIntegerTranslator );
		toByteTranslators.put( int.class, toByteIntegerTranslator );

		Translator toByteLongTranslator = new LongToBytesTranslator( null );
		toByteTranslators.put( Long.class, toByteLongTranslator );
		toByteTranslators.put( long.class, toByteLongTranslator );

		Translator toByteFloatTranslator = new FloatToBytesTranslator( null );
		toByteTranslators.put( Float.class, toByteFloatTranslator );
		toByteTranslators.put( float.class, toByteFloatTranslator );

		Translator toByteDoubleTranslator = new DoubleToBytesTranslator( null );
		toByteTranslators.put( Double.class, toByteDoubleTranslator );
		toByteTranslators.put( double.class, toByteDoubleTranslator );

		Translator toByteBooleanTranslator = new BooleanToBytesTranslator( null );
		toByteTranslators.put( Boolean.class, toByteBooleanTranslator );
		toByteTranslators.put( boolean.class, toByteBooleanTranslator );

		Translator toByteDateTimeTranslator = new DateTimeToBytesTranslator( true, null );
		toByteTranslators.put( DateTime.class, toByteDateTimeTranslator );

		Translator toByteStringTranslator = new StringToBytesTranslator( null );
		toByteTranslators.put( String.class, toByteStringTranslator );
		
		Translator toByteByteArrayTranslator = new ByteArrayToBytesTranslator( null ); 
		toByteTranslators.put( byte[].class, toByteByteArrayTranslator );
		
		
	}

	/**
	 * Returns the storage type facility being used.
	 * @return the storage type facility
	 */
	public StorageTypeFacility getStorageTypeFacility( ) {
		return this.storageFacility;
	}

	/***
	 * This method is used to add translators into the system for ensuring proper conversion 
	 * into the storage system itself. This is used for both columns and row keys.
	 * The translators are expected to get or set byte representations (the native hbase type).
	 * If creating byte translators are difficult to crated then BytesToStringToChainTranslator and 
	 * ChainToStringToBytesTranslator classes can be used to wrap translators that can convert
	 * from/to strings.
	 * @param theClass the class being mapped
	 * @param fromByteTranslator the translator that takes bytes and create the proper type
	 * @param toByteTranslator the translator that takes a proper type and creates bytes
	 */
	public void registerDataTranslators( Class<?> theClass, Translator fromByteTranslator, Translator toByteTranslator ) {
		Preconditions.checkNotNull( fromByteTranslator, "need a from-byte  translator" );
		Preconditions.checkNotNull( toByteTranslator, "need a to-byte translator" );
		
		fromByteTranslators.put( theClass, fromByteTranslator );
		toByteTranslators.put( theClass, toByteTranslator );
	}

	
	// TODO: consider how translators get used for this
	
	public static byte[] translateName( String theName ) {
		return Bytes.toBytes( theName );
	}
	
	public static String translateName( byte[] theName ) {
		return Bytes.toString( theName );
	}
	
	public Translator getToByteTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = toByteTranslators.get( theType );
		
		if( translator == null ) {
			if( theType.isEnum( ) ) {
				translator = new EnumToBytesTranslator( theType );
				toByteTranslators.put( theType, translator );
			} else {
				// TODO: currently a hack
				Translator jsonTranslator = this.jsonTranslationFacility.getToJsonElementTranslator(theType, theGenericType );
				translator = new ChainToStringToBytesTranslator( new ChainToJsonElementToStringTranslator( jsonTranslator ) );
			}
		}
		return translator;
	}

	public Translator getFromByteTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = fromByteTranslators.get( theType );
		
		if( translator == null ) {
			if( theType.isEnum( ) ) {
				translator = new BytesToEnumTranslator( theType );
				fromByteTranslators.put( theType, translator );
			} else {
				// TODO: currently a hack
				Translator jsonTranslator = this.jsonTranslationFacility.getFromJsonElementTranslator(theType, theGenericType );
				translator = new BytesToStringToChainTranslator( new StringToJsonElementToChainTranslator( jsonTranslator ) );
			}
		}
		return translator;
	}

	Translator getToByteKeyTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = toByteTranslators.get( theType );
		
		if( translator == null ) {
			if( theType.isEnum( ) ) {
				translator = new EnumToBytesTranslator( theType );
				toByteTranslators.put( theType, translator );
			} else {
				// TODO: currently a hack ... keys do not know what to do with this
			}
		}
		return translator;
	}

	Translator getFromByteKeyTranslator( Class<?> theType, Type theGenericType ) {
		Translator translator = fromByteTranslators.get( theType );
		
		if( translator == null ) {
			if( theType.isEnum( ) ) {
				translator = new BytesToEnumTranslator( theType );
				fromByteTranslators.put( theType, translator );
			} else {
				// TODO: currently a hack ... keys do not know what to do with this
			}
		}
		return translator;
	}

	public HBaseTableMap generateTableMap( Class<?> theType ) {
		Preconditions.checkNotNull( theType, "need a type" );

		if( typeToTableMaps.containsKey( theType ) ) {
			return typeToTableMaps.get( theType );
			
		} else {
			return generateTableMap( this.storageFacility.generateType( theType ) );
		}

	}
	
	public HBaseTableMap generateTableMap( StorageType theType ) {
		Preconditions.checkNotNull( theType, "need a type" );
		Preconditions.checkArgument( theType.isRootStorageType(), String.format( "'%s' is not a root storage table since it has no id", theType.getType().getName() ) );

		if( storageToTableMaps.containsKey( theType ) ) {
			return storageToTableMaps.get( theType );
			
		} else {
			HBaseTableMap tableMap = new HBaseTableMap( theType );
			
			Translator keyTranslator = getToByteKeyTranslator( theType.getIdType(), null ); // TODO: we do this because the json handling couldn't overlap
			if( keyTranslator == null ) {
				if( theType.getCompoundIdType() == null ) {
					// this means we have no way to translate the non-compound key
					throw new IllegalStateException( String.format( "Could not find a translator for non-compound key of type '%s'.", theType.getIdType( ).getName() ) );
				} else {
					List<Translator> translators = new ArrayList<Translator>();
					// TODO: may need to consider how to do something fixed width
					//       like they specific widths in the annotation
					for( CompoundIdField idField : theType.getCompoundIdType().getFields( ) ) {
						Translator translator = getToByteKeyTranslator( idField.getFieldType().getType( ),  idField.getFieldType().getGenericType( ) ); // TODO: don't really want key translator for this
						if( translator == null ) {
							throw new IllegalStateException( String.format( "Could not find a translator for compound id field '%s.%s'.", theType.getIdType( ).getName(), idField.getName() ) );
						} else {
							translators.add( translator );
						}
					}
					keyTranslator = new KeyToBytesTranslator( theType.getCompoundIdType(), translators );
				}
			}
			HBaseKeyMap keyMap = new HBaseKeyMap( keyTranslator, tableMap );
			
			Map<String,ArrayList<StorageType>> families = theType.getFamilies();
			ArrayList<HBaseFamilyMap> familyMaps = new ArrayList<HBaseFamilyMap>( families.size() );
			
			// we need to capture which fields on one storage type point to a different storage type
			// so this map is for that ... the storage field values are NOT the children to the 
			// storage type key, but the fields of another type that point to storage type 
			HashMap<StorageType,ArrayList<StorageField>> facetFields = new HashMap<StorageType, ArrayList<StorageField>>();
			
			for( String familyName : families.keySet( ) ) {
				// create the family map representing the current family being analyzed
				HBaseFamilyMap familyMap = new HBaseFamilyMap( familyName, tableMap );
				// we get the storage types that belong to the family
				ArrayList<StorageType> familyStorageType = families.get( familyName );
				// we need to collect all the family parts
				ArrayList<HBaseFamilyPartMap> familyPartMaps = new ArrayList<HBaseFamilyPartMap>();
				
				// for each storage type for the family, we create a family part
				// which is specific to the storage type (and the storage type 
				// can only be with at most, one family)
				for( StorageType storageType : familyStorageType ) { // storage types map directly to family parts
					// create the family part map representing the current storage type
					HBaseFamilyPartMap familyPartMap = new HBaseFamilyPartMap( storageType, familyMap );
					// we will create a list of all columns that belong to this family part
					// this is reading/writing is at the scope of the fields for the storage type
					ArrayList<HBaseColumnMap >familyPartColumnMaps = new ArrayList<HBaseColumnMap>( );
					// now we go through the fields that storage type
					for( StorageField field : storageType.getFields( ) ) {
						if( field.isFacetField( ) ) {
							// if we are facet field, we don't save like others
							// instead we want to store a reference of this field with the
							// family of the class the facet field is referring to. Note, this
							// family could be different than the family of the facet field's parent
							// ... note, this could be a collection, in which case this is the element type
							StorageType facetType = field.getFacetReferenceType();
							// get the family map  for this facet
							ArrayList<StorageField> familyFacetFields = facetFields.get( facetType );
							if( familyFacetFields == null ) {
								// if this is the first tiem we have referenced the family, we
								// create a list for other potential references
								familyFacetFields = new ArrayList<StorageField>( 2 );
								facetFields.put( facetType, familyFacetFields );
							}
							// we keep the reference
							familyFacetFields.add( field );
							
						} else if( !field.isParentField( ) ) {
							Translator toByteTranslator = getToByteTranslator( field.getFieldType().getType(), field.getFieldType().getGenericType( ) );
							Translator fromByteTranslator = getFromByteTranslator( field.getFieldType().getType(), field.getFieldType().getGenericType( ) );
							
							if( toByteTranslator == null ) {
				            	throw new IllegalStateException( String.format( "Unable to get a to-byte-translator for '%s'.", field.getFieldType().getType().getName( ) ) );
							} else if( fromByteTranslator == null ) {
				            	throw new IllegalStateException( String.format( "Unable to get a from-byte-translator for '%s'.", field.getFieldType().getType().getName( ) ) );
							} else {
								HBaseColumnMap columnMap = new HBaseColumnMap( field, new TranslatedDataSite( field.getSite(), toByteTranslator, fromByteTranslator) ,familyPartMap );
								// now track that we made it for the family part
								familyPartColumnMaps.add( columnMap );
							}
						}
					}
					familyPartMap.setRealColumns( familyPartColumnMaps );
					// TODO: to help with facet object handling, I could generate translators here to sue
					familyPartMaps.add( familyPartMap );
				}
				familyMap.setFamilyParts( familyPartMaps );
				familyMaps.add( familyMap );
			}
			tableMap.setFamilies( familyMaps );
			tableMap.setKey( keyMap );
			
			// with the families all settled, we now tell the families about references
			// to them from facet fields
			for( StorageType storageType : facetFields.keySet( ) ) {
				HBaseFamilyPartMap familyPartMap = tableMap.getFamilyPart( storageType );
				familyPartMap.setReferringFacetFields( facetFields.get( storageType ), this );
			}
			// with the facets all settled, we now go over families to get their facet
			// references and columns prepared for lookup during get/scan calls
			for( HBaseFamilyMap family : tableMap.getFamilies() ) {
				family.prepareNameLookup();
			}
			this.storageToTableMaps.put( theType, tableMap );
			this.typeToTableMaps.put( theType.getType(), tableMap );
			return tableMap;
		}
	}

}
