package com.wifichat.admin.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SessionStore {
    private final Path sessionFile;

    public SessionStore(String profileName) {
        String safeProfile = (profileName == null || profileName.isBlank()) ? "admin" : profileName.trim();
        Path baseDir = Path.of(System.getProperty("user.home"), ".wifichat-admin");
        this.sessionFile = baseDir.resolve("session-" + safeProfile + ".bin");
    }

    public AuthSession load() {
        if (!Files.exists(sessionFile)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(sessionFile))) {
            Object obj = in.readObject();
            return obj instanceof AuthSession session ? session : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void save(AuthSession session) {
        if (session == null) {
            return;
        }
        try {
            Files.createDirectories(sessionFile.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(sessionFile))) {
                out.writeObject(session);
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
}
