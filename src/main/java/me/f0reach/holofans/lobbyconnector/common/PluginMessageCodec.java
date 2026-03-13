package me.f0reach.holofans.lobbyconnector.common;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

public final class PluginMessageCodec {
    private PluginMessageCodec() {}

    public static byte[] serialize(PluginMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message.messageId());

        switch (message) {
            case PluginMessage.TeleportSpawn teleportSpawn -> {
                writeUuid(out, teleportSpawn.playerUuid());
                out.writeBoolean(teleportSpawn.useBedSpawn());
            }
            case PluginMessage.StartDamageTracking startDamageTracking -> {
                writeUuid(out, startDamageTracking.playerUuid());
                out.writeInt(startDamageTracking.delaySeconds());
            }
            case PluginMessage.CancelDamageTracking cancelDamageTracking ->
                    writeUuid(out, cancelDamageTracking.playerUuid());
            case PluginMessage.DamageClear damageClear -> writeUuid(out, damageClear.playerUuid());
            case PluginMessage.LobbyDeath lobbyDeath -> writeUuid(out, lobbyDeath.playerUuid());
        }

        return out.toByteArray();
    }

    public static PluginMessage deserialize(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String messageId = in.readUTF();

        return switch (messageId) {
            case MessageConstants.TELEPORT_SPAWN ->
                    new PluginMessage.TeleportSpawn(readUuid(in), in.readBoolean());
            case MessageConstants.START_DAMAGE_TRACKING ->
                    new PluginMessage.StartDamageTracking(readUuid(in), in.readInt());
            case MessageConstants.CANCEL_DAMAGE_TRACKING ->
                    new PluginMessage.CancelDamageTracking(readUuid(in));
            case MessageConstants.DAMAGE_CLEAR -> new PluginMessage.DamageClear(readUuid(in));
            case MessageConstants.LOBBY_DEATH -> new PluginMessage.LobbyDeath(readUuid(in));
            default -> throw new IllegalArgumentException("Unknown plugin message id: " + messageId);
        };
    }

    private static void writeUuid(ByteArrayDataOutput out, UUID uuid) {
        out.writeUTF(uuid.toString());
    }

    private static UUID readUuid(ByteArrayDataInput in) {
        return UUID.fromString(in.readUTF());
    }
}
