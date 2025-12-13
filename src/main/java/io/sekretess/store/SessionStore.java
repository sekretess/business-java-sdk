package io.sekretess.store;

import io.sekretess.model.SessionData;

import java.util.List;

public interface SessionStore {
    void saveSession(String name, int deviceId, String base64EncodedRecord);

    List<SessionData> loadAll();

    void deleteSession(String name);
}
