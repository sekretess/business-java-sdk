package io.sekretess.client.request;

public record SendMessage(String text, String consumerName, String type) {}
