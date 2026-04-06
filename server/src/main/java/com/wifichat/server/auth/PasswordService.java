package com.wifichat.server.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordService {
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    public String newSaltBase64() {
        byte[] bytes = new byte[SALT_LENGTH];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String hash(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hash error", e);
        }
    }

    public boolean verify(String plainPassword, String saltBase64, String expectedHash) {
        String computed = hash(plainPassword, saltBase64);
        return computed.equals(expectedHash);
    }
}
