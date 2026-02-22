package io.sekretess.demobusinesssdk;

import io.sekretess.model.SessionData;
import io.sekretess.store.SessionStore;

import java.io.Serializable;
import java.util.List;

import io.sekretess.store.SessionStore;
import io.sekretess.model.SessionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}

