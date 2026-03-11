package me.f0reach.holofans.lobbyconnector.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import net.kyori.adventure.text.minimessage.MiniMessage;

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

        String currentServer = currentServerOpt.get().getServerInfo().getName();

        if (currentServer.equalsIgnoreCase(plugin.getConfig().getLobbyServer())) {
            // Already in lobby - teleport to spawn
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(MessageConstants.TELEPORT_SPAWN);
            out.writeUTF(player.getUniqueId().toString());
            currentServerOpt.get().sendPluginMessage(VelocityPlugin.CHANNEL, out.toByteArray());
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>スポーン地点に移動しました。"));
            return;
        }

        // Not in lobby - check server config
        VelocityConfig.ServerConfig serverConfig = plugin.getConfig().getServerConfig(currentServer);

        if (serverConfig.isDelayed()) {
            // Cancel existing tracking if any, then start new
            plugin.getPendingLobbyTransfer().put(player.getUniqueId(), true);

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(MessageConstants.START_DAMAGE_TRACKING);
            out.writeUTF(player.getUniqueId().toString());
            out.writeInt(serverConfig.getDelaySeconds());
            currentServerOpt.get().sendPluginMessage(VelocityPlugin.CHANNEL, out.toByteArray());

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>ロビーに移動します。<white>" + serverConfig.getDelaySeconds()
                            + "<yellow>秒間ダメージを受けないでください。"));
        } else {
            // Immediate transfer
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>ロビーに移動します..."));
            plugin.transferToLobby(player);
        }
    }
}
