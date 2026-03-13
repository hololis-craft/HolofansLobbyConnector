package me.f0reach.holofans.lobbyconnector.paper;

import me.f0reach.holofans.lobbyconnector.common.PluginMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathListener implements Listener {
    private final PaperPlugin plugin;
    private final Set<UUID> deadPlayers = new HashSet<>();

    public DeathListener(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        deadPlayers.add(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!deadPlayers.remove(uuid)) return;

        // Send LOBBY_DEATH message to Velocity on next tick (after respawn completes)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            var player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PaperMessageHandler.sendMessage(plugin, player, new PluginMessage.LobbyDeath(uuid));
            }
        }, 1L);
    }
}
