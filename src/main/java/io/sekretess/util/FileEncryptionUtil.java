package io.sekretess.util;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class FileEncryptionUtil {

    private static final int AES_KEY_SIZE_BYTES = 32;
    private static final int GCM_IV_SIZE_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private FileEncryptionUtil() {
    }

    public static EncryptedFilePayload encrypt(Path inputFile) throws IOException, GeneralSecurityException {
        byte[] key = new byte[AES_KEY_SIZE_BYTES];
        byte[] iv = new byte[GCM_IV_SIZE_BYTES];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        Path encryptedFile = Files.createTempFile("sekretess-file-", ".bin");
        long plaintextSize = Files.size(inputFile);

        try (InputStream inputStream = Files.newInputStream(inputFile);
             OutputStream outputStream = Files.newOutputStream(encryptedFile, StandardOpenOption.TRUNCATE_EXISTING);
             DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
             CipherOutputStream cipherOutputStream = new CipherOutputStream(digestOutputStream, cipher)) {
            inputStream.transferTo(cipherOutputStream);
        } catch (IOException e) {
            Files.deleteIfExists(encryptedFile);
            throw e;
        }

        String mimeType = Files.probeContentType(inputFile);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        return new EncryptedFilePayload(
                encryptedFile,
                Base64.getEncoder().encodeToString(key),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(messageDigest.digest()),
                plaintextSize,
                Files.size(encryptedFile),
                mimeType
        );
    }
}
