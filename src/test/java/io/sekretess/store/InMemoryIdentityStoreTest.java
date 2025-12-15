package io.sekretess.store;

import io.sekretess.model.IdentityKeyData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InMemoryIdentityStore.
 */
class InMemoryIdentityStoreTest {

    private InMemoryIdentityStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdentityStore();
    }

    @Test
    void testSaveAndLoadIdentity() {
        // Arrange
        String username = "test-user";
        byte[] keyPair = new byte[]{1, 2, 3, 4, 5};
        int regId = 12345;

        // Act
        store.saveIdentity(username, keyPair, regId);
        IdentityKeyData loaded = store.loadIdentity(username);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.username()).isEqualTo(username);
        assertThat(loaded.registrationId()).isEqualTo(regId);
        assertThat(loaded.serializedIdentityKeyPair()).isEqualTo(keyPair);
    }

    @Test
    void testLoadNonExistentIdentity() {
        // Act
        IdentityKeyData loaded = store.loadIdentity("non-existent");

        // Assert
        assertThat(loaded).isNull();
    }

    @Test
    void testUpdateIdentity() {
        // Arrange
        String username = "user";
        byte[] keyPair1 = new byte[]{1, 2};
        byte[] keyPair2 = new byte[]{3, 4};

        // Act - Save first identity
        store.saveIdentity(username, keyPair1, 100);
        IdentityKeyData first = store.loadIdentity(username);

        // Act - Save second identity (overwrite)
        store.saveIdentity(username, keyPair2, 200);
        IdentityKeyData second = store.loadIdentity(username);

        // Assert
        assertThat(first.registrationId()).isEqualTo(100);
        assertThat(second.registrationId()).isEqualTo(200);
        assertThat(second.serializedIdentityKeyPair()).isEqualTo(keyPair2);
    }

    @Test
    void testMultipleUsers() {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";

        // Act
        store.saveIdentity(user1, new byte[]{1}, 100);
        store.saveIdentity(user2, new byte[]{2}, 200);

        // Assert
        assertThat(store.loadIdentity(user1).registrationId()).isEqualTo(100);
        assertThat(store.loadIdentity(user2).registrationId()).isEqualTo(200);
    }

    @Test
    void testClear() {
        // Arrange
        store.saveIdentity("user1", new byte[]{1}, 100);
        store.saveIdentity("user2", new byte[]{2}, 200);
        assertThat(store.size()).isEqualTo(2);

        // Act
        store.clear();

        // Assert
        assertThat(store.size()).isEqualTo(0);
        assertThat(store.loadIdentity("user1")).isNull();
        assertThat(store.loadIdentity("user2")).isNull();
    }

    @Test
    void testSize() {
        // Arrange
        assertThat(store.size()).isEqualTo(0);

        // Act & Assert
        store.saveIdentity("user1", new byte[]{1}, 100);
        assertThat(store.size()).isEqualTo(1);

        store.saveIdentity("user2", new byte[]{2}, 200);
        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void testWithEmptyKeyPair() {
        // Arrange
        String username = "user";
        byte[] emptyKeyPair = new byte[0];

        // Act
        store.saveIdentity(username, emptyKeyPair, 100);
        IdentityKeyData loaded = store.loadIdentity(username);

        // Assert
        assertThat(loaded.serializedIdentityKeyPair()).isEmpty();
    }

    @Test
    void testWithZeroRegistrationId() {
        // Arrange
        String username = "user";

        // Act
        store.saveIdentity(username, new byte[]{1}, 0);
        IdentityKeyData loaded = store.loadIdentity(username);

        // Assert
        assertThat(loaded.registrationId()).isEqualTo(0);
    }
}

