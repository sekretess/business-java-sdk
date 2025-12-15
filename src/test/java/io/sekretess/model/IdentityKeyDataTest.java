package io.sekretess.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for IdentityKeyData record.
 */
class IdentityKeyDataTest {

    @Test
    void testCreateIdentityKeyData() {
        // Arrange
        String username = "test-user";
        byte[] keyPair = new byte[]{1, 2, 3, 4, 5};
        int registrationId = 12345;

        // Act
        IdentityKeyData data = new IdentityKeyData(username, keyPair, registrationId);

        // Assert
        assertThat(data).isNotNull();
        assertThat(data.username()).isEqualTo(username);
        assertThat(data.serializedIdentityKeyPair()).isEqualTo(keyPair);
        assertThat(data.registrationId()).isEqualTo(registrationId);
    }

    @Test
    void testUsernameAccessor() {
        // Arrange
        String username = "business-user";
        IdentityKeyData data = new IdentityKeyData(username, new byte[]{1}, 100);

        // Act & Assert
        assertThat(data.username()).isEqualTo(username);
    }

    @Test
    void testRegistrationIdAccessor() {
        // Arrange
        int regId = 54321;
        IdentityKeyData data = new IdentityKeyData("user", new byte[]{1}, regId);

        // Act & Assert
        assertThat(data.registrationId()).isEqualTo(regId);
    }

    @Test
    void testSerializedKeyPairAccessor() {
        // Arrange
        byte[] keyPair = new byte[]{10, 20, 30, 40};
        IdentityKeyData data = new IdentityKeyData("user", keyPair, 100);

        // Act & Assert
        assertThat(data.serializedIdentityKeyPair()).isEqualTo(keyPair);
    }

    @Test
    void testEqualityWithSameValues() {
        // Arrange
        byte[] keyPair = new byte[]{1, 2, 3};
        IdentityKeyData data1 = new IdentityKeyData("user", keyPair, 100);
        IdentityKeyData data2 = new IdentityKeyData("user", keyPair, 100);

        // Act & Assert
        assertThat(data1).isEqualTo(data2);
    }

    @Test
    void testInequalityWithDifferentUsername() {
        // Arrange
        byte[] keyPair = new byte[]{1, 2, 3};
        IdentityKeyData data1 = new IdentityKeyData("user1", keyPair, 100);
        IdentityKeyData data2 = new IdentityKeyData("user2", keyPair, 100);

        // Act & Assert
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testInequalityWithDifferentRegistrationId() {
        // Arrange
        byte[] keyPair = new byte[]{1, 2, 3};
        IdentityKeyData data1 = new IdentityKeyData("user", keyPair, 100);
        IdentityKeyData data2 = new IdentityKeyData("user", keyPair, 200);

        // Act & Assert
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testWithLargeRegistrationId() {
        // Arrange
        int largeRegId = Integer.MAX_VALUE;
        IdentityKeyData data = new IdentityKeyData("user", new byte[]{1}, largeRegId);

        // Act & Assert
        assertThat(data.registrationId()).isEqualTo(largeRegId);
    }

    @Test
    void testWithEmptyKeyPair() {
        // Arrange
        byte[] emptyKeyPair = new byte[0];
        IdentityKeyData data = new IdentityKeyData("user", emptyKeyPair, 100);

        // Act & Assert
        assertThat(data.serializedIdentityKeyPair()).isEmpty();
    }

    @Test
    void testWithZeroRegistrationId() {
        // Arrange
        IdentityKeyData data = new IdentityKeyData("user", new byte[]{1}, 0);

        // Act & Assert
        assertThat(data.registrationId()).isEqualTo(0);
    }
}

