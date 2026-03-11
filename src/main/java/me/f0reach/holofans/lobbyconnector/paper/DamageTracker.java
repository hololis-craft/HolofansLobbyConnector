package me.f0reach.holofans.lobbyconnector.paper;

import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DamageTracker implements Listener {
    private final PaperPlugin plugin;
    private final Map<UUID, TrackingData> tracking = new ConcurrentHashMap<>();

    private static class TrackingData {
        final int delaySeconds;
        BukkitTask task;

        TrackingData(int delaySeconds) {
            this.delaySeconds = delaySeconds;
        }
    }

    public DamageTracker(PaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTracking(Player player, int delaySeconds) {
        UUID uuid = player.getUniqueId();

        // Cancel existing tracking if any
        cancelTracking(uuid);

        TrackingData data = new TrackingData(delaySeconds);
        tracking.put(uuid, data);

        scheduleCompletion(uuid, data);
    }

    public void cancelTracking(UUID uuid) {
        TrackingData existing = tracking.remove(uuid);
        if (existing != null && existing.task != null) {
            existing.task.cancel();
        }
    }

    public void cancelAll() {
        for (Map.Entry<UUID, TrackingData> entry : tracking.entrySet()) {
            if (entry.getValue().task != null) {
                entry.getValue().task.cancel();
            }
        }
        tracking.clear();
    }

    private void scheduleCompletion(UUID uuid, TrackingData data) {
        data.task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tracking.remove(uuid);
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PaperMessageHandler.sendMessage(plugin, player, MessageConstants.DAMAGE_CLEAR, uuid);
            }
        }, data.delaySeconds * 20L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        TrackingData data = tracking.get(uuid);
        if (data == null) return;

        // Cancel current timer and restart
        if (data.task != null) {
            data.task.cancel();
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>ダメージを受けたためカウンターがリセットされました。"));

        scheduleCompletion(uuid, data);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelTracking(event.getPlayer().getUniqueId());
    }
}
