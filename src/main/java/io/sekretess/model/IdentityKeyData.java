package io.sekretess.model;

public record IdentityKeyData(String username, byte[] serializedIdentityKeyPair, int registrationId) {}
