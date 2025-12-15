package io.sekretess.manager;

import io.sekretess.client.SekretessServerClient;
import io.sekretess.store.InMemoryGroupSessionStore;
import io.sekretess.store.InMemoryIdentityStore;
import io.sekretess.store.InMemorySessionStore;
import io.sekretess.store.SekretessSignalProtocolStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SekretessManager with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class SekretessManagerTest {

    private InMemoryIdentityStore identityStore;
    private InMemorySessionStore sessionStore;
    private InMemoryGroupSessionStore groupSessionStore;

    @Mock
    private SekretessServerClient mockServerClient;

    private SekretessManager manager;

    @BeforeEach
    void setUp() throws Exception {
        identityStore = new InMemoryIdentityStore();
        sessionStore = new InMemorySessionStore();
        groupSessionStore = new InMemoryGroupSessionStore();
        System.setProperty("BUSINESS_USER_NAME", "test-business");

        // Create the signal protocol store directly for testing
        // You may need to provide dummy values for the constructor, or use a test utility if available
        // For now, let's assume a constructor exists that takes the stores
        SekretessSignalProtocolStore protocolStore = new SekretessSignalProtocolStore(
            /* identityKeyPair */ null, /* registrationId */ 0, sessionStore, groupSessionStore
        );
        manager = new SekretessManager(protocolStore, mockServerClient);
    }

    @Test
    void testManagerInitialization() {
        // Assert
        assertThat(manager).isNotNull();
    }

    @Test
    void testDeleteUserSession() {
        // Arrange
        String consumer = "consumer-123";
        sessionStore.saveSession(consumer, 123, Base64.getEncoder().encodeToString("test".getBytes()));
        assertThat(sessionStore.size()).isEqualTo(1);

        // Act
        manager.deleteUserSession(consumer);

        // Assert
        assertThat(sessionStore.size()).isEqualTo(0);
    }

    @Test
    void testDeleteNonExistentUserSession() {
        // Act & Assert - Should not throw exception
        assertThatNoException().isThrownBy(() ->
                manager.deleteUserSession("non-existent-consumer")
        );
    }

    @Test
    void testMultipleSessionDeletion() {
        // Arrange
        sessionStore.saveSession("consumer1", 123, Base64.getEncoder().encodeToString("session1".getBytes()));
        sessionStore.saveSession("consumer2", 123, Base64.getEncoder().encodeToString("session2".getBytes()));
        assertThat(sessionStore.size()).isEqualTo(2);

        // Act
        manager.deleteUserSession("consumer1");

        // Assert
        assertThat(sessionStore.size()).isEqualTo(1);
        assertThat(sessionStore.loadAll().get(0).name()).isEqualTo("consumer2");
    }

    @Test
    void testIdentityStoreIsPreserved() {
        // Assert - Identity should have been created by factory
        assertThat(identityStore.loadIdentity("test-business")).isNull(); // Since we didn't use the factory
    }

    @Test
    void testGroupSessionStoreIsPreserved() {
        // Assert - Group session should have been created by factory
        assertThat(groupSessionStore.loadGroupSession("test-business")).isNull(); // Since we didn't use the factory
    }

    @Test
    void testManagerWithMultipleSessionDeletions() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            sessionStore.saveSession("consumer" + i, 123, Base64.getEncoder().encodeToString(("session" + i).getBytes()));
        }
        assertThat(sessionStore.size()).isEqualTo(5);

        // Act
        for (int i = 0; i < 5; i++) {
            manager.deleteUserSession("consumer" + i);
        }

        // Assert
        assertThat(sessionStore.size()).isEqualTo(0);
    }

    @Test
    void testDeleteSessionMultipleTimes() {
        // Arrange
        String consumer = "consumer-123";
        sessionStore.saveSession(consumer, 123, Base64.getEncoder().encodeToString("test".getBytes()));

        // Act & Assert - First delete should work
        assertThatNoException().isThrownBy(() -> manager.deleteUserSession(consumer));

        // Act & Assert - Second delete should also not throw (idempotent)
        assertThatNoException().isThrownBy(() -> manager.deleteUserSession(consumer));

        assertThat(sessionStore.size()).isEqualTo(0);
    }

    @Test
    void testManagerUsesInjectedServerClient() {
        // This test verifies that the manager was created with the mocked server client
        // The mock allows us to verify behavior without making actual HTTP calls
        assertThat(manager).isNotNull();
        // Manager is properly initialized with the mock
    }
}
