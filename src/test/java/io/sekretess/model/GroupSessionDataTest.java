package io.sekretess.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GroupSessionData record.
 */
class GroupSessionDataTest {

    @Test
    void testCreateGroupSessionData() {
        // Arrange
        String name = "business-123";
        int deviceId = 1;
        String distributionId = "dist-id-12345";
        String sessionRecord = "base64-group-session-record";

        // Act
        GroupSessionData groupSession = new GroupSessionData(name, deviceId, distributionId, sessionRecord);

        // Assert
        assertThat(groupSession).isNotNull();
        assertThat(groupSession.name()).isEqualTo(name);
        assertThat(groupSession.deviceId()).isEqualTo(deviceId);
        assertThat(groupSession.distributionId()).isEqualTo(distributionId);
        assertThat(groupSession.sessionRecord()).isEqualTo(sessionRecord);
    }

    @Test
    void testNameAccessor() {
        // Arrange
        String name = "test-business";
        GroupSessionData groupSession = new GroupSessionData(name, 1, "dist-id", "record");

        // Act & Assert
        assertThat(groupSession.name()).isEqualTo(name);
    }

    @Test
    void testDeviceIdAccessor() {
        // Arrange
        int deviceId = 5;
        GroupSessionData groupSession = new GroupSessionData("business", deviceId, "dist-id", "record");

        // Act & Assert
        assertThat(groupSession.deviceId()).isEqualTo(deviceId);
    }

    @Test
    void testDistributionIdAccessor() {
        // Arrange
        String distributionId = "uuid-style-dist-id";
        GroupSessionData groupSession = new GroupSessionData("business", 1, distributionId, "record");

        // Act & Assert
        assertThat(groupSession.distributionId()).isEqualTo(distributionId);
    }

    @Test
    void testSessionRecordAccessor() {
        // Arrange
        String record = "base64-session-data";
        GroupSessionData groupSession = new GroupSessionData("business", 1, "dist-id", record);

        // Act & Assert
        assertThat(groupSession.sessionRecord()).isEqualTo(record);
    }

    @Test
    void testEqualityWithSameValues() {
        // Arrange
        GroupSessionData session1 = new GroupSessionData("business", 1, "dist-1", "record");
        GroupSessionData session2 = new GroupSessionData("business", 1, "dist-1", "record");

        // Act & Assert
        assertThat(session1).isEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentName() {
        // Arrange
        GroupSessionData session1 = new GroupSessionData("business1", 1, "dist-1", "record");
        GroupSessionData session2 = new GroupSessionData("business2", 1, "dist-1", "record");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentDistributionId() {
        // Arrange
        GroupSessionData session1 = new GroupSessionData("business", 1, "dist-1", "record");
        GroupSessionData session2 = new GroupSessionData("business", 1, "dist-2", "record");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testInequalityWithDifferentSessionRecord() {
        // Arrange
        GroupSessionData session1 = new GroupSessionData("business", 1, "dist-1", "record1");
        GroupSessionData session2 = new GroupSessionData("business", 1, "dist-1", "record2");

        // Act & Assert
        assertThat(session1).isNotEqualTo(session2);
    }

    @Test
    void testWithMaxIntDeviceId() {
        // Arrange
        GroupSessionData session = new GroupSessionData("business", Integer.MAX_VALUE, "dist-id", "record");

        // Act & Assert
        assertThat(session.deviceId()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void testWithZeroDeviceId() {
        // Arrange
        GroupSessionData session = new GroupSessionData("business", 0, "dist-id", "record");

        // Act & Assert
        assertThat(session.deviceId()).isEqualTo(0);
    }

    @Test
    void testWithEmptyRecord() {
        // Arrange
        GroupSessionData session = new GroupSessionData("business", 1, "dist-id", "");

        // Act & Assert
        assertThat(session.sessionRecord()).isEmpty();
    }
}

