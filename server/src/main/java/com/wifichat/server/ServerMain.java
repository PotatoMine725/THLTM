package com.wifichat.server;

import com.wifichat.server.auth.PasswordService;
import com.wifichat.server.db.ChatRepository;
import com.wifichat.server.model.UserAccount;
import com.wifichat.server.net.TcpChatServer;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = parseIntArg(args, "--port", 61000);
        String dbArg = stringArg(args, "--db", null);

        String dbPath;
        if (dbArg == null || dbArg.isBlank()) {
            Path dataDir = Path.of(System.getProperty("user.home"), ".wifichat-server");
            Files.createDirectories(dataDir);
            dbPath = dataDir.resolve("chat.db").toString();
        } else {
            Path dbFile = Path.of(dbArg);
            Path parent = dbFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            dbPath = dbFile.toString();
        }

        ChatRepository repository = new ChatRepository(dbPath);
        repository.init();
        bootstrapAdmin(repository);

        TcpChatServer server = new TcpChatServer(port, repository);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("Starting WiFi Chat TCP server...");
        System.out.println("Port: " + port);
        System.out.println("DB: " + dbPath);

        server.start();
    }

    private static void bootstrapAdmin(ChatRepository repository) throws Exception {
        final String defaultAdminUser = "admin";
        final String defaultAdminPass = "admin123";
        UserAccount existing = repository.findUserByUsername(defaultAdminUser);
        if (existing != null) {
            return;
        }

        PasswordService passwordService = new PasswordService();
        String salt = passwordService.newSaltBase64();
        String hash = passwordService.hash(defaultAdminPass, salt);
        repository.createUserWithRole(defaultAdminUser, "System Admin", hash, salt, ChatRepository.ROLE_ADMIN);

        System.out.println("[bootstrap] Created admin account");
        System.out.println("[bootstrap] username: admin");
        System.out.println("[bootstrap] password: admin123");
    }

    private static int parseIntArg(String[] args, String name, int defaultValue) {
        String value = stringArg(args, name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String stringArg(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (!name.equals(args[i])) {
                continue;
            }
            if (i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
