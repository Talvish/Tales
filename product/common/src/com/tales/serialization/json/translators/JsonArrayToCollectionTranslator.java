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
package com.tales.serialization.json.translators;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import com.tales.parts.translators.TranslationException;
import com.tales.parts.translators.Translator;


/**
 * Translator that converts a json array into a list.
 * @author jmolnar
 *
 */
public class JsonArrayToCollectionTranslator implements Translator {
	private final Translator elementTranslator;
	private final Class<?> collectionType;
	private final Constructor<?> constructor;

	/**
	 * Empty default constructor.
	 */
	public JsonArrayToCollectionTranslator( Translator theElementTranslator, Class<?> theCollectionType ) {
		Preconditions.checkNotNull( theElementTranslator );
		Preconditions.checkNotNull( theCollectionType, "need a collection type" );
		Preconditions.checkArgument( Collection.class.isAssignableFrom( theCollectionType ), "needs to implement collection" );
		
		elementTranslator = theElementTranslator;
		if( Modifier.isAbstract( theCollectionType.getModifiers( ) ) || theCollectionType.isInterface( ) ) {
			if( theCollectionType.isAssignableFrom( ArrayList.class ) ) {
				collectionType = ArrayList.class; // a list
			} else if( theCollectionType.isAssignableFrom( HashSet.class ) ) {
				collectionType = HashSet.class; // a set 
			} else if( theCollectionType.isAssignableFrom( TreeSet.class ) ) {
				collectionType = TreeSet.class; // a sorted set
			} else {
				throw new IllegalArgumentException( String.format( "unclear how to use the collection of type '%s'", theCollectionType.getName() ) );
			}
		} else {
			collectionType = theCollectionType;
		}
		try {
			constructor = collectionType.getDeclaredConstructor( );
		} catch (SecurityException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for collection of type '%s'", theCollectionType.getName() ), e );
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( String.format( "unable to get constructor for collection of type '%s'", theCollectionType.getName() ), e );
		}
	}

	/**
	 * Translates the received object into a list.
	 * If the object is of the wrong type, a TranslationException will occur.
	 */
	@Override
	public Object translate(Object anObject) {
		Object returnValue;
		
		if( anObject == null || anObject.equals( JsonNull.INSTANCE ) ) {
			returnValue = null;
		} else {
			try {
				JsonArray jsonArray = ( JsonArray )anObject;
				@SuppressWarnings("unchecked")
				Collection<Object> collection = ( Collection<Object> )constructor.newInstance();
				
				for( JsonElement element : jsonArray ) {
					collection.add( elementTranslator.translate( element ) );
				}
				
				returnValue = collection;

			} catch( ClassCastException e ) {
				throw new TranslationException( e );
			} catch( NullPointerException e ) {
				throw new TranslationException( String.format( "Unable to use null in the collection of type '%s'", collectionType.getName() ), e );
			} catch (InstantiationException e) {
				throw new TranslationException( String.format( "Unable to create a collection of type '%s'", collectionType.getName() ), e );
			} catch (IllegalArgumentException e) {
				throw new TranslationException( String.format( "Unable to create a collection of type '%s'", collectionType.getName() ), e );
			} catch (InvocationTargetException e) {
				throw new TranslationException( String.format( "Unable to create a collection of type '%s'", collectionType.getName() ), e );
			} catch (IllegalAccessException e) {
				throw new TranslationException( String.format( "Unable to create a collection of type '%s'", collectionType.getName() ), e );
			}
		}
		return returnValue;	
	}
}
