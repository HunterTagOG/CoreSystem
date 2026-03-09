package dev.huntertagog.coresystem.platform.player.playerdata;

import java.util.Objects;
import java.util.UUID;

public final class PlayerInventorySnapshot {

    private final UUID playerUuid;
    private final String context;        // z. B. "lobby", "islands", "minigame:bedwars"
    private final long createdAt;        // epoch millis
    private final String payloadBase64;  // komprimiertes NBT als Base64
    private final String version; // optional für spätere Migrationen

    public PlayerInventorySnapshot(UUID playerUuid,
                                   String context,
                                   long createdAt,
                                   String payloadBase64,
                                   String version) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.context = Objects.requireNonNull(context, "context");
        this.createdAt = createdAt;
        this.payloadBase64 = Objects.requireNonNull(payloadBase64, "payloadBase64");
        this.version = version;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String context() {
        return context;
    }

    public long createdAt() {
        return createdAt;
    }

    public String payloadBase64() {
        return payloadBase64;
    }

    public String version() {
        return version;
    }
}
