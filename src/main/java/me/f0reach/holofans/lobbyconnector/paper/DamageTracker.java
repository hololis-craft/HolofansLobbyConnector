package me.f0reach.holofans.lobbyconnector.paper;

import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
        final int delayTicks;
        int remainingTicks;
        BukkitTask task;
        BossBar bossBar;

        TrackingData(int delaySeconds) {
            this.delayTicks = delaySeconds * 20;
            this.remainingTicks = this.delayTicks;
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
        data.bossBar = BossBar.bossBar(
                buildBossBarName(data),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(data.bossBar);
        tracking.put(uuid, data);

        startTimer(uuid, data);
    }

    public void cancelTracking(UUID uuid) {
        TrackingData existing = tracking.remove(uuid);
        if (existing != null) {
            if (existing.task != null) existing.task.cancel();
            hideBossBar(uuid, existing);
        }
    }

    public void cancelAll() {
        for (Map.Entry<UUID, TrackingData> entry : tracking.entrySet()) {
            if (entry.getValue().task != null) entry.getValue().task.cancel();
            hideBossBar(entry.getKey(), entry.getValue());
        }
        tracking.clear();
    }

    private void hideBossBar(UUID uuid, TrackingData data) {
        if (data.bossBar == null) return;
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            player.hideBossBar(data.bossBar);
        }
    }

    private void startTimer(UUID uuid, TrackingData data) {
        data.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            data.remainingTicks--;

            if (data.remainingTicks <= 0) {
                // Completed
                data.task.cancel();
                tracking.remove(uuid);
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.hideBossBar(data.bossBar);
                    PaperMessageHandler.sendMessage(plugin, player, MessageConstants.DAMAGE_CLEAR, uuid);
                }
                return;
            }

            // Update boss bar
            float progress = (float) data.remainingTicks / data.delayTicks;
            data.bossBar.progress(Math.max(0f, Math.min(1f, progress)));
            data.bossBar.name(buildBossBarName(data));

            // Change color when running low
            if (progress <= 0.3f) {
                data.bossBar.color(BossBar.Color.RED);
            } else if (progress <= 0.6f) {
                data.bossBar.color(BossBar.Color.YELLOW);
            }
        }, 1L, 1L);
    }

    private Component buildBossBarName(TrackingData data) {
        int seconds = (int) Math.ceil(data.remainingTicks / 20.0);
        return MiniMessage.miniMessage().deserialize(
                plugin.getMessage("bossbar-countdown"),
                Placeholder.unparsed("seconds", String.valueOf(seconds)));
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

        // Reset remaining ticks and boss bar
        data.remainingTicks = data.delayTicks;
        data.bossBar.progress(1.0f);
        data.bossBar.color(BossBar.Color.GREEN);
        data.bossBar.name(buildBossBarName(data));

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                plugin.getMessage("damage-reset")));

        startTimer(uuid, data);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelTracking(event.getPlayer().getUniqueId());
    }
}
