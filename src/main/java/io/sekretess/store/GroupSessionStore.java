package io.sekretess.store;

import io.sekretess.model.GroupSessionData;


public interface GroupSessionStore {
    void saveGroupSession(String name, int deviceId, String distributionId, String sessionRecord);

    void saveSendDistributionMessage(String name, int deviceId, String distributionId, String businessDistributionMessage);

    GroupSessionData loadGroupSession(String name);

}
