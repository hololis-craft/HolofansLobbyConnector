package me.f0reach.holofans.lobbyconnector.paper;

import me.f0reach.holofans.lobbyconnector.common.MessageConstants;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class PaperPlugin extends JavaPlugin {
    private DamageTracker damageTracker;

    @Override
    public void onEnable() {
        damageTracker = new DamageTracker(this);

        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, MessageConstants.CHANNEL);

        PaperMessageHandler messageHandler = new PaperMessageHandler(this);
        messenger.registerIncomingPluginChannel(this, MessageConstants.CHANNEL, messageHandler);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(damageTracker, this);

        getLogger().info("HolofansLobbyConnector (Paper) enabled");
    }

    @Override
    public void onDisable() {
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(this);
        messenger.unregisterIncomingPluginChannel(this);

        if (damageTracker != null) {
            damageTracker.cancelAll();
        }
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
}
