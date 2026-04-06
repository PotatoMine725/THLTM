package com.wifichat.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SessionStore {
    private final Path sessionFile;

    public SessionStore() {
        this("default");
    }

    public SessionStore(String profileName) {
        Path dir = Path.of(System.getProperty("user.home"), ".wifichat");
        String safeProfile = sanitizeProfile(profileName);
        this.sessionFile = dir.resolve("session-" + safeProfile + ".bin");
    }

    public AuthSession load() {
        try {
            if (!Files.exists(sessionFile)) {
                return null;
            }
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(sessionFile))) {
                Object value = in.readObject();
                if (value instanceof AuthSession session) {
                    return session;
                }
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void save(AuthSession session) {
        if (session == null) {
            clear();
            return;
        }
        try {
            Files.createDirectories(sessionFile.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(sessionFile))) {
                out.writeObject(session);
                out.flush();
            }
        } catch (IOException ignored) {
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException ignored) {
        }
    }

    private String sanitizeProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return "default";
        }
        String safe = profileName.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        if (safe.isBlank()) {
            return "default";
        }
        return safe;
    }
}
