package me.f0reach.holofans.lobbyconnector.velocity;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class DefaultSurvivalCommand {
    private DefaultSurvivalCommand() {}

    public static BrigadierCommand create(VelocityPlugin plugin) {
        var node = BrigadierCommand.literalArgumentBuilder("defaultsurvival")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource();
                    boolean enabled = plugin.getPlayerDataManager()
                            .toggleDefaultSurvival(player.getUniqueId());

                    if (enabled) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<green>デフォルトサバイバルを有効にしました。次回接続時からサバイバルサーバに直接接続します。"));
                    } else {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<yellow>デフォルトサバイバルを無効にしました。次回接続時からロビーに接続します。"));
                    }
                    return 1;
                })
                .build();
        return new BrigadierCommand(node);
    }
}
