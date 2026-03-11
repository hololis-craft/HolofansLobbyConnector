package me.f0reach.holofans.lobbyconnector.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;

public class VelocityMessageHandler {
    private final VelocityPlugin plugin;

    public VelocityMessageHandler(VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityPlugin.CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String messageId = in.readUTF();

        switch (messageId) {
            case MessageConstants.DAMAGE_CLEAR -> handleDamageClear(in);
            case MessageConstants.LOBBY_DEATH -> handleLobbyDeath(in);
        }
    }

    private void handleDamageClear(ByteArrayDataInput in) {
        UUID playerUUID = UUID.fromString(in.readUTF());

        // Only transfer if the player still has a pending lobby transfer
        if (plugin.getPendingLobbyTransfer().remove(playerUUID) == null) return;

        plugin.getServer().getPlayer(playerUUID).ifPresent(player -> {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>ロビーに移動します..."));
            plugin.transferToLobby(player);
        });
    }

    private void handleLobbyDeath(ByteArrayDataInput in) {
        UUID playerUUID = UUID.fromString(in.readUTF());

        plugin.getServer().getPlayer(playerUUID).ifPresent(player -> {
            String lastServer = plugin.getPlayerDataManager().getLastServer(playerUUID);
            if (lastServer == null) {
                lastServer = plugin.getConfig().getDefaultSurvivalServer();
            }

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>直前のサーバに移動します..."));
            plugin.transferToServer(player, lastServer);
        });
    }
}
