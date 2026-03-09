package dev.huntertagog.coresystem.common.world.structures.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Abbild der JSON-Konfiguration:
 * <p>
 * {
 * "groups": [
 * {
 * "id": "starter_cluster",
 * "min_distance": 512,
 * "world_radius": 3000,
 * "structures": [
 * {
 * "id": "coresystem:starter_island",
 * "count": 1,
 * "biomes": ["minecraft:plains", "minecraft:forest"],
 * "y_offset": 0
 * }
 * ]
 * }
 * ]
 * }
 * <p>
 * y_offset-Semantik:
 * placementY = getTopY(x,z) + y_offset
 */
public class StructureSpawnConfig {

    public List<Group> groups = new ArrayList<>();

    public static class Group {
        public String id = "default";

        /**
         * Mindestabstand zwischen allen Ziel-Positionen dieser Gruppe in Blöcken.
         */
        public int min_distance = 512;

        /**
         * Radius um 0/0, in dem Zielpositionen gesucht werden (6000x6000 => 3000).
         */
        public int world_radius = 3000;

        public List<StructureEntry> structures = new ArrayList<>();
    }

    public static class StructureEntry {
        /**
         * Struktur-ID (Namespace:Name), z.B. "coresystem:starter_island".
         */
        public String id;

        /**
         * Anzahl Instanzen dieser Struktur pro Welt.
         */
        public int count = 1;

        /**
         * Whitelist von Biomen, leere Liste/null => alle Biome erlaubt.
         */
        public List<String> biomes = new ArrayList<>();

        /**
         * Y-Offset relativ zur Terrainhöhe (getTopY).
         * Beispiel:
         * Boden der Struktur liegt 21 Blöcke über minY und soll auf dem Terrain sitzen
         * => y_offset = -21
         */
        public int y_offset = 0;
    }
}

