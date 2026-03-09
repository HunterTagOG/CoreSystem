package dev.huntertagog.coresystem.fabric.server.world.island;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class IslandDecorator {

    private static final Logger LOG = LoggerFactory.get("IslandDecorator");

    // Basisblöcke
    private static final BlockState GRASS_BLOCK = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final BlockState SAND = Blocks.SAND.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();

    // Flora
    private static final BlockState TALL_GRASS = Blocks.TALL_GRASS.getDefaultState();
    private static final BlockState FERN = Blocks.FERN.getDefaultState();
    private static final BlockState FLOWER_1 = Blocks.DANDELION.getDefaultState();
    private static final BlockState FLOWER_2 = Blocks.POPPY.getDefaultState();
    private static final BlockState FLOWER_3 = Blocks.PINK_TULIP.getDefaultState();

    // Logs / Leaves für Baumtypen
    private static final BlockState OAK_LOG = Blocks.OAK_LOG.getDefaultState();
    private static final BlockState BIRCH_LOG = Blocks.BIRCH_LOG.getDefaultState();
    private static final BlockState SPRUCE_LOG = Blocks.SPRUCE_LOG.getDefaultState();

    private static final BlockState OAK_LEAVES =
            Blocks.OAK_LEAVES.getDefaultState()
                    .with(LeavesBlock.PERSISTENT, true)
                    .with(LeavesBlock.DISTANCE, 7);

    private static final BlockState BIRCH_LEAVES =
            Blocks.BIRCH_LEAVES.getDefaultState()
                    .with(LeavesBlock.PERSISTENT, true)
                    .with(LeavesBlock.DISTANCE, 7);

    private static final BlockState SPRUCE_LEAVES =
            Blocks.SPRUCE_LEAVES.getDefaultState()
                    .with(LeavesBlock.PERSISTENT, true)
                    .with(LeavesBlock.DISTANCE, 7);

    // „Felsen“-Material
    private static final BlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockState COBBLESTONE = Blocks.COBBLESTONE.getDefaultState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.getDefaultState();

    private IslandDecorator() {
    }

    /**
     * Dekoriert eine generierte Ocean-Insel.
     * Muss auf dem Server-Thread laufen.
     */
    public static void decorateOceanIsland(ServerWorld world,
                                           IslandHeightmap heightmap,
                                           int islandRadiusBlocks,
                                           int seaLevel,
                                           IslandStyle style,
                                           long seed) {

        LOG.info("Decorating island (radius={} blocks, style={}) in world '{}'",
                islandRadiusBlocks, style.name(), world.getRegistryKey().getValue());

        try {
            Random random = new Random(seed ^ (style.ordinal() * 31L) ^ 0xDEADBEEFCAFEL);

            // 1) Beach-Rand
            decorateBeachRing(world, heightmap, islandRadiusBlocks, seaLevel, style, random);

            // 2) Optional: Rock-Patches
            if (style.groundProfile().stonePatchChance() > 0.01f) {
                placeRockPatches(world, heightmap, islandRadiusBlocks, seaLevel,
                        style.groundProfile().stonePatchChance(), random);
            }

            // 3) Tree-Spots sammeln
            float treeSpotDensity = computeTreeSpotDensity(islandRadiusBlocks, style.treeProfile());
            List<BlockPos> treeSpots = findTreeSpots(world, heightmap, islandRadiusBlocks, seaLevel, random, treeSpotDensity);

            int treeCount = computeTreeCount(islandRadiusBlocks, style.treeProfile(), random);
            if (!treeSpots.isEmpty()) {
                treeCount = Math.min(treeCount, treeSpots.size());
            }

            LOG.info("Placing {} trees on island (style={}).", treeCount, style.name());

            for (int i = 0; i < treeCount; i++) {
                BlockPos base = treeSpots.get(i);
                placeStyleTree(world, base, heightmap, style.treeProfile(), random);
            }

            // 4) Flora
            decorateFlora(world, heightmap, islandRadiusBlocks, seaLevel, style.floraProfile(), random);

            LOG.info("Decoration of island in world '{}' (style={}) finished.",
                    world.getRegistryKey().getValue(), style.name());

        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,           // falls du einen dedizierten WORLDGEN/DECORATION-Code hast, hier tauschen
                            CoreErrorSeverity.ERROR,
                            "Exception during island decoration"
                    )
                    .withCause(e)
                    .withContextEntry("world", world.getRegistryKey().getValue().toString())
                    .withContextEntry("radiusBlocks", islandRadiusBlocks)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("style", style != null ? style.name() : "null")
                    .withContextEntry("seed", seed);

            LOG.error(error.toLogString(), e);
        }
    }

    // ------------------------------------------------------
    // 1. Beach-Rand
    // ------------------------------------------------------

    private static void decorateBeachRing(ServerWorld world,
                                          IslandHeightmap heightmap,
                                          int radiusBlocks,
                                          int seaLevel,
                                          IslandStyle style,
                                          Random random) {

        int padding = 3;
        int minRadius = radiusBlocks - padding;
        int maxRadius = radiusBlocks + 1;

        int step = 2;

        float baseCoverage = 0.4f;
        float coverage = baseCoverage * style.beachCoverageFactor();

        for (int x = -radiusBlocks - 2; x <= radiusBlocks + 2; x += step) {
            for (int z = -radiusBlocks - 2; z <= radiusBlocks + 2; z += step) {

                double dist = Math.sqrt(x * x + z * z);
                if (dist < minRadius || dist > maxRadius) continue;

                if (random.nextFloat() > coverage) continue;

                int h = heightmap.sampleHeight(x, z);
                if (h < 0) continue;

                BlockPos top = new BlockPos(x, h, z);
                BlockState current = world.getBlockState(top);
                if (!current.isOf(Blocks.GRASS_BLOCK) &&
                        !current.isOf(Blocks.DIRT) &&
                        !current.isOf(Blocks.COARSE_DIRT)) {
                    continue;
                }

                // Sand nach unten
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos p = top.down(dy);
                    if (p.getY() < world.getBottomY()) break;
                    BlockState s = world.getBlockState(p);
                    if (s.isOf(Blocks.AIR) || s.isOf(Blocks.WATER)) break;
                    world.setBlockState(p, SAND, Block.NOTIFY_LISTENERS);
                }

                // Wasser bis Meeresspiegel auffüllen
                for (int y = h + 1; y <= seaLevel; y++) {
                    BlockPos waterPos = new BlockPos(x, y, z);
                    if (world.getBlockState(waterPos).isAir()) {
                        world.setBlockState(waterPos, WATER, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------
    // 2. Rock-Patches / kleine Hügel
    // ------------------------------------------------------

    private static void placeRockPatches(ServerWorld world,
                                         IslandHeightmap heightmap,
                                         int radiusBlocks,
                                         int seaLevel,
                                         float stonePatchChance,
                                         Random random) {

        int basePatches = Math.max(1, radiusBlocks / 16);
        int patchCount = (int) Math.round(basePatches * (0.5 + stonePatchChance * 2.0));

        for (int i = 0; i < patchCount; i++) {
            int px = random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
            int pz = random.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;

            double dist = Math.sqrt(px * px + pz * pz);
            if (dist > radiusBlocks - 4) continue;

            int h = heightmap.sampleHeight(px, pz);
            if (h < 0) continue;
            if (h <= seaLevel + 1) continue;

            int radius = 2 + random.nextInt(3); // 2–4
            carveRockPatch(world, new BlockPos(px, h, pz), radius, random);
        }
    }

    private static void carveRockPatch(ServerWorld world, BlockPos center, int radius, Random random) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + random.nextFloat() * 0.7f) continue;

                BlockPos ground = center.add(dx, 0, dz);
                BlockState below = world.getBlockState(ground);

                if (!below.isOf(Blocks.GRASS_BLOCK)
                        && !below.isOf(Blocks.DIRT)
                        && !below.isOf(Blocks.COARSE_DIRT)) {
                    continue;
                }

                float r = random.nextFloat();
                BlockState newState;
                if (r < 0.5f) newState = STONE;
                else if (r < 0.7f) newState = COBBLESTONE;
                else if (r < 0.85f) newState = GRAVEL;
                else newState = COARSE_DIRT;

                world.setBlockState(ground, newState, Block.NOTIFY_LISTENERS);

                BlockPos above = ground.up();
                if (!world.getBlockState(above).isAir()) {
                    world.setBlockState(above, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    // ------------------------------------------------------
    // 3. Tree-Spots & Anzahl
    // ------------------------------------------------------

    private static float computeTreeSpotDensity(int radiusBlocks,
                                                IslandStyle.TreeProfile treeProfile) {
        float base = 0.18f;
        float factor = Math.min(0.5f, treeProfile.maxTrees() / 32.0f);
        return Math.min(0.6f, base + factor);
    }

    private static List<BlockPos> findTreeSpots(ServerWorld world,
                                                IslandHeightmap heightmap,
                                                int radiusBlocks,
                                                int seaLevel,
                                                Random random,
                                                float density) {
        List<BlockPos> spots = new ArrayList<>();

        int innerRadius = radiusBlocks - 4;
        int step = 2;

        for (int x = -innerRadius; x <= innerRadius; x += step) {
            for (int z = -innerRadius; z <= innerRadius; z += step) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > innerRadius) continue;

                int h = heightmap.sampleHeight(x, z);
                if (h < 0) continue;
                if (h <= seaLevel + 1) continue;

                BlockPos ground = new BlockPos(x, h, z);
                BlockState groundState = world.getBlockState(ground);
                if (!groundState.isOf(Blocks.GRASS_BLOCK)
                        && !groundState.isOf(Blocks.DIRT)
                        && !groundState.isOf(Blocks.COARSE_DIRT)) {
                    continue;
                }

                BlockPos above = ground.up();
                if (!world.getBlockState(above).isAir()) continue;

                if (random.nextFloat() > density) continue;

                spots.add(ground);
            }
        }

        Collections.shuffle(spots, random);
        return spots;
    }

    private static int computeTreeCount(int radiusBlocks,
                                        IslandStyle.TreeProfile treeProfile,
                                        Random random) {

        int min = treeProfile.minTrees();
        int max = treeProfile.maxTrees();

        double areaFactor = Math.min(1.5, Math.max(0.5, (radiusBlocks * radiusBlocks) / 1024.0));
        int base = (int) Math.round(min + (max - min) * areaFactor * 0.66);

        base += random.nextInt(3) - 1; // -1..+1

        if (base < min) base = min;
        if (base > max) base = max;
        return base;
    }

    // ------------------------------------------------------
    // 4. Tree-Placer mit Biome-Override
    // ------------------------------------------------------

    private static void placeStyleTree(ServerWorld world,
                                       BlockPos ground,
                                       IslandHeightmap heightmap,
                                       IslandStyle.TreeProfile profile,
                                       Random random) {

        IslandHeightmap.BiomeType biome = heightmap.pickBiome(ground.getX(), ground.getZ());

        // Biome-spezifische Overrides
        switch (biome) {
            case JUNGLE -> {
                placeOakLikeTree(world, ground, IslandStyle.TreeProfile.TreeType.PALM_LIKE_OAK, random);
                return;
            }
            case SAVANNA -> {
                placeOakLikeTree(world, ground, IslandStyle.TreeProfile.TreeType.SPARSE_OAK, random);
                return;
            }
            case TAIGA -> {
                placeSpruceTree(world, ground, random);
                return;
            }
            case FROSTLAND -> {
                // keine Bäume
                return;
            }
            case MEADOW -> {
                // fällt auf Style zurück
            }
        }

        // Style-basierte Standard-Bepflanzung
        IslandStyle.TreeProfile.TreeType type = profile.type();

        switch (type) {
            case OAK, SPARSE_OAK, PALM_LIKE_OAK -> placeOakLikeTree(world, ground, type, random);
            case MIXED_OAK_BIRCH -> {
                if (random.nextBoolean()) {
                    placeOakLikeTree(world, ground, IslandStyle.TreeProfile.TreeType.OAK, random);
                } else {
                    placeBirchTree(world, ground, random);
                }
            }
            case SPARSE_SPRUCE -> placeSpruceTree(world, ground, random);
        }
    }

    private static void placeOakLikeTree(ServerWorld world,
                                         BlockPos ground,
                                         IslandStyle.TreeProfile.TreeType type,
                                         Random random) {

        switch (type) {
            case OAK -> {
                placeSimpleBlobTree(world, ground, random,
                        OAK_LOG, OAK_LEAVES,
                        4, 6,
                        2
                );
            }
            case SPARSE_OAK -> {
                placeSimpleBlobTree(world, ground, random,
                        OAK_LOG, OAK_LEAVES,
                        3, 4,
                        1
                );
            }
            case PALM_LIKE_OAK -> {
                int trunkHeight = 5 + random.nextInt(3); // 5–7

                for (int i = 1; i <= trunkHeight; i++) {
                    BlockPos pos = ground.up(i);
                    if (!world.getBlockState(pos).isAir()) return;
                    world.setBlockState(pos, OAK_LOG, Block.NOTIFY_LISTENERS);
                }

                BlockPos top = ground.up(trunkHeight);

                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                        BlockPos lp = top.add(dx, 0, dz);
                        if (!world.getBlockState(lp).isAir()) continue;
                        if (random.nextFloat() < 0.9f) {
                            world.setBlockState(lp, OAK_LEAVES, Block.NOTIFY_LISTENERS);
                        }
                    }
                }

                if (world.getBlockState(top.up()).isAir()) {
                    world.setBlockState(top.up(), OAK_LEAVES, Block.NOTIFY_LISTENERS);
                }
            }
            default -> {
                placeSimpleBlobTree(world, ground, random,
                        OAK_LOG, OAK_LEAVES,
                        4, 5,
                        2
                );
            }
        }
    }

    private static void placeBirchTree(ServerWorld world,
                                       BlockPos ground,
                                       Random random) {
        placeSimpleBlobTree(world, ground, random,
                BIRCH_LOG, BIRCH_LEAVES,
                5, 7,
                2
        );
    }

    private static void placeSpruceTree(ServerWorld world,
                                        BlockPos ground,
                                        Random random) {

        int trunkHeight = 5 + random.nextInt(3);

        for (int i = 1; i <= trunkHeight; i++) {
            BlockPos pos = ground.up(i);
            if (!world.getBlockState(pos).isAir()) return;
            world.setBlockState(pos, SPRUCE_LOG, Block.NOTIFY_LISTENERS);
        }

        BlockPos top = ground.up(trunkHeight);

        int maxRadius = 2;
        for (int dy = -2; dy <= 1; dy++) {
            int yLevel = top.getY() + dy;
            int radius = maxRadius - Math.max(0, dy + 1);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;

                    BlockPos lp = new BlockPos(top.getX() + dx, yLevel, top.getZ() + dz);
                    if (!world.getBlockState(lp).isAir()) continue;

                    if (random.nextFloat() < 0.9f) {
                        world.setBlockState(lp, SPRUCE_LEAVES, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        BlockPos tip = top.up();
        if (world.getBlockState(tip).isAir()) {
            world.setBlockState(tip, SPRUCE_LEAVES, Block.NOTIFY_LISTENERS);
        }
    }

    private static void placeSimpleBlobTree(ServerWorld world,
                                            BlockPos ground,
                                            Random random,
                                            BlockState logState,
                                            BlockState leafState,
                                            int minHeight,
                                            int maxHeight,
                                            int leafRadius) {

        int trunkHeight = minHeight + random.nextInt(maxHeight - minHeight + 1);

        for (int i = 1; i <= trunkHeight; i++) {
            BlockPos pos = ground.up(i);
            if (!world.getBlockState(pos).isAir()) {
                return;
            }
            world.setBlockState(pos, logState, Block.NOTIFY_LISTENERS);
        }

        BlockPos top = ground.up(trunkHeight);

        for (int dx = -leafRadius; dx <= leafRadius; dx++) {
            for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.5);
                    if (dist > leafRadius + 0.5) continue;

                    BlockPos lp = top.add(dx, dy, dz);
                    if (!world.getBlockState(lp).isAir()) continue;

                    if (random.nextFloat() < 0.85f) {
                        world.setBlockState(lp, leafState, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------
    // 5. Flora mit Biome-Overrides
    // ------------------------------------------------------

    private static void decorateFlora(ServerWorld world,
                                      IslandHeightmap heightmap,
                                      int radiusBlocks,
                                      int seaLevel,
                                      IslandStyle.FloraProfile flora,
                                      Random random) {

        float total = flora.total();
        float grassThreshold = flora.grassChance() / total;
        float flowerThreshold = grassThreshold + flora.flowerChance() / total;
        float bushThreshold = flowerThreshold + flora.bushChance() / total;

        for (int x = -radiusBlocks; x <= radiusBlocks; x++) {
            for (int z = -radiusBlocks; z <= radiusBlocks; z++) {

                double dist = Math.sqrt(x * x + z * z);
                if (dist > radiusBlocks) continue;

                int h = heightmap.sampleHeight(x, z);
                if (h < 0) continue;
                if (h <= seaLevel + 1) continue;

                BlockPos ground = new BlockPos(x, h, z);
                BlockState groundState = world.getBlockState(ground);
                if (!groundState.isOf(Blocks.GRASS_BLOCK)
                        && !groundState.isOf(Blocks.DIRT)
                        && !groundState.isOf(Blocks.COARSE_DIRT)
                        && !groundState.isOf(Blocks.PODZOL)) {
                    continue;
                }

                BlockPos above = ground.up();
                if (!world.getBlockState(above).isAir()) continue;

                IslandHeightmap.BiomeType biome = heightmap.pickBiome(x, z);
                float r = random.nextFloat();

                // Biome-spezifische Flora
                switch (biome) {
                    case JUNGLE -> {
                        if (r < 0.6f) {
                            world.setBlockState(above, TALL_GRASS, Block.NOTIFY_LISTENERS);
                        } else if (r < 0.85f) {
                            placeLeafBush(world, above, random);
                        }
                        continue;
                    }
                    case SAVANNA -> {
                        if (r < 0.3f) {
                            world.setBlockState(above, TALL_GRASS, Block.NOTIFY_LISTENERS);
                        }
                        continue;
                    }
                    case TAIGA -> {
                        if (r < 0.1f) {
                            world.setBlockState(above, FERN, Block.NOTIFY_LISTENERS);
                        }
                        continue;
                    }
                    case FROSTLAND -> {
                        // keine Flora
                        continue;
                    }
                    case MEADOW -> {
                        // fällt auf Style-Profil zurück (unten)
                    }
                }

                // Style-basierte Standard-Flora
                if (r < grassThreshold) {
                    BlockState plant = random.nextBoolean() ? TALL_GRASS : FERN;
                    world.setBlockState(above, plant, Block.NOTIFY_LISTENERS);
                } else if (r < flowerThreshold) {
                    BlockState flower = pickRandomFlower(random);
                    world.setBlockState(above, flower, Block.NOTIFY_LISTENERS);
                } else if (r < bushThreshold) {
                    placeLeafBush(world, above, random);
                }
            }
        }
    }

    private static BlockState pickRandomFlower(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> FLOWER_1;
            case 1 -> FLOWER_2;
            case 2 -> FLOWER_3;
            case 3 -> Blocks.AZURE_BLUET.getDefaultState();
            default -> Blocks.OXEYE_DAISY.getDefaultState();
        };
    }

    private static void placeLeafBush(ServerWorld world, BlockPos center, Random random) {
        BlockState leaves = OAK_LEAVES;
        world.setBlockState(center, leaves, Block.NOTIFY_LISTENERS);

        if (random.nextBoolean()) world.setBlockState(center.north(), leaves, Block.NOTIFY_LISTENERS);
        if (random.nextBoolean()) world.setBlockState(center.south(), leaves, Block.NOTIFY_LISTENERS);
        if (random.nextBoolean()) world.setBlockState(center.east(), leaves, Block.NOTIFY_LISTENERS);
        if (random.nextBoolean()) world.setBlockState(center.west(), leaves, Block.NOTIFY_LISTENERS);

        if (random.nextFloat() < 0.3f) {
            world.setBlockState(center.up(), leaves, Block.NOTIFY_LISTENERS);
        }
    }
}
