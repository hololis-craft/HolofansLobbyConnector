package me.f0reach.holofans.lobbyconnector.common;

import java.util.UUID;

public sealed interface PluginMessage permits PluginMessage.TeleportSpawn,
        PluginMessage.StartDamageTracking,
        PluginMessage.CancelDamageTracking,
        PluginMessage.DamageClear,
        PluginMessage.OnDeath {

    String messageId();

    record TeleportSpawn(UUID playerUuid, boolean useBedSpawn) implements PluginMessage {
        @Override
        public String messageId() {
            return MessageConstants.TELEPORT_SPAWN;
        }
    }

    record StartDamageTracking(UUID playerUuid, int delaySeconds) implements PluginMessage {
        @Override
        public String messageId() {
            return MessageConstants.START_DAMAGE_TRACKING;
        }
    }

    record CancelDamageTracking(UUID playerUuid) implements PluginMessage {
        @Override
        public String messageId() {
            return MessageConstants.CANCEL_DAMAGE_TRACKING;
        }
    }

    record DamageClear(UUID playerUuid) implements PluginMessage {
        @Override
        public String messageId() {
            return MessageConstants.DAMAGE_CLEAR;
        }
    }

    record OnDeath(UUID playerUuid) implements PluginMessage {
        @Override
        public String messageId() {
            return MessageConstants.ON_DEATH;
        }
    }
}
