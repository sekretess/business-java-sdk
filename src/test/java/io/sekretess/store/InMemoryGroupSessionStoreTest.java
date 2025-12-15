package io.sekretess.store;

import io.sekretess.model.GroupSessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InMemoryGroupSessionStore.
 */
class InMemoryGroupSessionStoreTest {

    private InMemoryGroupSessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryGroupSessionStore();
    }

    @Test
    void testSaveAndLoadGroupSession() {
        // Arrange
        String name = "business-123";
        int deviceId = 1;
        String distributionId = "dist-id";
        String sessionRecord = "record";

        // Act
        store.saveGroupSession(name, deviceId, distributionId, sessionRecord);
        GroupSessionData loaded = store.loadGroupSession(name);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.name()).isEqualTo(name);
        assertThat(loaded.deviceId()).isEqualTo(deviceId);
        assertThat(loaded.distributionId()).isEqualTo(distributionId);
        assertThat(loaded.sessionRecord()).isEqualTo(sessionRecord);
    }

    @Test
    void testLoadNonExistentGroupSession() {
        // Act
        GroupSessionData loaded = store.loadGroupSession("non-existent");

        // Assert
        assertThat(loaded).isNull();
    }

    @Test
    void testSaveDistributionMessage() {
        // Arrange
        String name = "business";
        int deviceId = 1;
        String distributionId = "dist-id";
        String message = "distribution-message";

        // Act
        store.saveSendDistributionMessage(name, deviceId, distributionId, message);
        GroupSessionData loaded = store.loadGroupSession(name);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.name()).isEqualTo(name);
        assertThat(loaded.sessionRecord()).isEqualTo(message);
    }

    @Test
    void testUpdateGroupSession() {
        // Arrange
        String name = "business";
        store.saveGroupSession(name, 1, "dist-1", "record1");

        // Act
        store.saveGroupSession(name, 2, "dist-2", "record2");

        // Assert
        GroupSessionData loaded = store.loadGroupSession(name);
        assertThat(loaded.deviceId()).isEqualTo(2);
        assertThat(loaded.distributionId()).isEqualTo("dist-2");
        assertThat(loaded.sessionRecord()).isEqualTo("record2");
    }

    @Test
    void testMultipleBusinesses() {
        // Arrange
        String business1 = "business1";
        String business2 = "business2";

        // Act
        store.saveGroupSession(business1, 1, "dist-1", "record1");
        store.saveGroupSession(business2, 2, "dist-2", "record2");

        // Assert
        assertThat(store.loadGroupSession(business1).distributionId()).isEqualTo("dist-1");
        assertThat(store.loadGroupSession(business2).distributionId()).isEqualTo("dist-2");
    }

    @Test
    void testClear() {
        // Arrange
        store.saveGroupSession("business1", 1, "dist-1", "record1");
        store.saveGroupSession("business2", 2, "dist-2", "record2");
        assertThat(store.size()).isEqualTo(2);

        // Act
        store.clear();

        // Assert
        assertThat(store.size()).isEqualTo(0);
        assertThat(store.loadGroupSession("business1")).isNull();
        assertThat(store.loadGroupSession("business2")).isNull();
    }

    @Test
    void testSize() {
        // Arrange
        assertThat(store.size()).isEqualTo(0);

        // Act & Assert
        store.saveGroupSession("business1", 1, "dist-1", "record1");
        assertThat(store.size()).isEqualTo(1);

        store.saveGroupSession("business2", 2, "dist-2", "record2");
        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void testWithEmptySessionRecord() {
        // Arrange
        String name = "business";

        // Act
        store.saveGroupSession(name, 1, "dist-id", "");
        GroupSessionData loaded = store.loadGroupSession(name);

        // Assert
        assertThat(loaded.sessionRecord()).isEmpty();
    }

    @Test
    void testWithZeroDeviceId() {
        // Arrange
        store.saveGroupSession("business", 0, "dist-id", "record");

        // Act
        GroupSessionData loaded = store.loadGroupSession("business");

        // Assert
        assertThat(loaded.deviceId()).isEqualTo(0);
    }
}

