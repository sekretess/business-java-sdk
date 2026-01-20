package io.sekretess.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.sekretess.client.request.SendAdMessage;
import io.sekretess.client.request.SendMessage;
import io.sekretess.client.response.ConsumerKeysResponse;
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
import java.util.List;

public class SekretessServerClient {
    private final HttpClient httpClient;
    private final String businessServerUrl;
    private final TokenProvider tokenProvider;
    private static final Logger logger = LoggerFactory.getLogger(SekretessServerClient.class);


    public SekretessServerClient() {
        this.httpClient = HttpClient.newBuilder().build();
        this.tokenProvider = new TokenProvider();
        this.businessServerUrl = System.getenv("SEKRETESS_BUSINESS_SERVER_URL");
    }

    // Package-private constructor for testing
    SekretessServerClient(HttpClient httpClient, TokenProvider tokenProvider, String businessServerUrl) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.businessServerUrl = businessServerUrl;
    }

    public SendMessageResponse sendMessage(String text, String consumer) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendMessage(text, consumer))))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/messages"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenProvider.fetchToken())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send text message to consumer!" + consumer + " ,statusCode: " + response.statusCode());
        } else {
            logger.info("Successfully forwarded message for consumer! {}", consumer);
            Gson gson = new Gson();
            return gson.fromJson(response.body(), SendMessageResponse.class);
        }

    }


    public void sendKeyDistMessage(String text, String consumer) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendMessage(text, consumer))))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/messages/distributions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenProvider.fetchToken())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send key dist message to consumer!" + consumer + " ,statusCode: " + response.statusCode());
        } else {
            logger.info("Successfully forwarded key-dist message for consumer! {}", consumer);
        }
    }

    public List<SendAdsMessageResponse> sendAdsMessage(String text, String exchangeName) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(new SendAdMessage(text, exchangeName))))
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/messages/ads"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenProvider.fetchToken())
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
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(businessServerUrl + "/api/v1/businesses/consumers/" + consumer + "/key-bundles"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenProvider.fetchToken())
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


}
