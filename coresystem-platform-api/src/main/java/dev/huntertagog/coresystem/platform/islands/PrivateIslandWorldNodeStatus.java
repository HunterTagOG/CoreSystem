package dev.huntertagog.coresystem.platform.islands;

public record PrivateIslandWorldNodeStatus(
        String nodeId,         // z.B. "island-1"
        String serverName,     // Velocity/FabricProxy-Lite Server-Name
        boolean islandServer,  // CORESYSTEM_ISLAND_SERVER=true?
        int currentIslands,
        int maxIslands,
        int currentPlayers,
        int maxPlayers,
        long lastHeartbeatMillis
) {
    public double islandLoadFactor() {
        return maxIslands > 0 ? (double) currentIslands / (double) maxIslands : 1.0;
    }

    public double playerLoadFactor() {
        return maxPlayers > 0 ? (double) currentPlayers / (double) maxPlayers : 1.0;
    }

    /**
     * Je kleiner, desto „besser“ / leerer.
     */
    public double combinedScore() {
        return islandLoadFactor() * 0.7 + playerLoadFactor() * 0.3;
    }

    public boolean hasIslandCapacity() {
        return currentIslands < maxIslands;
    }

    public boolean hasPlayerCapacity() {
        return currentPlayers < maxPlayers;
    }
}