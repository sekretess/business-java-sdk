package io.sekretess.util;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 16;

    public static char[] generatePassword() {
        return generatePassword(DEFAULT_LENGTH).toCharArray();
    }

    public static String generatePassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        String alphabet = DEFAULT_ALPHABET;

        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(alphabet.length());
            sb.append(alphabet.charAt(idx));
        }
        return sb.toString();
    }
}

