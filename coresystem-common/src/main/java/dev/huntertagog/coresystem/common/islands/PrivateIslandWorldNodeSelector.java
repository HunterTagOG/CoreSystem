package dev.huntertagog.coresystem.common.islands;

import dev.huntertagog.coresystem.platform.islands.PrivateIslandWorldNodeStatus;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class PrivateIslandWorldNodeSelector {

    private PrivateIslandWorldNodeSelector() {
    }

    /**
     * Default: stale after 3x heartbeat interval
     */
    public static Optional<PrivateIslandWorldNodeStatus> selectBestIslandNode(
            Collection<PrivateIslandWorldNodeStatus> nodes,
            long heartbeatIntervalMs,
            UUID ownerId
    ) {
        long now = System.currentTimeMillis();
        long maxAgeMs = Math.max(heartbeatIntervalMs * 3, 5_000);

        // deterministischer "Noise" pro Owner -> verteilt Last, bleibt aber stabil
        int ownerSalt = ownerId != null ? ownerId.hashCode() : 0;

        return nodes.stream()
                .filter(PrivateIslandWorldNodeStatus::islandServer)
                .filter(n -> now - n.lastHeartbeatMillis() <= maxAgeMs)
                .filter(PrivateIslandWorldNodeStatus::hasIslandCapacity)
                .filter(PrivateIslandWorldNodeStatus::hasPlayerCapacity)
                .min(Comparator
                        // 1) Hauptscore (dein Score)
                        .comparingDouble(PrivateIslandWorldNodeStatus::combinedScore)
                        // 2) Bei Gleichstand: mehr freie Island-Slots bevorzugen
                        .thenComparingInt(PrivateIslandWorldNodeSelector::freeIslandSlots).reversed()
                        // 3) Bei weiterem Gleichstand: deterministischer Spread pro Owner
                        .thenComparingInt(n -> stableOwnerSpread(n.nodeId(), ownerSalt))
                );
    }

    private static int freeIslandSlots(PrivateIslandWorldNodeStatus n) {
        return Math.max(0, n.maxIslands() - n.currentIslands());
    }

    private static int stableOwnerSpread(String nodeId, int ownerSalt) {
        // kleiner stabiler “Jitter”: sorgt für gleichmäßigere Verteilung ohne Random() / Threads
        return (nodeId != null ? nodeId.hashCode() : 0) ^ ownerSalt;
    }
}
