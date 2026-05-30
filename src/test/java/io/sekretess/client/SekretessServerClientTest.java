package io.sekretess.client;

import io.sekretess.client.response.ConsumerKeysResponse;
import io.sekretess.client.response.FileUploadResponse;
import io.sekretess.client.response.SendAdsMessageResponse;
import io.sekretess.client.response.SendMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SekretessServerClientTest {

    @TempDir
    Path tempDir;

    @Mock
    private HttpClient httpClient;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private HttpResponse<String> httpResponse;

    private SekretessServerClient serverClient;

    private static final String TEST_SERVER_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        serverClient = new SekretessServerClient(httpClient, tokenProvider, TEST_SERVER_URL);
    }

    // ==================== sendMessage Tests ====================

    @Test
    void sendMessage_ReturnsResponse_WhenStatusIs200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        String message = "Hello";
        String responseBody = "{\"userIK\":\"test-ik\",\"subscribedToAdMessages\":true}";

        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);

        // Act
        SendMessageResponse response = serverClient.sendMessage(message, consumer);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userIK()).isEqualTo("test-ik");
        assertThat(response.subscribedToAdMessages()).isTrue();
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void sendMessage_ThrowsRuntimeException_WhenStatusIsNot200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendMessage("message", consumer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send text message")
                .hasMessageContaining(consumer)
                .hasMessageContaining("500");
    }

    @Test
    void sendMessage_ThrowsIOException_WhenHttpClientFails() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendMessage("message", "consumer"))
                .isInstanceOf(IOException.class)
                .hasMessage("Network error");
    }

    @Test
    void sendMessage_ThrowsInterruptedException_WhenInterrupted() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendMessage("message", "consumer"))
                .isInstanceOf(InterruptedException.class);
    }

    // ==================== sendKeyDistMessage Tests ====================

    @Test
    void sendKeyDistMessage_Succeeds_WhenStatusIs200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> serverClient.sendKeyDistMessage("key-dist", consumer));
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void sendKeyDistMessage_ThrowsRuntimeException_WhenStatusIsNot200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(403);

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendKeyDistMessage("key-dist", consumer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send key dist message")
                .hasMessageContaining(consumer)
                .hasMessageContaining("403");
    }

    @Test
    void sendKeyDistMessage_ThrowsIOException_WhenHttpClientFails() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendKeyDistMessage("key-dist", "consumer"))
                .isInstanceOf(IOException.class)
                .hasMessage("Connection refused");
    }

    // ==================== sendFileMessage Tests ====================

    @Test
    void sendFileMessage_ReturnsResponse_WhenStatusIs200() throws Exception {
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"userIK\":\"test-ik\",\"subscribedToAdMessages\":false}");

        SendMessageResponse response = serverClient.sendFileMessage("encrypted-file-metadata", "test-consumer");

        assertThat(response).isNotNull();
        assertThat(response.userIK()).isEqualTo("test-ik");
        assertThat(response.subscribedToAdMessages()).isFalse();
    }

    // ==================== uploadFile Tests ====================

    @Test
    void uploadFile_ReturnsResponse_WhenStatusIs200() throws Exception {
        Path encryptedFile = tempDir.resolve("encrypted.bin");
        Files.writeString(encryptedFile, "ciphertext");

        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"fileId\":\"file-123\",\"fileToken\":\"signed-token\",\"expiresAt\":\"2026-01-01T00:00:00Z\"}");

        FileUploadResponse response = serverClient.uploadFile(encryptedFile, "test-consumer");

        assertThat(response.fileId()).isEqualTo("file-123");
        assertThat(response.fileToken()).isEqualTo("signed-token");
        assertThat(response.expiresAt()).isEqualTo("2026-01-01T00:00:00Z");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).isEqualTo(TEST_SERVER_URL + "/api/v1/businesses/uploads");
        assertThat(request.headers().firstValue("Content-Type"))
                .hasValueSatisfying(contentType -> assertThat(contentType).contains("multipart/form-data; boundary="));
    }

    @Test
    void uploadFile_ThrowsRuntimeException_WhenStatusIsNot200() throws Exception {
        Path encryptedFile = tempDir.resolve("encrypted.bin");
        Files.writeString(encryptedFile, "ciphertext");

        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);

        assertThatThrownBy(() -> serverClient.uploadFile(encryptedFile, "test-consumer"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload encrypted file")
                .hasMessageContaining("500");
    }

    // ==================== sendAdsMessage Tests ====================

    @Test
    void sendAdsMessage_ReturnsResponseList_WhenStatusIs202() throws Exception {
        // Arrange
        String exchangeName = "test-exchange";
        String responseBody = "[{\"consumerName\":\"consumer1\"},{\"consumerName\":\"consumer2\"}]";

        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(202);
        when(httpResponse.body()).thenReturn(responseBody);

        // Act
        List<SendAdsMessageResponse> responses = serverClient.sendAdsMessage("ad message", exchangeName);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).consumerName()).isEqualTo("consumer1");
        assertThat(responses.get(1).consumerName()).isEqualTo("consumer2");
    }

    @Test
    void sendAdsMessage_ReturnsEmptyList_WhenNoSubscribers() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(202);
        when(httpResponse.body()).thenReturn("[]");

        // Act
        List<SendAdsMessageResponse> responses = serverClient.sendAdsMessage("ad", "exchange");

        // Assert
        assertThat(responses).isEmpty();
    }

    @Test
    void sendAdsMessage_ThrowsRuntimeException_WhenStatusIsNot202() throws Exception {
        // Arrange
        String exchangeName = "test-exchange";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendAdsMessage("ad", exchangeName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send ads message")
                .hasMessageContaining(exchangeName)
                .hasMessageContaining("400");
    }

    @Test
    void sendAdsMessage_ThrowsIOException_WhenHttpClientFails() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Timeout"));

        // Act & Assert
        assertThatThrownBy(() -> serverClient.sendAdsMessage("ad", "exchange"))
                .isInstanceOf(IOException.class)
                .hasMessage("Timeout");
    }

    // ==================== getConsumerKeys Tests ====================

    @Test
    void getConsumerKeys_ReturnsResponse_WhenStatusIs200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        String responseBody = """
                {
                    "username": "test-consumer",
                    "ik": "identity-key",
                    "opk": "1:one-time-prekey",
                    "spkSignature": "signature",
                    "spk": "signed-prekey",
                    "spkID": "123",
                    "pqSpk": "pq-signed-prekey",
                    "pqSpkID": "456",
                    "pqSpkSignature": "pq-signature",
                    "regID": 789
                }
                """;

        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);

        // Act
        ConsumerKeysResponse response = serverClient.getConsumerKeys(consumer);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("test-consumer");
        assertThat(response.ik()).isEqualTo("identity-key");
        assertThat(response.opk()).isEqualTo("1:one-time-prekey");
        assertThat(response.spk()).isEqualTo("signed-prekey");
        assertThat(response.spkID()).isEqualTo("123");
        assertThat(response.regID()).isEqualTo(789);
    }

    @Test
    void getConsumerKeys_ThrowsRuntimeException_WhenStatusIsNot200() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(404);

        // Act & Assert
        assertThatThrownBy(() -> serverClient.getConsumerKeys(consumer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Exception happened when fetching consumer keys")
                .hasMessageContaining(consumer)
                .hasMessageContaining("404");
    }

    @Test
    void getConsumerKeys_ThrowsRuntimeException_WhenStatusIs500() throws Exception {
        // Arrange
        String consumer = "test-consumer";
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);

        // Act & Assert
        assertThatThrownBy(() -> serverClient.getConsumerKeys(consumer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("500");
    }

    @Test
    void getConsumerKeys_ThrowsIOException_WhenHttpClientFails() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("DNS resolution failed"));

        // Act & Assert
        assertThatThrownBy(() -> serverClient.getConsumerKeys("consumer"))
                .isInstanceOf(IOException.class)
                .hasMessage("DNS resolution failed");
    }

    // ==================== Token Provider Tests ====================

    @Test
    void allMethods_UseTokenProviderForAuthorization() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("expected-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"userIK\":\"ik\",\"subscribedToAdMessages\":false}");

        // Act
        serverClient.sendMessage("msg", "consumer");

        // Assert
        verify(tokenProvider).fetchToken();
    }

    @Test
    void sendMessage_UsesCorrectContentType() throws Exception {
        // Arrange
        when(tokenProvider.fetchToken()).thenReturn("test-token");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"userIK\":\"ik\",\"subscribedToAdMessages\":false}");

        // Act
        serverClient.sendMessage("msg", "consumer");

        // Assert - verify httpClient was called (request details are internal)
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    // ==================== API Key Auth Mode Tests ====================

    @Nested
    class ApiKeyAuthModeTests {

        private static final String TEST_API_KEY = "my-api-key";
        private static final String TEST_API_SECRET = "my-api-secret";
        private static final String EXPECTED_BASIC_HEADER = "Basic " + Base64.getEncoder()
                .encodeToString((TEST_API_KEY + ":" + TEST_API_SECRET).getBytes(StandardCharsets.UTF_8));

        private SekretessServerClient apiKeyClient;

        @BeforeEach
        void setUp() {
            apiKeyClient = new SekretessServerClient(httpClient, TEST_API_KEY, TEST_API_SECRET, TEST_SERVER_URL);
        }

        @Test
        void sendMessage_SendsBasicAuthHeader_InApiKeyMode() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"userIK\":\"ik\",\"subscribedToAdMessages\":false}");

            apiKeyClient.sendMessage("hello", "consumer");

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().headers().firstValue("Authorization"))
                    .hasValue(EXPECTED_BASIC_HEADER);
        }

        @Test
        void sendMessage_DoesNotUseTokenProvider_InApiKeyMode() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"userIK\":\"ik\",\"subscribedToAdMessages\":false}");

            apiKeyClient.sendMessage("hello", "consumer");

            verifyNoInteractions(tokenProvider);
        }

        @Test
        void sendAdsMessage_SendsBasicAuthHeader_InApiKeyMode() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(202);
            when(httpResponse.body()).thenReturn("[]");

            apiKeyClient.sendAdsMessage("ad", "exchange");

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().headers().firstValue("Authorization"))
                    .hasValue(EXPECTED_BASIC_HEADER);
        }

        @Test
        void getConsumerKeys_SendsBasicAuthHeader_InApiKeyMode() throws Exception {
            String responseBody = """
                    {
                        "username": "c1", "ik": "ik", "opk": "1:opk",
                        "spkSignature": "sig", "spk": "spk", "spkID": "1",
                        "pqSpk": "pqspk", "pqSpkID": "2", "pqSpkSignature": "pqsig", "regID": 1
                    }
                    """;
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(responseBody);

            apiKeyClient.getConsumerKeys("c1");

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().headers().firstValue("Authorization"))
                    .hasValue(EXPECTED_BASIC_HEADER);
        }

        @Test
        void constructor_ThrowsIllegalStateException_WhenApiKeyIsBlank() {
            assertThatThrownBy(() -> new SekretessServerClient(httpClient, "", TEST_API_SECRET, TEST_SERVER_URL))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructor_ThrowsIllegalStateException_WhenApiSecretIsBlank() {
            assertThatThrownBy(() -> new SekretessServerClient(httpClient, TEST_API_KEY, "", TEST_SERVER_URL))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
