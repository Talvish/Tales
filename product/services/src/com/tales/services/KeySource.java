package com.tales.services;

import java.security.KeyPair;

/**
 * A helper class to find and retrieve keys from.
 * @author jmolnar
 *
 * @param <Decider>
 */
public interface KeySource<LookupKey> {
	/**
	 * Called to get the keys from the source.
	 * @param theLookupKey the lookup key 
	 * @return the key pair found using the lookup key, provider, null if not found
	 */
    KeyPair getKeys( LookupKey theLookupKey );
}
