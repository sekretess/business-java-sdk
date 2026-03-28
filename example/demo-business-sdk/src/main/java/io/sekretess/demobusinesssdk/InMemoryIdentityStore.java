package io.sekretess.demobusinesssdk;

import io.sekretess.store.IdentityStore;
import io.sekretess.model.IdentityKeyData;

import java.util.HashMap;
import java.util.Map;

public class InMemoryIdentityStore implements IdentityStore {
    private final Map<String, IdentityKeyData> identities = new HashMap<>();

    @Override
    public IdentityKeyData loadIdentity(String username) {
        return identities.get(username);
    }

    @Override
    public void saveIdentity(String username, byte[] serializedIdentityKeyPair, int registrationId) {
        identities.put(username, new IdentityKeyData(username, serializedIdentityKeyPair, registrationId));
    }
}