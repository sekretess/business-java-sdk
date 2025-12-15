package io.sekretess.store;

import io.sekretess.model.SessionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of SessionStore for testing.
 */
public class InMemorySessionStore implements SessionStore {

    private final Map<String, SessionData> store = new HashMap<>();

    @Override
    public void saveSession(String name, int deviceId, String base64EncodedRecord) {
        store.put(name, new SessionData(name, deviceId, base64EncodedRecord));
    }

    @Override
    public List<SessionData> loadAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteSession(String name) {
        store.remove(name);
    }

    /**
     * Clear all stored sessions (useful for test cleanup).
     */
    public void clear() {
        store.clear();
    }

    /**
     * Get the number of stored sessions.
     */
    public int size() {
        return store.size();
    }
}

