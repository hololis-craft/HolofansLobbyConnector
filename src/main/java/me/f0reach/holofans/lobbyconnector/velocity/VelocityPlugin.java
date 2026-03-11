package me.f0reach.holofans.lobbyconnector.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "holofanslobbyconnector",
        name = "HolofansLobbyConnector",
        version = "1.0.0",
        description = "Lobby connection management for Holofans",
        authors = {"f0reachARR"}
)
public class VelocityPlugin {
    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from(MessageConstants.CHANNEL);

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final VelocityConfig config;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Boolean> pendingLobbyTransfer = new ConcurrentHashMap<>();
    private final Set<UUID> pendingBedSpawn = ConcurrentHashMap.newKeySet();

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = new VelocityConfig();
        this.playerDataManager = new PlayerDataManager(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            config.load(dataDirectory);
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }

        try {
            playerDataManager.load();
        } catch (IOException e) {
            logger.error("Failed to load player data", e);
        }

        server.getChannelRegistrar().register(CHANNEL);

        server.getEventManager().register(this, new VelocityMessageHandler(this));

        BrigadierCommand lobbyCommand = LobbyCommand.create(this);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("lobby").build(),
                lobbyCommand
        );

        BrigadierCommand defaultSurvivalCommand = DefaultSurvivalCommand.create(this);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("defaultsurvival").build(),
                defaultSurvivalCommand
        );

        logger.info("HolofansLobbyConnector (Velocity) enabled");
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(serverConnection -> {
            String currentServer = serverConnection.getServerInfo().getName();

            if (!currentServer.equalsIgnoreCase(config.getLobbyServer())) {
                playerDataManager.setLastServer(player.getUniqueId(), currentServer);
            }

            // If player was transferred after lobby death, teleport to bed/world spawn
            if (pendingBedSpawn.remove(player.getUniqueId())) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(MessageConstants.TELEPORT_BED_SPAWN);
                out.writeUTF(player.getUniqueId().toString());
                serverConnection.sendPluginMessage(CHANNEL, out.toByteArray());
            }
        });
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        // Only handle initial connection (player has no current server yet)
        if (player.getCurrentServer().isPresent()) return;

        if (playerDataManager.isDefaultSurvival(player.getUniqueId())) {
            server.getServer(config.getDefaultSurvivalServer()).ifPresent(s ->
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(s))
            );
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingLobbyTransfer.remove(uuid);
        pendingBedSpawn.remove(uuid);
    }

    public void transferToLobby(Player player) {
        server.getServer(config.getLobbyServer()).ifPresent(lobby ->
                player.createConnectionRequest(lobby).fireAndForget()
        );
    }

    public void transferToServer(Player player, String serverName) {
        server.getServer(serverName).ifPresent(s ->
                player.createConnectionRequest(s).fireAndForget()
        );
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public VelocityConfig getConfig() {
        return config;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public Map<UUID, Boolean> getPendingLobbyTransfer() {
        return pendingLobbyTransfer;
    }

    public Set<UUID> getPendingBedSpawn() {
        return pendingBedSpawn;
    }
}
