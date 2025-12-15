package io.sekretess.store;

import io.sekretess.model.SessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InMemorySessionStore.
 */
class InMemorySessionStoreTest {

    private InMemorySessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemorySessionStore();
    }

    @Test
    void testSaveAndLoadSession() {
        // Arrange
        String name = "consumer-123";
        int deviceId = 1;
        String record = "base64-record";

        // Act
        store.saveSession(name, deviceId, record);
        List<SessionData> all = store.loadAll();

        // Assert
        assertThat(all).hasSize(1);
        assertThat(all.get(0).name()).isEqualTo(name);
        assertThat(all.get(0).deviceId()).isEqualTo(deviceId);
        assertThat(all.get(0).base64SessionRecord()).isEqualTo(record);
    }

    @Test
    void testLoadAll() {
        // Arrange
        store.saveSession("consumer1", 1, "record1");
        store.saveSession("consumer2", 2, "record2");
        store.saveSession("consumer3", 3, "record3");

        // Act
        List<SessionData> all = store.loadAll();

        // Assert
        assertThat(all).hasSize(3);
        assertThat(all.stream().map(SessionData::name))
                .containsExactlyInAnyOrder("consumer1", "consumer2", "consumer3");
    }

    @Test
    void testLoadAllEmpty() {
        // Act
        List<SessionData> all = store.loadAll();

        // Assert
        assertThat(all).isEmpty();
    }

    @Test
    void testDeleteSession() {
        // Arrange
        store.saveSession("consumer1", 1, "record1");
        store.saveSession("consumer2", 2, "record2");
        assertThat(store.size()).isEqualTo(2);

        // Act
        store.deleteSession("consumer1");

        // Assert
        assertThat(store.size()).isEqualTo(1);
        List<SessionData> all = store.loadAll();
        assertThat(all.get(0).name()).isEqualTo("consumer2");
    }

    @Test
    void testDeleteNonExistent() {
        // Arrange
        store.saveSession("consumer1", 1, "record1");

        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            store.deleteSession("non-existent"));

        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void testUpdateSession() {
        // Arrange
        String name = "consumer";
        store.saveSession(name, 1, "record1");

        // Act
        store.saveSession(name, 2, "record2");

        // Assert
        List<SessionData> all = store.loadAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).deviceId()).isEqualTo(2);
        assertThat(all.get(0).base64SessionRecord()).isEqualTo("record2");
    }

    @Test
    void testClear() {
        // Arrange
        store.saveSession("consumer1", 1, "record1");
        store.saveSession("consumer2", 2, "record2");
        assertThat(store.size()).isEqualTo(2);

        // Act
        store.clear();

        // Assert
        assertThat(store.size()).isEqualTo(0);
        assertThat(store.loadAll()).isEmpty();
    }

    @Test
    void testSize() {
        // Arrange
        assertThat(store.size()).isEqualTo(0);

        // Act & Assert
        store.saveSession("consumer1", 1, "record1");
        assertThat(store.size()).isEqualTo(1);

        store.saveSession("consumer2", 2, "record2");
        assertThat(store.size()).isEqualTo(2);

        store.deleteSession("consumer1");
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void testWithEmptyRecord() {
        // Arrange
        String name = "consumer";
        String emptyRecord = "";

        // Act
        store.saveSession(name, 1, emptyRecord);
        List<SessionData> all = store.loadAll();

        // Assert
        assertThat(all.get(0).base64SessionRecord()).isEmpty();
    }

    @Test
    void testWithZeroDeviceId() {
        // Arrange
        store.saveSession("consumer", 0, "record");

        // Act
        List<SessionData> all = store.loadAll();

        // Assert
        assertThat(all.get(0).deviceId()).isEqualTo(0);
    }
}

