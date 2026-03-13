package me.f0reach.holofans.lobbyconnector.velocity;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.proxy.Player;
import me.f0reach.holofans.lobbyconnector.common.PermissionConstants;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class DefaultSurvivalCommand {
    private DefaultSurvivalCommand() {}

    public static BrigadierCommand create(VelocityPlugin plugin) {
        var node = BrigadierCommand.literalArgumentBuilder("defaultsurvival")
                .requires(source -> source instanceof Player player
                        && player.hasPermission(PermissionConstants.DEFAULT_SURVIVAL_USE))
                .executes(context -> {
                    Player player = (Player) context.getSource();
                    boolean enabled = plugin.getPlayerDataManager()
                            .toggleDefaultSurvival(player.getUniqueId());

                    String key = enabled ? "default-survival-enabled" : "default-survival-disabled";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            plugin.getConfig().getMessage(key)));
                    return 1;
                })
                .build();
        return new BrigadierCommand(node);
    }
}
