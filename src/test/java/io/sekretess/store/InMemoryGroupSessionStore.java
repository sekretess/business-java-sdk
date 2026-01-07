package io.sekretess.store;

import io.sekretess.model.GroupSessionData;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of GroupSessionStore for testing.
 */
public class InMemoryGroupSessionStore implements GroupSessionStore {

    private final Map<String, GroupSessionData> store = new HashMap<>();

    @Override
    public void saveGroupSession(String name, int deviceId, String distributionId, String sessionRecord) {
        store.put(name, new GroupSessionData(name, deviceId, distributionId, sessionRecord, null));
    }

    @Override
    public void saveSendDistributionMessage(String name, int deviceId, String distributionId, String businessDistributionMessage) {
        store.put(name, new GroupSessionData(name, deviceId, distributionId,null, businessDistributionMessage));
    }

    @Override
    public GroupSessionData loadGroupSession(String name) {
        return store.get(name);
    }

    /**
     * Clear all stored group sessions (useful for test cleanup).
     */
    public void clear() {
        store.clear();
    }

    /**
     * Get the number of stored group sessions.
     */
    public int size() {
        return store.size();
    }
}

