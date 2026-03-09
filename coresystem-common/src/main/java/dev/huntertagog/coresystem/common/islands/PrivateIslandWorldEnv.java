package dev.huntertagog.coresystem.common.islands;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;

public final class PrivateIslandWorldEnv {

    private static final Logger LOG = LoggerFactory.get("Coresystem");

    /**
     * Radius der eigentlichen Insel (Land), in Chunks.
     * Wird u. a. in IslandHeightmap genutzt.
     */
    public static final int ISLAND_RADIUS;

    /**
     * Radius des Ozean-/Pufferbereichs um die Insel, in Chunks.
     * Wird z. B. für Seafloor/Shelves genutzt.
     */
    public static final int OCEAN_RADIUS;

    /**
     * Effektiver Border-/Chunky-Radius in Chunks.
     * → WorldBorder.setSize(BORDER_RADIUS * 2)
     * → Chunky radius = BORDER_RADIUS
     */
    public static final int BORDER_RADIUS;

    static {
        // sinnvolle Defaults für Dev / Fallback
        int defaultIsland = 4;    // 4 Chunks
        int defaultOcean = 8;     // 8 Chunks
        int defaultBorder = 12;   // 12 Chunks

        ISLAND_RADIUS = readIntEnv("ISLAND_RADIUS", defaultIsland);
        OCEAN_RADIUS = readIntEnv("OCEAN_RADIUS", defaultOcean);
        BORDER_RADIUS = readIntEnv("BORDER_RADIUS", defaultBorder);

        LOG.info(
                "PrivateIslandWorldEnv initialized: ISLAND_RADIUS={} OCEAN_RADIUS={} BORDER_RADIUS={}",
                ISLAND_RADIUS, OCEAN_RADIUS, BORDER_RADIUS
        );
    }

    private static int readIntEnv(String name, int defaultValue) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                LOG.warn("Env {} has non-positive value '{}', falling back to default {}", name, raw, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ex) {
            LOG.warn("Env {}='{}' is not a valid integer, falling back to default {}", name, raw, defaultValue);
            return defaultValue;
        }
    }

    private PrivateIslandWorldEnv() {
        // no instances
    }
}
