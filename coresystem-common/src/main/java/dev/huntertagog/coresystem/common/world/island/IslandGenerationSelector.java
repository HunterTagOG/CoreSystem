package dev.huntertagog.coresystem.common.world.island;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;

import java.util.Random;
import java.util.UUID;

public final class IslandGenerationSelector {

    private static final Logger LOG = LoggerFactory.get("IslandGenerationSelector");

    private IslandGenerationSelector() {
    }

    public enum GenerationPreset {
        REALISTIC_SINGLE,
        REALISTIC_MULTI_PEAKS,
        CLUSTER,
        CLUSTER_LAGOONS
    }

    /**
     * Wählt deterministisch ein Preset basierend auf Weltseed + Owner-ID.
     * Verteilung:
     * 35% Realistic Single, 25% Multi-Peaks, 25% Cluster, 15% Cluster-Lagoons
     */
    public static GenerationPreset selectPreset(long worldSeed, UUID ownerId) {
        if (ownerId == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,      // ggf. später auf einen spezifischeren Code ändern
                            CoreErrorSeverity.WARN,
                            "IslandGenerationSelector.selectPreset called with null ownerId"
                    )
                    .withContextEntry("worldSeed", worldSeed);

            error.log();
            LOG.warn("selectPreset() received null ownerId – falling back to REALISTIC_SINGLE.");
            return GenerationPreset.REALISTIC_SINGLE;
        }

        long hash =
                worldSeed
                        ^ ownerId.getMostSignificantBits()
                        ^ (ownerId.getLeastSignificantBits() << 1)
                        ^ 0x9E3779B97F4A7C15L;

        Random r = new Random(hash);
        double v = r.nextDouble();

        if (v < 0.35) return GenerationPreset.REALISTIC_SINGLE;
        if (v < 0.60) return GenerationPreset.REALISTIC_MULTI_PEAKS;
        if (v < 0.85) return GenerationPreset.CLUSTER;
        return GenerationPreset.CLUSTER_LAGOONS;
    }
}
