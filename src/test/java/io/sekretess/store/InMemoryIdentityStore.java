package io.sekretess.store;

import io.sekretess.model.IdentityKeyData;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of IdentityStore for testing.
 */
public class InMemoryIdentityStore implements IdentityStore {

    private final Map<String, IdentityKeyData> store = new HashMap<>();

    @Override
    public IdentityKeyData loadIdentity(String username) {
        return store.get(username);
    }

    @Override
    public void saveIdentity(String username, byte[] serializedIdentityKeyPair, int registrationId) {
        store.put(username, new IdentityKeyData(username, serializedIdentityKeyPair, registrationId));
    }

    /**
     * Clear all stored identities (useful for test cleanup).
     */
    public void clear() {
        store.clear();
    }

    /**
     * Get the number of stored identities.
     */
    public int size() {
        return store.size();
    }
}

