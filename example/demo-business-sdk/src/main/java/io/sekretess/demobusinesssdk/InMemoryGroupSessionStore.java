package io.sekretess.demobusinesssdk;

import io.sekretess.store.GroupSessionStore;
import io.sekretess.model.GroupSessionData;

import java.util.HashMap;
import java.util.Map;

public class InMemoryGroupSessionStore implements GroupSessionStore {
    private final Map<String, GroupSessionData> store = new HashMap<>();

    @Override
    public void saveGroupSession(String name, int deviceId, String distributionId, String sessionRecord) {
        GroupSessionData existing = store.get(name);
        String existingDistMessage = existing != null ? existing.businessDistributionMessage() : null;
        store.put(name, new GroupSessionData(name, deviceId, distributionId, sessionRecord, existingDistMessage));
    }

    @Override
    public void saveSendDistributionMessage(String name, int deviceId, String distributionId, String businessDistributionMessage) {
        store.put(name, new GroupSessionData(name, deviceId, distributionId, null, businessDistributionMessage));
    }

    @Override
    public GroupSessionData loadGroupSession(String name) {
        return store.get(name);
    }
}


