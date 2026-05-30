package io.sekretess.client.response;

public record FileUploadResponse(String fileId, String fileToken, String expiresAt) {}
