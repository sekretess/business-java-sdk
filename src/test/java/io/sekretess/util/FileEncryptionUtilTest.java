package io.sekretess.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FileEncryptionUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void encrypt_ReturnsDecryptablePayloadWithExpectedCryptoSizes() throws Exception {
        byte[] plaintext = "sekretess-file-message".getBytes(StandardCharsets.UTF_8);
        Path inputFile = tempDir.resolve("payload.txt");
        Files.write(inputFile, plaintext);

        EncryptedFilePayload payload = FileEncryptionUtil.encrypt(inputFile);
        try {
            byte[] key = Base64.getDecoder().decode(payload.encodedKey());
            byte[] iv = Base64.getDecoder().decode(payload.encodedIv());
            byte[] ciphertext = Files.readAllBytes(payload.encryptedFilePath());

            assertThat(key).hasSize(32);
            assertThat(iv).hasSize(12);
            assertThat(Base64.getDecoder().decode(payload.ciphertextSha256())).hasSize(32);
            assertThat(payload.plaintextSize()).isEqualTo(plaintext.length);
            assertThat(payload.ciphertextSize()).isEqualTo(ciphertext.length);
            assertThat(payload.mimeType()).isNotBlank();
            assertThat(ciphertext).isNotEqualTo(plaintext);

            String digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(ciphertext));
            assertThat(digest).isEqualTo(payload.ciphertextSha256());

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);

            assertThat(decrypted).isEqualTo(plaintext);
        } finally {
            payload.deleteTempFile();
        }

        assertThat(Files.exists(payload.encryptedFilePath())).isFalse();
    }
}
