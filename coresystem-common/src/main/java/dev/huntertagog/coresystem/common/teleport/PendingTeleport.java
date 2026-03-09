package dev.huntertagog.coresystem.common.teleport;

import java.util.UUID;

public final class PendingTeleport {

    private final UUID playerUuid;
    private final String targetServer;      // z. B. "islands-1"
    private final String inventoryContext;  // z. B. "network-global" oder "pre-minigame"
    private final long createdAt;           // epoch millis
    private final String reason;  // optional: "portal-islands", "hub-command"

    public PendingTeleport(UUID playerUuid,
                           String targetServer,
                           String inventoryContext,
                           long createdAt,
                           String reason) {
        this.playerUuid = playerUuid;
        this.targetServer = targetServer;
        this.inventoryContext = inventoryContext;
        this.createdAt = createdAt;
        this.reason = reason;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String targetServer() {
        return targetServer;
    }

    public String inventoryContext() {
        return inventoryContext;
    }

    public long createdAt() {
        return createdAt;
    }

    public String reason() {
        return reason;
    }
}
