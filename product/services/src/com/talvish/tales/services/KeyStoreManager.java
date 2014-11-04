package com.talvish.tales.services;

import java.security.KeyStore;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.talvish.tales.system.Facility;

/**
 * A simple manager that manages key stores used in the system.
 * @author jmolnar
 *
 */
public class KeyStoreManager implements Facility {
	private Map<String,KeyStore> keyStores = Collections.unmodifiableMap( new HashMap< String,KeyStore >( ) );
	private Object storeLock = new Object();
	
	/**
	 * Returns all of the key stores.
	 * @return the collection of key stores
	 */
	public Collection<KeyStore> getKeyStores( ) {
		return keyStores.values();
	}

	/**
	 * Retrieves a particular key store.
	 * @param theName the name of the key store to retrieve
	 * @return the keystore if found, null otherwise
	 */
	public KeyStore getKeyStore( String theName) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ) );
		return keyStores.get( theName );
	}
	
	/**
	 * Registers a key store with the manager. 
	 * If a key store with this manager already exist then 
	 * an exception is thrown.
	 * @param theName the name to give the key store
	 * @param theKeyStore the key store
	 */
	public void register( String theName, KeyStore theKeyStore ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theName ), "need a name" );
		Preconditions.checkNotNull( theKeyStore, "need a keystore" );

		synchronized( storeLock ) {
			if( keyStores.containsKey( theName ) ) {
				throw new IllegalStateException( String.format( "Key store with name '%s' is already registered.", theName ) );
			} else {
				Map<String,KeyStore> newKeyStores = new HashMap<String, KeyStore>( keyStores );
				
				newKeyStores.put( theName, theKeyStore );
				keyStores = newKeyStores;
			}
		}
	}
}
