package me.f0reach.holofans.lobbyconnector.paper;

import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import me.f0reach.holofans.lobbyconnector.common.PluginMessage;
import me.f0reach.holofans.lobbyconnector.common.PluginMessageCodec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PaperMessageHandler implements PluginMessageListener {
    private final PaperPlugin plugin;

    public PaperMessageHandler(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(MessageConstants.CHANNEL)) return;

        PluginMessage pluginMessage = PluginMessageCodec.deserialize(message);

        switch (pluginMessage) {
            case PluginMessage.TeleportSpawn teleportSpawn -> handleTeleportSpawn(teleportSpawn);
            case PluginMessage.StartDamageTracking startDamageTracking ->
                    handleStartDamageTracking(startDamageTracking);
            case PluginMessage.CancelDamageTracking cancelDamageTracking ->
                    handleCancelDamageTracking(cancelDamageTracking);
            default -> {
            }
        }
    }

    private void handleTeleportSpawn(PluginMessage.TeleportSpawn message) {
        Player player = Bukkit.getPlayer(message.playerUuid());
        if (player != null) {
            Location respawn = message.useBedSpawn() ? player.getRespawnLocation() : null;
            if (respawn == null) {
                if (message.overrideWorld() != null && !message.overrideWorld().isEmpty()) {
                    World world = Bukkit.getWorld(message.overrideWorld());
                    if (world != null) {
                        respawn = world.getSpawnLocation();
                    }
                } else if (!plugin.getDefaultWorld().isEmpty()) {
                    World world = Bukkit.getWorld(plugin.getDefaultWorld());
                    if (world != null) {
                        respawn = world.getSpawnLocation();
                    }
                }
            }
            if (respawn == null) {
                respawn = player.getWorld().getSpawnLocation();
            }
            player.teleport(respawn);
        }
    }

    private void handleStartDamageTracking(PluginMessage.StartDamageTracking message) {
        Player player = Bukkit.getPlayer(message.playerUuid());
        if (player != null) {
            plugin.getDamageTracker().startTracking(player, message.delaySeconds());
        }
    }

    private void handleCancelDamageTracking(PluginMessage.CancelDamageTracking message) {
        plugin.getDamageTracker().cancelTracking(message.playerUuid());
    }

    public static void sendMessage(PaperPlugin plugin, Player player, PluginMessage message) {
        player.sendPluginMessage(plugin, MessageConstants.CHANNEL, PluginMessageCodec.serialize(message));
    }
}
