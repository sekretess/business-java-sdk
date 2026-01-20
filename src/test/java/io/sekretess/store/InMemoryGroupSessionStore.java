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
        // Preserve existing businessDistributionMessage if present
        GroupSessionData existing = store.get(name);
        String existingDistMessage = existing != null ? existing.businessDistributionMessage() : null;
        store.put(name, new GroupSessionData(name, deviceId, distributionId, sessionRecord, existingDistMessage));
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

    /**
     * Save complete group session data with both session record and distribution message.
     * This is needed for testing scenarios where both are required.
     */
    public void saveCompleteGroupSession(String name, int deviceId, String distributionId,
                                          String sessionRecord, String businessDistributionMessage) {
        store.put(name, new GroupSessionData(name, deviceId, distributionId, sessionRecord, businessDistributionMessage));
    }
}

