package io.sekretess.model;

public record FileMessageData(
        String kind,
        String algorithm,
        String digestAlgorithm,
        String fileId,
        String fileToken,
        String fileKey,
        String iv,
        String ciphertextSha256,
        long plaintextSize,
        long ciphertextSize,
        String mimeType
) {}
