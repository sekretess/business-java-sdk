package io.sekretess.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 */
class ExceptionTest {

    @Test
    void testMessageSendExceptionWithMessage() {
        // Arrange
        String message = "Failed to send message to consumer";

        // Act
        MessageSendException exception = new MessageSendException(message);

        // Assert
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testMessageSendExceptionCanBeThrown() {
        // Arrange
        String message = "Message send failed";

        // Act & Assert
        assertThatThrownBy(() -> {
            throw new MessageSendException(message);
        }).isInstanceOf(MessageSendException.class)
         .hasMessage(message);
    }

    @Test
    void testSessionCreationExceptionWithMessage() {
        // Arrange
        String message = "Failed to create session";

        // Act
        SessionCreationException exception = new SessionCreationException(message);

        // Assert
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testSessionCreationExceptionCanBeThrown() {
        // Arrange
        String message = "Session creation failed";

        // Act & Assert
        assertThatThrownBy(() -> {
            throw new SessionCreationException(message);
        }).isInstanceOf(SessionCreationException.class)
         .hasMessage(message);
    }

    @Test
    void testPrekeyBundleExceptionWithMessage() {
        // Arrange
        String message = "Invalid prekey bundle";

        // Act
        PrekeyBundleException exception = new PrekeyBundleException(message);

        // Assert
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testPrekeyBundleExceptionCanBeThrown() {
        // Arrange
        String message = "Prekey bundle validation failed";

        // Act & Assert
        assertThatThrownBy(() -> {
            throw new PrekeyBundleException(message);
        }).isInstanceOf(PrekeyBundleException.class)
         .hasMessage(message);
    }

    @Test
    void testRetryMessageExceptionWithMessage() {
        // Arrange
        String message = "Message needs retry";

        // Act
        RetryMessageException exception = new RetryMessageException(message);

        // Assert
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void testRetryMessageExceptionCanBeThrown() {
        // Arrange
        String message = "Retry required";

        // Act & Assert
        assertThatThrownBy(() -> {
            throw new RetryMessageException(message);
        }).isInstanceOf(RetryMessageException.class)
         .hasMessage(message);
    }

    @Test
    void testExceptionWithEmptyMessage() {
        // Arrange
        String emptyMessage = "";

        // Act
        MessageSendException exception = new MessageSendException(emptyMessage);

        // Assert
        assertThat(exception.getMessage()).isEmpty();
    }

    @Test
    void testExceptionWithLongMessage() {
        // Arrange
        String longMessage = "A".repeat(500);

        // Act
        MessageSendException exception = new MessageSendException(longMessage);

        // Assert
        assertThat(exception.getMessage()).hasSize(500);
    }

    @Test
    void testMultipleExceptionsIndependently() {
        // Act & Assert
        assertThatThrownBy(() -> {
            throw new MessageSendException("Error 1");
        }).isInstanceOf(MessageSendException.class);

        assertThatThrownBy(() -> {
            throw new SessionCreationException("Error 2");
        }).isInstanceOf(SessionCreationException.class);

        assertThatThrownBy(() -> {
            throw new PrekeyBundleException("Error 3");
        }).isInstanceOf(PrekeyBundleException.class);
    }
}

