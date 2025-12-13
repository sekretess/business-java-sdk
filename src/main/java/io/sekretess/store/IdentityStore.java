package io.sekretess.store;

import io.sekretess.model.IdentityKeyData;

public interface IdentityStore {
    IdentityKeyData loadIdentity(String username);

    void saveIdentity(String username, byte[] serializedIdentityKeyPair, int registrationId);

}
