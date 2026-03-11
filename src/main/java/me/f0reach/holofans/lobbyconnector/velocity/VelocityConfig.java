package me.f0reach.holofans.lobbyconnector.velocity;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VelocityConfig {
    private String lobbyServer = "lobby";
    private String defaultSurvivalServer = "survival";
    private final Map<String, ServerConfig> servers = new HashMap<>();

    public static class ServerConfig {
        private final String mode;
        private final int delaySeconds;

        public ServerConfig(String mode, int delaySeconds) {
            this.mode = mode;
            this.delaySeconds = delaySeconds;
        }

        public String getMode() {
            return mode;
        }

        public int getDelaySeconds() {
            return delaySeconds;
        }

        public boolean isDelayed() {
            return "delayed".equalsIgnoreCase(mode);
        }
    }

    public void load(Path dataDirectory) throws IOException {
        Path configPath = dataDirectory.resolve("config.toml");

        if (!Files.exists(configPath)) {
            Files.createDirectories(dataDirectory);
            try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                }
            }
        }

        try (CommentedFileConfig config = CommentedFileConfig.of(configPath)) {
            config.load();

            lobbyServer = config.getOrElse("lobby-server", "lobby");
            defaultSurvivalServer = config.getOrElse("default-survival-server", "survival");

            Config serversConfig = config.get("servers");
            if (serversConfig != null) {
                for (Config.Entry entry : serversConfig.entrySet()) {
                    String serverName = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof Config serverNode) {
                        String mode = serverNode.getOrElse("mode", "immediate");
                        int delay = serverNode.getOrElse("delay-seconds", 5);
                        servers.put(serverName, new ServerConfig(mode, delay));
                    }
                }
            }
        }
    }

    public String getLobbyServer() {
        return lobbyServer;
    }

    public String getDefaultSurvivalServer() {
        return defaultSurvivalServer;
    }

    public ServerConfig getServerConfig(String serverName) {
        return servers.getOrDefault(serverName, new ServerConfig("immediate", 0));
    }
}
