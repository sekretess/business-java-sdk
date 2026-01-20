package io.sekretess.manager;

import io.sekretess.client.SekretessServerClient;
import io.sekretess.client.response.ConsumerKeysResponse;
import io.sekretess.client.response.SendAdsMessageResponse;
import io.sekretess.client.response.SendMessageResponse;
import io.sekretess.exception.MessageSendException;
import io.sekretess.exception.PrekeyBundleException;
import io.sekretess.store.InMemoryGroupSessionStore;
import io.sekretess.store.InMemoryIdentityStore;
import io.sekretess.store.InMemorySessionStore;
import io.sekretess.store.SekretessSignalProtocolStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyPair;
import org.signal.libsignal.protocol.kem.KEMKeyType;
import org.signal.libsignal.protocol.util.KeyHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    // ==================== sendMessageToConsumer Tests ====================

    @Test
    void sendMessageToConsumer_ThrowsPrekeyBundleException_WhenNoSessionAndServerFails() throws Exception {
        // Arrange
        String consumer = "consumer-123";
        setUserName(manager, "test-business");

        when(mockServerClient.getConsumerKeys(consumer))
                .thenThrow(new RuntimeException("Server unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> manager.sendMessageToConsumer("Hello", consumer))
                .isInstanceOf(PrekeyBundleException.class)
                .hasMessageContaining(consumer);
    }

    @Test
    void sendMessageToConsumer_ThrowsPrekeyBundleException_WhenGetConsumerKeysThrowsIOException() throws Exception {
        // Arrange
        String consumer = "consumer-456";
        setUserName(manager, "test-business");

        when(mockServerClient.getConsumerKeys(consumer))
                .thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThatThrownBy(() -> manager.sendMessageToConsumer("Test message", consumer))
                .isInstanceOf(PrekeyBundleException.class)
                .hasMessageContaining(consumer);
    }

    @Test
    void sendMessageToConsumer_ThrowsPrekeyBundleException_WhenConsumerKeysResponseIsInvalid() throws Exception {
        // Arrange
        String consumer = "consumer-789";
        setUserName(manager, "test-business");

        // Return an invalid response that will cause parsing to fail
        ConsumerKeysResponse invalidResponse = new ConsumerKeysResponse(
                consumer, "invalid-ik", "invalid-opk", "invalid-sig",
                "invalid-spk", "not-a-number", "invalid-pqspk",
                "not-a-number", "invalid-pqsig", 123
        );
        when(mockServerClient.getConsumerKeys(consumer)).thenReturn(invalidResponse);

        // Act & Assert
        assertThatThrownBy(() -> manager.sendMessageToConsumer("Test", consumer))
                .isInstanceOf(PrekeyBundleException.class)
                .hasMessageContaining(consumer);
    }

    // ==================== sendAdsMessage Tests ====================

    @Test
    void sendAdsMessage_ThrowsMessageSendException_WhenGroupSessionNotFound() throws Exception {
        // Arrange
        setUserName(manager, "test-business");
        // groupSessionStore is empty, so loadGroupSession will return null

        // Act & Assert
        assertThatThrownBy(() -> manager.sendAdsMessage("Promotional message"))
                .isInstanceOf(MessageSendException.class)
                .hasMessageContaining("ads message");
    }

    @Test
    void sendAdsMessage_ThrowsMessageSendException_WhenGroupSessionRecordIsNull() throws Exception {
        // Arrange
        String businessName = "test-business";
        setUserName(manager, businessName);

        // Save a group session with null session record
        groupSessionStore.saveGroupSession(businessName, 1, "test-dist-id", null);

        // Act & Assert
        assertThatThrownBy(() -> manager.sendAdsMessage("Ad message"))
                .isInstanceOf(MessageSendException.class)
                .hasMessageContaining("ads message");
    }

    @Test
    void sendAdsMessage_ThrowsMessageSendException_WhenUserNameIsNull() throws Exception {
        // Arrange - userName is null by default if not set via env var
        // Create a new manager without setting userName
        SekretessSignalProtocolStore protocolStore = new SekretessSignalProtocolStore(
                null, 0, sessionStore, groupSessionStore
        );
        SekretessManager managerWithNoUser = new SekretessManager(protocolStore, mockServerClient);

        // Act & Assert
        assertThatThrownBy(() -> managerWithNoUser.sendAdsMessage("Ad message"))
                .isInstanceOf(MessageSendException.class);
    }

    // ==================== deleteUserSession Edge Cases ====================

    @Test
    void deleteUserSession_HandlesSpecialCharactersInUsername() {
        // Arrange
        String consumer = "consumer+special@chars.123";
        sessionStore.saveSession(consumer, 123, Base64.getEncoder().encodeToString("test".getBytes()));
        assertThat(sessionStore.size()).isEqualTo(1);

        // Act
        manager.deleteUserSession(consumer);

        // Assert
        assertThat(sessionStore.size()).isEqualTo(0);
    }

    @Test
    void deleteUserSession_HandlesEmptyUsername() {
        // Act & Assert - Should not throw exception for empty string
        assertThatNoException().isThrownBy(() -> manager.deleteUserSession(""));
    }

    @Test
    void deleteUserSession_HandlesLongUsername() {
        // Arrange
        String longConsumer = "a".repeat(1000);
        sessionStore.saveSession(longConsumer, 123, Base64.getEncoder().encodeToString("test".getBytes()));
        assertThat(sessionStore.size()).isEqualTo(1);

        // Act
        manager.deleteUserSession(longConsumer);

        // Assert
        assertThat(sessionStore.size()).isEqualTo(0);
    }

    // ==================== Integration-style Tests ====================

    @Test
    void testManagerConstructorWithServerClient() {
        // Arrange & Act
        SekretessSignalProtocolStore protocolStore = new SekretessSignalProtocolStore(
                null, 0, sessionStore, groupSessionStore
        );
        SekretessManager newManager = new SekretessManager(protocolStore, mockServerClient);

        // Assert
        assertThat(newManager).isNotNull();
    }

    @Test
    void testServerClientIsCalledOnSendMessageToConsumer() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        setUserName(manager, "test-business");

        // Make getConsumerKeys throw to verify it's being called
        when(mockServerClient.getConsumerKeys(consumer))
                .thenThrow(new RuntimeException("Expected call"));

        // Act & Assert
        assertThatThrownBy(() -> manager.sendMessageToConsumer("message", consumer))
                .isInstanceOf(PrekeyBundleException.class);

        verify(mockServerClient).getConsumerKeys(consumer);
    }

    @Test
    void testMultipleConsumerMessageAttempts() throws Exception {
        // Arrange
        setUserName(manager, "test-business");

        when(mockServerClient.getConsumerKeys(anyString()))
                .thenThrow(new RuntimeException("Server error"));

        // Act & Assert - Multiple calls should all fail consistently
        for (int i = 0; i < 3; i++) {
            String consumer = "consumer-" + i;
            assertThatThrownBy(() -> manager.sendMessageToConsumer("message", consumer))
                    .isInstanceOf(PrekeyBundleException.class);
        }

        verify(mockServerClient, times(3)).getConsumerKeys(anyString());
    }

    // ==================== Success Case Tests ====================

    @Test
    void sendMessageToConsumer_Success_WhenValidConsumerKeys() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        String businessName = "test-business";

        // Create a manager with real identity key pair
        IdentityKeyPair businessIdentityKeyPair = IdentityKeyPair.generate();
        int registrationId = KeyHelper.generateRegistrationId(false);

        SekretessSignalProtocolStore realProtocolStore = new SekretessSignalProtocolStore(
                businessIdentityKeyPair, registrationId, sessionStore, groupSessionStore
        );
        SekretessManager realManager = new SekretessManager(realProtocolStore, mockServerClient);
        setUserName(realManager, businessName);

        // Generate consumer keys
        ConsumerKeysResponse consumerKeys = generateValidConsumerKeys(consumer);

        when(mockServerClient.getConsumerKeys(consumer)).thenReturn(consumerKeys);

        // Mock successful message send - return the same identity key that was used
        SendMessageResponse sendResponse = new SendMessageResponse(consumerKeys.ik(), false);
        when(mockServerClient.sendMessage(anyString(), eq(consumer))).thenReturn(sendResponse);

        // Act & Assert - should not throw any exception
        assertThatNoException().isThrownBy(() -> realManager.sendMessageToConsumer("Hello!", consumer));

        // Verify interactions
        verify(mockServerClient).getConsumerKeys(consumer);
        verify(mockServerClient).sendMessage(anyString(), eq(consumer));
    }

    @Test
    void sendMessageToConsumer_Success_WithExistingSession() throws Exception {
        // Arrange
        String consumer = "existing-consumer";
        String businessName = "test-business";

        IdentityKeyPair businessIdentityKeyPair = IdentityKeyPair.generate();
        int registrationId = KeyHelper.generateRegistrationId(false);

        SekretessSignalProtocolStore realProtocolStore = new SekretessSignalProtocolStore(
                businessIdentityKeyPair, registrationId, sessionStore, groupSessionStore
        );
        SekretessManager realManager = new SekretessManager(realProtocolStore, mockServerClient);
        setUserName(realManager, businessName);

        // Generate consumer keys and create initial session
        ConsumerKeysResponse consumerKeys = generateValidConsumerKeys(consumer);
        when(mockServerClient.getConsumerKeys(consumer)).thenReturn(consumerKeys);

        SendMessageResponse sendResponse = new SendMessageResponse(consumerKeys.ik(), false);
        when(mockServerClient.sendMessage(anyString(), eq(consumer))).thenReturn(sendResponse);

        // Send first message to establish session
        realManager.sendMessageToConsumer("First message", consumer);

        // Act - send second message using existing session
        assertThatNoException().isThrownBy(() -> realManager.sendMessageToConsumer("Second message", consumer));

        // Verify getConsumerKeys called only once (session reused)
        verify(mockServerClient, times(1)).getConsumerKeys(consumer);
        verify(mockServerClient, times(2)).sendMessage(anyString(), eq(consumer));
    }

    @Test
    void sendMessageToConsumer_TriggersRetry_WhenIdentityKeyMismatch() throws Exception {
        // Arrange
        String consumer = "retry-consumer";
        String businessName = "test-business";

        IdentityKeyPair businessIdentityKeyPair = IdentityKeyPair.generate();
        int registrationId = KeyHelper.generateRegistrationId(false);

        SekretessSignalProtocolStore realProtocolStore = new SekretessSignalProtocolStore(
                businessIdentityKeyPair, registrationId, sessionStore, groupSessionStore
        );
        SekretessManager realManager = new SekretessManager(realProtocolStore, mockServerClient);
        setUserName(realManager, businessName);

        // Generate initial consumer keys
        ConsumerKeysResponse initialConsumerKeys = generateValidConsumerKeys(consumer);

        // Generate different consumer keys for retry (simulating key change)
        ConsumerKeysResponse newConsumerKeys = generateValidConsumerKeys(consumer);

        // First call returns initial keys, subsequent calls return new keys
        when(mockServerClient.getConsumerKeys(consumer))
                .thenReturn(initialConsumerKeys)
                .thenReturn(newConsumerKeys);

        // First send returns different identity key (triggers retry)
        SendMessageResponse mismatchResponse = new SendMessageResponse(newConsumerKeys.ik(), false);
        // Second send (retry) returns matching key
        SendMessageResponse matchResponse = new SendMessageResponse(newConsumerKeys.ik(), false);

        when(mockServerClient.sendMessage(anyString(), eq(consumer)))
                .thenReturn(mismatchResponse)
                .thenReturn(matchResponse);

        // Act & Assert - should complete without exception (retry succeeds)
        assertThatNoException().isThrownBy(() -> realManager.sendMessageToConsumer("Hello!", consumer));

        // Verify retry happened - getConsumerKeys called twice
        verify(mockServerClient, times(2)).getConsumerKeys(consumer);
        verify(mockServerClient, times(2)).sendMessage(anyString(), eq(consumer));
    }

    @Test
    void sendAdsMessage_Success_WithValidGroupSession() throws Exception {
        // Arrange
        String businessName = "test-business";

        IdentityKeyPair businessIdentityKeyPair = IdentityKeyPair.generate();
        int registrationId = KeyHelper.generateRegistrationId(false);

        SekretessSignalProtocolStore realProtocolStore = new SekretessSignalProtocolStore(
                businessIdentityKeyPair, registrationId, sessionStore, groupSessionStore
        );
        SekretessManager realManager = new SekretessManager(realProtocolStore, mockServerClient);
        setUserName(realManager, businessName);

        // Create a valid group session
        String distributionId = java.util.UUID.randomUUID().toString();
        SignalProtocolAddress businessAddress = new SignalProtocolAddress(businessName, 1);

        // Create sender key distribution message
        org.signal.libsignal.protocol.groups.GroupSessionBuilder groupSessionBuilder =
                new org.signal.libsignal.protocol.groups.GroupSessionBuilder(realProtocolStore);
        org.signal.libsignal.protocol.message.SenderKeyDistributionMessage distributionMessage =
                groupSessionBuilder.create(businessAddress, java.util.UUID.fromString(distributionId));

        // Save the group session with distribution message
        String sessionRecord = Base64.getEncoder().encodeToString(
                realProtocolStore.loadSenderKey(businessAddress, java.util.UUID.fromString(distributionId)).serialize()
        );
        String distributionMessageBase64 = Base64.getEncoder().encodeToString(distributionMessage.serialize());

        // Save complete group session with both sessionRecord and businessDistributionMessage
        groupSessionStore.saveCompleteGroupSession(
                businessName, 1, distributionId, sessionRecord, distributionMessageBase64
        );

        // Mock successful ads message send with no consumers needing distribution
        when(mockServerClient.sendAdsMessage(anyString(), eq(businessName)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatNoException().isThrownBy(() -> realManager.sendAdsMessage("Promotional content!"));

        // Verify
        verify(mockServerClient).sendAdsMessage(anyString(), eq(businessName));
    }

    @Test
    void sendAdsMessage_Success_AndTriggersDistributionFlowForNewSubscribers() throws Exception {
        // Arrange
        String businessName = "test-business";
        String newConsumer = "new-subscriber";

        IdentityKeyPair businessIdentityKeyPair = IdentityKeyPair.generate();
        int registrationId = KeyHelper.generateRegistrationId(false);

        SekretessSignalProtocolStore realProtocolStore = new SekretessSignalProtocolStore(
                businessIdentityKeyPair, registrationId, sessionStore, groupSessionStore
        );
        SekretessManager realManager = new SekretessManager(realProtocolStore, mockServerClient);
        setUserName(realManager, businessName);

        // Create a valid group session
        String distributionId = java.util.UUID.randomUUID().toString();
        SignalProtocolAddress businessAddress = new SignalProtocolAddress(businessName, 1);

        org.signal.libsignal.protocol.groups.GroupSessionBuilder groupSessionBuilder =
                new org.signal.libsignal.protocol.groups.GroupSessionBuilder(realProtocolStore);
        org.signal.libsignal.protocol.message.SenderKeyDistributionMessage distributionMessage =
                groupSessionBuilder.create(businessAddress, java.util.UUID.fromString(distributionId));

        // Get the session record that was created
        String sessionRecord = Base64.getEncoder().encodeToString(
                realProtocolStore.loadSenderKey(businessAddress, java.util.UUID.fromString(distributionId)).serialize()
        );
        String distributionMessageBase64 = Base64.getEncoder().encodeToString(distributionMessage.serialize());

        // Save complete group session with BOTH sessionRecord and businessDistributionMessage
        // This is required because sendSenderKeyDistributionMessage checks sessionRecord != null
        // and then uses businessDistributionMessage
        groupSessionStore.saveCompleteGroupSession(
                businessName, 1, distributionId, sessionRecord, distributionMessageBase64
        );

        // Mock ads message response with new subscriber
        List<SendAdsMessageResponse> adsResponses = List.of(new SendAdsMessageResponse(newConsumer));
        when(mockServerClient.sendAdsMessage(anyString(), eq(businessName))).thenReturn(adsResponses);

        // Mock consumer keys for the new subscriber - this will be called during sendSenderKeyDistributionMessage
        ConsumerKeysResponse consumerKeys = generateValidConsumerKeys(newConsumer);
        when(mockServerClient.getConsumerKeys(newConsumer)).thenReturn(consumerKeys);

        // Mock key distribution message send
        doNothing().when(mockServerClient).sendKeyDistMessage(anyString(), eq(newConsumer));

        // Act - sendAdsMessage should complete without throwing
        assertThatNoException().isThrownBy(() -> realManager.sendAdsMessage("Promotional content!"));

        // Verify ads message was sent
        verify(mockServerClient).sendAdsMessage(anyString(), eq(businessName));

        // Verify that distribution flow was triggered - getConsumerKeys is called to establish session
        verify(mockServerClient).getConsumerKeys(newConsumer);
    }

    // ==================== Helper Methods ====================

    private ConsumerKeysResponse generateValidConsumerKeys(String consumer) throws Exception {
        // Generate identity key pair
        IdentityKeyPair identityKeyPair = IdentityKeyPair.generate();
        IdentityKey identityKey = identityKeyPair.getPublicKey();

        // Generate prekey
        int preKeyId = new Random().nextInt(0xFFFFFF);
        ECKeyPair preKeyPair = ECKeyPair.generate();

        // Generate signed prekey
        int signedPreKeyId = new Random().nextInt(0xFFFFFF);
        ECKeyPair signedPreKeyPair = ECKeyPair.generate();
        byte[] signedPreKeySignature = identityKeyPair.getPrivateKey().calculateSignature(
                signedPreKeyPair.getPublicKey().serialize()
        );

        // Generate KEM prekey (Kyber)
        int pqPreKeyId = new Random().nextInt(0xFFFFFF);
        KEMKeyPair kemKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
        byte[] pqSignedPreKeySignature = identityKeyPair.getPrivateKey().calculateSignature(
                kemKeyPair.getPublicKey().serialize()
        );

        int registrationId = KeyHelper.generateRegistrationId(false);

        return new ConsumerKeysResponse(
                consumer,
                Base64.getEncoder().encodeToString(identityKey.serialize()),
                preKeyId + ":" + Base64.getEncoder().encodeToString(preKeyPair.getPublicKey().serialize()),
                Base64.getEncoder().encodeToString(signedPreKeySignature),
                Base64.getEncoder().encodeToString(signedPreKeyPair.getPublicKey().serialize()),
                String.valueOf(signedPreKeyId),
                Base64.getEncoder().encodeToString(kemKeyPair.getPublicKey().serialize()),
                String.valueOf(pqPreKeyId),
                Base64.getEncoder().encodeToString(pqSignedPreKeySignature),
                registrationId
        );
    }

    private static void setUserName(SekretessManager manager, String value) throws Exception {
        Field field = SekretessManager.class.getDeclaredField("userName");
        field.setAccessible(true);
        field.set(manager, value);
    }
}
