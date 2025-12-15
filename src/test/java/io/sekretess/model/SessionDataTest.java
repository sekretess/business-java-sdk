package io.sekretess.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SessionData record.
 */
class SessionDataTest {

    @Test
    void testCreateSessionData() {
        // Arrange
        String name = "consumer-123";
        int deviceId = 1;
        String record = "base64-encoded-session-record";

        // Act
        SessionData session = new SessionData(name, deviceId, record);

        // Assert
        assertThat(session).isNotNull();
        assertThat(session.name()).isEqualTo(name);
        assertThat(session.deviceId()).isEqualTo(deviceId);
        assertThat(session.base64SessionRecord()).isEqualTo(record);
    }

    @Test
    void testNameAccessor() {
        // Arrange
        String name = "test-consumer";
        SessionData session = new SessionData(name, 1, "record");

        // Act & Assert
        assertThat(session.name()).isEqualTo(name);
    }

    @Test
    void testDeviceIdAccessor() {
        // Arrange
        int deviceId = 42;
        SessionData session = new SessionData("consumer", deviceId, "record");

        // Act & Assert
        assertThat(session.deviceId()).isEqualTo(deviceId);
    }

    @Test
    void testBase64SessionRecordAccessor() {
        // Arrange
        String record = "long-base64-encoded-data==";
        SessionData session = new SessionData("consumer", 1, record);

        // Act & Assert
        assertThat(session.base64SessionRecord()).isEqualTo(record);
    }

    @Test
    void testEqualityWithSameValues() {
        // Arrange
        SessionData session1 = new SessionData("consumer", 1, "record");
        SessionData session2 = new SessionData("consumer", 1, "record");

        // Act & Assert
        assertThat(session1).isEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentName() {
        // Arrange
        SessionData session1 = new SessionData("consumer1", 1, "record");
        SessionData session2 = new SessionData("consumer2", 1, "record");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentDeviceId() {
        // Arrange
        SessionData session1 = new SessionData("consumer", 1, "record");
        SessionData session2 = new SessionData("consumer", 2, "record");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentRecord() {
        // Arrange
        SessionData session1 = new SessionData("consumer", 1, "record1");
        SessionData session2 = new SessionData("consumer", 1, "record2");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testWithDeviceIdZero() {
        // Arrange
        SessionData session = new SessionData("consumer", 0, "record");

        // Act & Assert
        assertThat(session.deviceId()).isEqualTo(0);
    }

    @Test
    void testWithEmptyRecord() {
        // Arrange
        SessionData session = new SessionData("consumer", 1, "");

        // Act & Assert
        assertThat(session.base64SessionRecord()).isEmpty();
    }

    @Test
    void testWithLongConsumerName() {
        // Arrange
        String longName = "very-long-consumer-name-with-many-characters-12345";
        SessionData session = new SessionData(longName, 1, "record");

        // Act & Assert
        assertThat(session.name()).isEqualTo(longName);
    }
}

