package dev.huntertagog.coresystem.fabric.server.world.island;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public enum IslandStyle {

    MEADOW(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    0.00f, // coarse dirt
                    0.00f, // podzol
                    0.02f  // stone patch
            ),
            new FloraProfile(
                    0.55f, // grass density
                    0.25f, // flower density
                    0.05f, // bush density
                    0.15f  // empty
            ),
            new TreeProfile(
                    TreeProfile.TreeType.OAK,
                    4,     // min trees
                    8      // max trees
            ),
            0.5f // beachCoverageFactor
    ),

    FOREST(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    0.10f, // coarse dirt
                    0.15f, // podzol
                    0.03f  // stone patch
            ),
            new FloraProfile(
                    0.35f, // grass
                    0.15f, // flowers
                    0.20f, // bushes
                    0.30f  // empty
            ),
            new TreeProfile(
                    TreeProfile.TreeType.MIXED_OAK_BIRCH,
                    8,
                    16
            ),
            0.4f
    ),

    ROCKY(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.STONE.getDefaultState(),
                    Blocks.STONE.getDefaultState(),
                    0.05f, // coarse dirt
                    0.00f, // podzol
                    0.35f  // stone patch
            ),
            new FloraProfile(
                    0.20f,
                    0.05f,
                    0.15f,
                    0.60f
            ),
            new TreeProfile(
                    TreeProfile.TreeType.SPARSE_SPRUCE,
                    2,
                    6
            ),
            0.6f
    ),

    FLOWER_FIELD(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    0.00f,
                    0.00f,
                    0.01f
            ),
            new FloraProfile(
                    0.30f,
                    0.50f,
                    0.05f,
                    0.15f
            ),
            new TreeProfile(
                    TreeProfile.TreeType.SPARSE_OAK,
                    3,
                    6
            ),
            0.3f
    ),

    TROPICAL(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.SAND.getDefaultState(),
                    Blocks.SAND.getDefaultState(),
                    0.00f,
                    0.00f,
                    0.05f
            ),
            new FloraProfile(
                    0.40f,
                    0.20f,
                    0.20f,
                    0.20f
            ),
            new TreeProfile(
                    TreeProfile.TreeType.PALM_LIKE_OAK, // wir nutzen intern modifizierte OAK-Strukturen
                    5,
                    10
            ),
            0.8f
    ),

    MIXED(
            new GroundProfile(
                    Blocks.GRASS_BLOCK.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    Blocks.DIRT.getDefaultState(),
                    0.08f,
                    0.05f,
                    0.08f
            ),
            new FloraProfile(
                    0.40f,
                    0.25f,
                    0.10f,
                    0.25f
            ),
            new TreeProfile(
                    TreeProfile.TreeType.MIXED_OAK_BIRCH,
                    4,
                    12
            ),
            0.5f
    );

    private final GroundProfile groundProfile;
    private final FloraProfile floraProfile;
    private final TreeProfile treeProfile;
    private final float beachCoverageFactor;

    IslandStyle(GroundProfile groundProfile,
                FloraProfile floraProfile,
                TreeProfile treeProfile,
                float beachCoverageFactor) {
        this.groundProfile = groundProfile;
        this.floraProfile = floraProfile;
        this.treeProfile = treeProfile;
        this.beachCoverageFactor = beachCoverageFactor;
    }

    public GroundProfile groundProfile() {
        return groundProfile;
    }

    public FloraProfile floraProfile() {
        return floraProfile;
    }

    public TreeProfile treeProfile() {
        return treeProfile;
    }

    public float beachCoverageFactor() {
        return beachCoverageFactor;
    }

    // ================== Helper-Records ==================

    public record GroundProfile(
            BlockState surface,
            BlockState subsurface,
            BlockState deep,
            float coarseDirtChance,
            float podzolChance,
            float stonePatchChance
    ) {
    }

    public record FloraProfile(
            float grassChance,
            float flowerChance,
            float bushChance,
            float emptyChance
    ) {
        public float total() {
            return grassChance + flowerChance + bushChance + emptyChance;
        }
    }

    public record TreeProfile(
            TreeType type,
            int minTrees,
            int maxTrees
    ) {
        public enum TreeType {
            OAK,
            SPARSE_OAK,
            MIXED_OAK_BIRCH,
            SPARSE_SPRUCE,
            PALM_LIKE_OAK
        }
    }
}

