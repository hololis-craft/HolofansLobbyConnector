package me.f0reach.holofans.lobbyconnector.paper;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PaperMessageHandler implements PluginMessageListener {
    private final PaperPlugin plugin;

    public PaperMessageHandler(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(MessageConstants.CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String messageId = in.readUTF();

        switch (messageId) {
            case MessageConstants.TELEPORT_SPAWN -> handleTeleportSpawn(in);
            case MessageConstants.START_DAMAGE_TRACKING -> handleStartDamageTracking(in);
            case MessageConstants.CANCEL_DAMAGE_TRACKING -> handleCancelDamageTracking(in);
        }
    }

    private void handleTeleportSpawn(ByteArrayDataInput in) {
        UUID playerUUID = UUID.fromString(in.readUTF());
        boolean useBedSpawn = in.readBoolean();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Location respawn = useBedSpawn ? player.getRespawnLocation() : null;
            player.teleport(respawn != null ? respawn : player.getWorld().getSpawnLocation());
        }
    }

    private void handleStartDamageTracking(ByteArrayDataInput in) {
        UUID playerUUID = UUID.fromString(in.readUTF());
        int delaySeconds = in.readInt();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            plugin.getDamageTracker().startTracking(player, delaySeconds);
        }
    }

    private void handleCancelDamageTracking(ByteArrayDataInput in) {
        UUID playerUUID = UUID.fromString(in.readUTF());
        plugin.getDamageTracker().cancelTracking(playerUUID);
    }

    public static void sendMessage(PaperPlugin plugin, Player player, String messageId, UUID playerUUID) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(messageId);
        out.writeUTF(playerUUID.toString());
        player.sendPluginMessage(plugin, MessageConstants.CHANNEL, out.toByteArray());
    }
}
