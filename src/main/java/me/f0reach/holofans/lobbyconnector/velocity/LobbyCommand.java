package me.f0reach.holofans.lobbyconnector.velocity;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import me.f0reach.holofans.lobbyconnector.common.PluginMessage;
import me.f0reach.holofans.lobbyconnector.common.PluginMessageCodec;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Optional;

public final class LobbyCommand {
    private LobbyCommand() {}

    public static BrigadierCommand create(VelocityPlugin plugin) {
        var node = BrigadierCommand.literalArgumentBuilder("lobby")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource();
                    execute(plugin, player);
                    return 1;
                })
                .build();
        return new BrigadierCommand(node);
    }

    private static void execute(VelocityPlugin plugin, Player player) {
        Optional<ServerConnection> currentServerOpt = player.getCurrentServer();
        if (currentServerOpt.isEmpty()) return;

        VelocityConfig config = plugin.getConfig();
        String currentServer = currentServerOpt.get().getServerInfo().getName();

        if (currentServer.equalsIgnoreCase(config.getLobbyServer())) {
            // Already in lobby - teleport to spawn
            currentServerOpt.get().sendPluginMessage(
                    VelocityPlugin.CHANNEL,
                    PluginMessageCodec.serialize(new PluginMessage.TeleportSpawn(player.getUniqueId(), false))
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    config.getMessage("lobby-teleport-spawn")));
            return;
        }

        // Not in lobby - check server config
        VelocityConfig.ServerConfig serverConfig = config.getServerConfig(currentServer);

        if (serverConfig.isDelayed()) {
            // Cancel existing tracking if any, then start new
            plugin.getPendingLobbyTransfer().put(player.getUniqueId(), true);

            currentServerOpt.get().sendPluginMessage(
                    VelocityPlugin.CHANNEL,
                    PluginMessageCodec.serialize(new PluginMessage.StartDamageTracking(
                            player.getUniqueId(),
                            serverConfig.getDelaySeconds()))
            );

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    config.getMessage("lobby-transfer-delayed"),
                    Placeholder.unparsed("seconds", String.valueOf(serverConfig.getDelaySeconds()))));
        } else {
            // Immediate transfer
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    config.getMessage("lobby-transfer-immediate")));
            plugin.transferToLobby(player);
        }
    }
}
