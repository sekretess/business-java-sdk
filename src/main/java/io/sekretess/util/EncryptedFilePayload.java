package io.sekretess.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record EncryptedFilePayload(
        Path encryptedFilePath,
        String encodedKey,
        String encodedIv,
        String ciphertextSha256,
        long plaintextSize,
        long ciphertextSize,
        String mimeType
) {
    public void deleteTempFile() throws IOException {
        Files.deleteIfExists(encryptedFilePath);
    }
}
