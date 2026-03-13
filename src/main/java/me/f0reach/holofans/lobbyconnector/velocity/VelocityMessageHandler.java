package me.f0reach.holofans.lobbyconnector.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import me.f0reach.holofans.lobbyconnector.common.PluginMessage;
import me.f0reach.holofans.lobbyconnector.common.PluginMessageCodec;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class VelocityMessageHandler {
    private final VelocityPlugin plugin;

    public VelocityMessageHandler(VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityPlugin.CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection serverConnection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        PluginMessage message = PluginMessageCodec.deserialize(event.getData());

        switch (message) {
            case PluginMessage.DamageClear damageClear -> handleDamageClear(damageClear);
            case PluginMessage.LobbyDeath lobbyDeath -> handleLobbyDeath(lobbyDeath, serverConnection);
            default -> {
            }
        }
    }

    private void handleDamageClear(PluginMessage.DamageClear message) {
        var playerUUID = message.playerUuid();

        // Only transfer if the player still has a pending lobby transfer
        if (plugin.getPendingLobbyTransfer().remove(playerUUID) == null) return;

        plugin.getServer().getPlayer(playerUUID).ifPresent(player -> {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getMessage("lobby-transfer-damage-clear")));
            plugin.transferToLobby(player);
        });
    }

    private void handleLobbyDeath(PluginMessage.LobbyDeath message, ServerConnection serverConnection) {
        var playerUUID = message.playerUuid();

        if (!serverConnection.getServerInfo().getName()
                .equalsIgnoreCase(plugin.getConfig().getLobbyServer())) {
            return;
        }

        plugin.getServer().getPlayer(playerUUID).ifPresent(player -> {
            String lastServer = plugin.getPlayerDataManager().getLastServer(playerUUID);
            if (lastServer == null) {
                lastServer = plugin.getConfig().getDefaultSurvivalServer();
            }

            plugin.getPendingBedSpawn().add(playerUUID);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getMessage("lobby-death-transfer")));
            plugin.transferToServer(player, lastServer);
        });
    }
}
