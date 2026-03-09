package dev.huntertagog.coresystem.platform.player;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uniqueId;
    private final String name;
    private final long firstSeenAt;   // epoch millis
    private final long lastSeenAt;    // epoch millis
    private final int totalJoins;
    private final String lastServer;  // z.B. "lobby", "islands-1"
    private final String lastNodeId;

    public PlayerProfile(
            UUID uniqueId,
            String name,
            long firstSeenAt,
            long lastSeenAt,
            int totalJoins,
            String lastServer,
            String lastNodeId
    ) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.totalJoins = totalJoins;
        this.lastServer = lastServer;
        this.lastNodeId = lastNodeId;
    }

    // ---------- Factory ----------

    public static PlayerProfile newEphemeral(UUID uniqueId, String safeName, long now) {
        return new PlayerProfile(
                uniqueId,
                safeName,
                now,
                now,
                0,
                "unknown",
                "unknown"
        );
    }

    // ---------- Immutable Updates ----------

    public PlayerProfile withUpdatedOnJoin(
            String newName,
            String serverName,
            String nodeId,
            long nowMillis
    ) {
        int updatedJoins = totalJoins <= 0 ? 1 : totalJoins + 1;
        long firstSeen = firstSeenAt > 0 ? firstSeenAt : nowMillis;

        return new PlayerProfile(
                uniqueId,
                newName,
                firstSeen,
                nowMillis,
                updatedJoins,
                serverName,
                nodeId
        );
    }

    public PlayerProfile withUpdatedLastSeen(
            long nowMillis,
            String serverName,
            String nodeId
    ) {
        return new PlayerProfile(
                uniqueId,
                name,
                firstSeenAt > 0 ? firstSeenAt : nowMillis,
                nowMillis,
                totalJoins,
                serverName,
                nodeId
        );
    }

    // ---------- Getters ----------

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public long getFirstSeenAt() {
        return firstSeenAt;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public int getTotalJoins() {
        return totalJoins;
    }

    public String getLastServer() {
        return lastServer;
    }

    public String getLastNodeId() {
        return lastNodeId;
    }
}
