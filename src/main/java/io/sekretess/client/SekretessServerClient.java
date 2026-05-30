package io.sekretess.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.sekretess.client.request.SendAdMessage;
import io.sekretess.client.request.SendMessage;
import io.sekretess.client.response.ConsumerKeysResponse;
import io.sekretess.client.response.FileUploadResponse;
import io.sekretess.client.response.SendAdsMessageResponse;
import io.sekretess.client.response.SendMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class SekretessServerClient {

    public enum AuthMode {
        MTLS, API_KEY
    }

    private final HttpClient httpClient;
    private final String businessServerUrl;
    private final AuthMode authMode;
    private final TokenProvider tokenProvider;
    private final String apiKeyCredentials;
    private static final Logger logger = LoggerFactory.getLogger(SekretessServerClient.class);

    public SekretessServerClient() {
        this.businessServerUrl = System.getenv("SEKRETESS_BUSINESS_SERVER_URL");
        this.authMode = resolveAuthMode(System.getenv("SEKRETESS_AUTH_MODE"));
        this.httpClient = HttpClient.newBuilder().build();

        if (this.authMode == AuthMode.MTLS) {
            this.tokenProvider = new TokenProvider();
            this.apiKeyCredentials = null;
        } else {
            String apiKey = System.getenv("SEKRETESS_API_KEY");
            String apiSecret = System.getenv("SEKRETESS_API_SECRET");
            if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
                throw new IllegalStateException(
                        "SEKRETESS_API_KEY and SEKRETESS_API_SECRET are required when SEKRETESS_AUTH_MODE=api_key");
            }
            this.tokenProvider = null;
            this.apiKeyCredentials = Base64.getEncoder()
                    .encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));
        }
    }

    // Package-private constructor for testing (mtls mode)
    SekretessServerClient(HttpClient httpClient, TokenProvider tokenProvider, String businessServerUrl) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.businessServerUrl = businessServerUrl;
        this.authMode = AuthMode.MTLS;
        this.apiKeyCredentials = null;
    }

    // Package-private constructor for testing (api_key mode)
    SekretessServerClient(HttpClient httpClient, String apiKey, String apiSecret, String businessServerUrl) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "apiKey and apiSecret must not be blank");
        }
        this.httpClient = httpClient;
        this.tokenProvider = null;
        this.businessServerUrl = businessServerUrl;
        this.authMode = AuthMode.API_KEY;
        this.apiKeyCredentials = Base64.getEncoder()
                .encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));
    }

    private static AuthMode resolveAuthMode(String authModeEnv) {
        if (authModeEnv == null || authModeEnv.isBlank()) {
            throw new IllegalStateException(
                    "SEKRETESS_AUTH_MODE environment variable is required. Supported values: 'mtls', 'api_key'");
        }
        return switch (authModeEnv.toLowerCase().replace("-", "_")) {
            case "mtls" -> AuthMode.MTLS;
            case "api_key" -> AuthMode.API_KEY;
            default -> throw new IllegalStateException(
                    "Unsupported SEKRETESS_AUTH_MODE: '" + authModeEnv + "'. Supported values: 'mtls', 'api_key'");
        };
    }

    private HttpRequest.Builder withAuthentication(HttpRequest.Builder builder) {
        if (authMode == AuthMode.API_KEY) {
            return builder.header("Authorization", "Basic " + apiKeyCredentials);
        }
        return builder.header("Authorization", "Bearer " + tokenProvider.fetchToken());
    }

    public SendMessageResponse sendMessage(String text, String consumer) throws IOException, InterruptedException {
        return sendMessageToPath("/api/v1/businesses/messages", text, consumer);
    }

    public SendMessageResponse sendFileMessage(String text, String consumer) throws IOException, InterruptedException {
        return sendMessageToPath("/api/v1/businesses/messages/files", text, consumer);
    }

    public void sendKeyDistMessage(String text, String consumer) throws IOException, InterruptedException {
        HttpRequest httpRequest = withAuthentication(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendMessage(text, consumer))))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/messages/distributions"))
                .header("Content-Type", "application/json"))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send key dist message to consumer!" + consumer + " ,statusCode: " + response.statusCode());
        } else {
            logger.info("Successfully forwarded key-dist message for consumer! {}", consumer);
        }
    }

    public List<SendAdsMessageResponse> sendAdsMessage(String text, String exchangeName) throws IOException, InterruptedException {
        HttpRequest httpRequest = withAuthentication(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendAdMessage(text, exchangeName))))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/messages/ads"))
                .header("Content-Type", "application/json"))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) {
            throw new RuntimeException("Failed to send ads message to exchange! " + exchangeName + " statusCode: " + response.statusCode());
        } else {
            logger.info("Successfully forwarded ads message to exchange! {}", exchangeName);
            Type listType = new TypeToken<List<SendAdsMessageResponse>>() {
            }.getType();
            return new Gson().fromJson(response.body(), listType);
        }
    }

    public ConsumerKeysResponse getConsumerKeys(String consumer) throws IOException, InterruptedException {
        HttpRequest httpRequest = withAuthentication(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/consumers/" + consumer + "/key-bundles"))
                .header("Content-Type", "application/json"))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Gson gson = new Gson();
            ConsumerKeysResponse consumerKeysResponse = gson.fromJson(response.body(), ConsumerKeysResponse.class);
            logger.debug("Received response from server for consumer: {}, {}", consumer, consumerKeysResponse);
            return consumerKeysResponse;
        } else {
            throw new RuntimeException("Exception happened when fetching consumer keys from sekretess! consumer: " + consumer + " statusCode: " + response.statusCode());
        }
    }

    public FileUploadResponse uploadFile(Path encryptedFile, String consumer) throws IOException, InterruptedException {
        String boundary = "----SekretessBoundary" + UUID.randomUUID();
        String fileName = encryptedFile.getFileName().toString();
        String consumerPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"consumerName\"\r\n\r\n"
                + consumer + "\r\n";
        String filePartHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String closingBoundary = "\r\n--" + boundary + "--\r\n";

        HttpRequest request = withAuthentication(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(consumerPart.getBytes(StandardCharsets.UTF_8)),
                        HttpRequest.BodyPublishers.ofByteArray(filePartHeader.getBytes(StandardCharsets.UTF_8)),
                        HttpRequest.BodyPublishers.ofFile(encryptedFile),
                        HttpRequest.BodyPublishers.ofByteArray(closingBoundary.getBytes(StandardCharsets.UTF_8))
                ))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/uploads"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to upload encrypted file for consumer! " + consumer + " ,statusCode: " + response.statusCode());
        }

        logger.info("Successfully uploaded encrypted file for consumer! {}", consumer);
        return new Gson().fromJson(response.body(), FileUploadResponse.class);
    }

    private SendMessageResponse sendMessageToPath(String path, String text, String consumer) throws IOException, InterruptedException {
        HttpRequest httpRequest = withAuthentication(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendMessage(text, consumer))))
                .uri(URI.create(businessServerUrl + path))
                .header("Content-Type", "application/json"))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send text message to consumer!" + consumer + " ,statusCode: " + response.statusCode());
        }

        logger.info("Successfully forwarded message for consumer! {}", consumer);
        return new Gson().fromJson(response.body(), SendMessageResponse.class);
    }

}
