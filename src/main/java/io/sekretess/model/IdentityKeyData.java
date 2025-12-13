package io.sekretess.model;

public record IdentityKeyData(byte[] serializedIdentityKeyPair, int registrationId) {}
