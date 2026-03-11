package me.f0reach.holofans.lobbyconnector.common;

public final class MessageConstants {
    public static final String CHANNEL = "holofans:lobbyconnector";

    // Velocity → Paper
    public static final String TELEPORT_SPAWN = "TELEPORT_SPAWN";
    public static final String START_DAMAGE_TRACKING = "START_DAMAGE_TRACKING";
    public static final String CANCEL_DAMAGE_TRACKING = "CANCEL_DAMAGE_TRACKING";
    public static final String TELEPORT_BED_SPAWN = "TELEPORT_BED_SPAWN";

    // Paper → Velocity
    public static final String DAMAGE_CLEAR = "DAMAGE_CLEAR";
    public static final String LOBBY_DEATH = "LOBBY_DEATH";

    private MessageConstants() {}
}
