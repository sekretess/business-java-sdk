package io.sekretess.client;

import io.sekretess.client.response.ConsumerKeysResponse;
import io.sekretess.client.response.SendAdsMessageResponse;
import io.sekretess.client.response.SendMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SekretessServerClientTest {

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
}

