package dev.huntertagog.coresystem.fabric.server.world.island;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.world.OpenSimplexNoise;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Random;

/**
 * @param peaks    für Multi-Peaks
 * @param clusters für Cluster-Varianten
 */
public record IslandHeightmap(int islandRadiusBlocks, int baseHeight, Shape shape, Peak[] peaks, Cluster[] clusters,
                              int seaLevel) {

    private static final Logger LOG = LoggerFactory.get("IslandHeightmap");

    public enum Shape {
        REALISTIC_SINGLE,
        REALISTIC_MULTI_PEAKS,
        CLUSTER,
        CLUSTER_LAGOONS
    }

    public enum BiomeType {
        MEADOW,
        SAVANNA,
        JUNGLE,
        TAIGA,
        FROSTLAND
    }

    private record Peak(int x, int z, int radius, int height) {
    }

    private record Cluster(int x, int z, int radius, int height) {
    }

    // Simplex-Noise Instanz
    private static final OpenSimplexNoise simplex = new OpenSimplexNoise(1234L);

    // -------- Height-Cache (per Thread) für sampleBaseHeight --------

    private static final int BASE_CACHE_SIZE = 2048; // power of two
    private static final ThreadLocal<BaseHeightCache> BASE_CACHE =
            ThreadLocal.withInitial(BaseHeightCache::new);

    private static final class BaseHeightCache {
        private final long[] keys = new long[BASE_CACHE_SIZE];
        private final int[] values = new int[BASE_CACHE_SIZE];

        private BaseHeightCache() {
            Arrays.fill(keys, Long.MIN_VALUE);
        }

        int getOrCompute(IslandHeightmap map, int x, int z) {
            long key = (((long) x) << 32) ^ (z & 0xffffffffL);
            int idx = (int) (key & (BASE_CACHE_SIZE - 1));

            if (keys[idx] == key) {
                return values[idx];
            }

            int v = map.computeBaseHeight(x, z);
            keys[idx] = key;
            values[idx] = v;
            return v;
        }
    }

    public int radius() {
        return islandRadiusBlocks;
    }

    // ---------------- Factory-Methoden ----------------

    public static IslandHeightmap createRealisticSingle(int radius,
                                                        int baseHeight,
                                                        int seaLevel,
                                                        long seed) {
        int r = radius;
        if (r <= 0) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "IslandHeightmap.createRealisticSingle called with non-positive radius"
                    )
                    .withContextEntry("radius", radius)
                    .withContextEntry("baseHeight", baseHeight)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("seed", seed);
            error.log();

            LOG.warn("createRealisticSingle(): radius={} invalid, falling back to radius=64.", radius);
            r = 64;
        }

        return new IslandHeightmap(r, baseHeight,
                Shape.REALISTIC_SINGLE, null, null, seaLevel);
    }

    public static IslandHeightmap createRealisticMultiPeak(int radius,
                                                           int baseHeight,
                                                           int seaLevel,
                                                           long seed) {

        int r = radius;
        if (r <= 0) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "IslandHeightmap.createRealisticMultiPeak called with non-positive radius"
                    )
                    .withContextEntry("radius", radius)
                    .withContextEntry("baseHeight", baseHeight)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("seed", seed);
            error.log();

            LOG.warn("createRealisticMultiPeak(): radius={} invalid, falling back to radius=64.", radius);
            r = 64;
        }

        Random rdm = new Random(seed ^ 0xBEEF1234L);
        int peakCount = 2 + rdm.nextInt(2); // 2–3 Gipfel

        Peak[] peaks = new Peak[peakCount];
        for (int i = 0; i < peakCount; i++) {
            double ring = r * (0.25 + rdm.nextDouble() * 0.35); // 25–60% des Radius
            double angle = rdm.nextDouble() * Math.PI * 2.0;

            int cx = (int) Math.round(Math.cos(angle) * ring);
            int cz = (int) Math.round(Math.sin(angle) * ring);

            int pRadius = (int) (r * (0.35 + rdm.nextDouble() * 0.15)); // 35–50% des Radius
            int pHeight = 10 + rdm.nextInt(6); // 10–15 Block „Gipfelhöhe“ relativ zu baseHeight

            peaks[i] = new Peak(cx, cz, pRadius, pHeight);
        }

        return new IslandHeightmap(r, baseHeight,
                Shape.REALISTIC_MULTI_PEAKS, peaks, null, seaLevel);
    }

    public static IslandHeightmap createCluster(int radius,
                                                int baseHeight,
                                                int seaLevel,
                                                long seed,
                                                int clusterCount) {

        int r = radius;
        if (r <= 0) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "IslandHeightmap.createCluster called with non-positive radius"
                    )
                    .withContextEntry("radius", radius)
                    .withContextEntry("baseHeight", baseHeight)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("seed", seed)
                    .withContextEntry("clusterCount", clusterCount);
            error.log();

            LOG.warn("createCluster(): radius={} invalid, falling back to radius=64.", radius);
            r = 64;
        }

        int count = clusterCount;
        if (count < 1) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "IslandHeightmap.createCluster called with clusterCount < 1"
                    )
                    .withContextEntry("radius", r)
                    .withContextEntry("baseHeight", baseHeight)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("seed", seed)
                    .withContextEntry("clusterCount", clusterCount);
            error.log();

            LOG.warn("createCluster(): clusterCount={} invalid, forcing clusterCount=1.", clusterCount);
            count = 1;
        }

        Random random = new Random(seed ^ 0xC0FFEE1234L);
        Cluster[] clusters = new Cluster[count];

        // ---------- 1) Hauptinsel in der Mitte ----------
        int minMainRadiusBlocks = 4 * 16; // 4 Chunks Radius = 64 Blöcke
        int mainRadius = Math.min(r - 8, Math.max(minMainRadiusBlocks, r / 2));
        if (mainRadius < 16) {
            mainRadius = 16; // Failsafe
        }

        int mainHeight = 10 + random.nextInt(8); // 10–17 Blöcke über baseHeight

        clusters[0] = new Cluster(
                0,          // Hauptinsel im Zentrum
                0,
                mainRadius,
                mainHeight
        );

        // ---------- 2) Satelliten-Inseln außenrum ----------
        for (int i = 1; i < count; i++) {
            double ring = r * (0.45 + random.nextDouble() * 0.4); // 45–85% Radius
            double angle = (2 * Math.PI / (count - 1)) * (i - 1) + random.nextDouble() * 0.5;

            int cx = (int) Math.round(Math.cos(angle) * ring);
            int cz = (int) Math.round(Math.sin(angle) * ring);

            int cRadius = 6 + random.nextInt(8);   // 6–13 Block Radius
            int cHeight = 6 + random.nextInt(6);   // 6–11 Block Höhe

            clusters[i] = new Cluster(cx, cz, cRadius, cHeight);
        }

        return new IslandHeightmap(r, baseHeight,
                Shape.CLUSTER, null, clusters, seaLevel);
    }

    public static IslandHeightmap createClusterWithLagoons(int radius,
                                                           int baseHeight,
                                                           int seaLevel,
                                                           long seed,
                                                           int clusterCount) {
        IslandHeightmap base = createCluster(radius, baseHeight, seaLevel, seed, clusterCount);
        return new IslandHeightmap(base.islandRadiusBlocks,
                base.baseHeight,
                Shape.CLUSTER_LAGOONS,
                base.peaks,
                base.clusters,
                seaLevel);
    }

    // ---------------- zentrale Abfrage ----------------

    /**
     * @return -1 => kein Land, sonst Y-Höhe der Geländeoberkante mit
     * Erosion, Canyons, Küsten-Shaping und optionalen Terrassen.
     */
    public int sampleHeight(int x, int z) {
        try {
            int base = sampleBaseHeight(x, z);
            if (base < 0) {
                return -1;
            }

            double height = base;

            // deterministischer "Zufall" über Noise (0..1)
            double selector = (simplex.noise2(x * 0.013, z * 0.013) + 1.0) * 0.5;

            // ~30%: hydraulische Erosion
            if (selector > 0.35 && selector < 0.65) {
                height = applyHydraulicErosion(height, x, z);
            }

            // ~30%: thermische Erosion
            double selector2 = (simplex.noise2((x + 512) * 0.017, (z - 777) * 0.017) + 1.0) * 0.5;
            if (selector2 > 0.4 && selector2 < 0.7) {
                height = applyThermalErosion(height, x, z);
            }

            // ~20%: Canyon-Cuts (abtragend)
            double canyonMask = (simplex.noise2((x - 1234) * 0.01, (z + 987) * 0.01) + 1.0) * 0.5;
            if (canyonMask > 0.8) {
                height -= canyonNoise(x, z);
            }

            // ------------------------------
            // Küsten-Shaping: flatten ODER strukturieren (random)
            // ------------------------------
            double coastMask = coastalBandMask(x, z);
            if (coastMask > 0.10) {
                double variant = (simplex.noise2((x - 345) * 0.02, (z + 678) * 0.02) + 1.0) * 0.5;

                if (variant < 0.5) {
                    // flacher, sanfter Übergang
                    height = flattenCoast(height, x, z, coastMask);
                } else {
                    // mehr Struktur / Terrassen an der Küste
                    height = sculptCoast(height, x, z, coastMask);
                }
            }

            // Terrassen / flache Ebenen (nicht überall)
            double terraceMask = (simplex.noise2((x - 999) * 0.02, (z + 321) * 0.02) + 1.0) * 0.5;
            if (terraceMask > 0.45 && terraceMask < 0.85) {
                height = applyTerraces(height);
            }

            return (int) Math.round(height);
        } catch (Throwable t) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "IslandHeightmap.sampleHeight failed"
                    )
                    .withContextEntry("x", x)
                    .withContextEntry("z", z)
                    .withContextEntry("shape", String.valueOf(shape))
                    .withContextEntry("radius", islandRadiusBlocks)
                    .withContextEntry("baseHeight", baseHeight)
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("exception", t.toString());
            error.log();

            LOG.error("sampleHeight failed at ({}, {}) for shape {} – returning -1.",
                    x, z, shape, t);
            // Failsafe: kein Land → Generator baut Wasser / Seafloor
            return -1;
        }
    }

    /**
     * Cache-Schicht für Basis-Höhe ohne Erosion/Canyon/Terrassen.
     */
    private int sampleBaseHeight(int x, int z) {
        return BASE_CACHE.get().getOrCompute(this, x, z);
    }

    /**
     * Tatsächliche Basis-Höhensamples (ohne Cache).
     */
    private int computeBaseHeight(int x, int z) {
        return switch (shape) {
            case REALISTIC_SINGLE -> sampleRealisticAdvanced(x, z);
            case REALISTIC_MULTI_PEAKS -> sampleMultiPeakAdvanced(x, z);
            case CLUSTER -> sampleClusterAdvanced(x, z, false);
            case CLUSTER_LAGOONS -> {
                int base = sampleClusterAdvanced(x, z, true);
                if (base < 0) {
                    yield -1;
                }
                int bonus = (int) Math.round(lagoonBonus(x, z));
                yield base + bonus;
            }
        };
    }

    private float lagoonBonus(int x, int z) {
        double n = simplex.noise2(x * 0.1, z * 0.1);
        return (float) (n * 1.5); // bis zu ~1.5 Block Bonus-Höhe
    }

    // ============= REALISTIC SINGLE (advanced) =============

    private int sampleRealisticAdvanced(int x, int z) {

        double dist = Math.sqrt(x * x + z * z);
        if (dist > islandRadiusBlocks) {
            return -1;
        }

        double norm = dist / islandRadiusBlocks;

        // 1) Smooth falloff from coast
        double coast = Math.pow(1.0 - norm, 1.4);

        // 2) Domain warp für organische Küstenformen
        double warp = simplex.domainWarp2(x * 0.05, z * 0.05) * 4;

        // 3) Ridge noise für markanten Gipfel
        double ridge = simplex.ridge(x, z, 0.02, 18);

        // 4) Base noise
        double base = simplex.noise2(x * 0.03, z * 0.03) * 3;

        double height = (ridge + base + warp) * coast;

        return (int) Math.round(baseHeight + height);
    }

    // ============= REALISTIC MULTI-PEAKS (advanced) =============

    private int sampleMultiPeakAdvanced(int x, int z) {

        double dist = Math.sqrt(x * x + z * z);
        if (dist > islandRadiusBlocks) return -1;

        // Domain warp für Chaos
        double wx = x + simplex.noise2(x * 0.04, z * 0.04) * 10;
        double wz = z + simplex.noise2(x * 0.04, z * 0.04) * 10;

        double v = 0;

        // mehrere Ridge-Bänder → viele Gipfel
        v += simplex.ridge(wx, wz, 0.02, 22);
        v += simplex.ridge(wx + 300, wz + 300, 0.03, 18);
        v += simplex.ridge(wx - 500, wz + 120, 0.015, 26);

        // Küsten-Abfall
        double coast = Math.pow(1.0 - (dist / islandRadiusBlocks), 1.6);

        double height = v * coast;

        return (int) Math.round(baseHeight + height);
    }

    // ============= CLUSTER & CLUSTER-LAGOONS (advanced) =============

    private int sampleClusterAdvanced(int x, int z, boolean lagoons) {

        double outerDist = Math.sqrt(x * x + z * z);
        if (outerDist > islandRadiusBlocks) {
            return -1;
        }

        if (clusters == null || clusters.length == 0) {
            return -1;
        }

        double relHeight = Double.NEGATIVE_INFINITY;

        // ---------- 1) Cluster-Hügel / Hauptinsel / Satelliten ----------
        for (Cluster c : clusters) {
            double dx = x - c.x();
            double dz = z - c.z();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < c.radius() * 1.2) {
                double blob = Math.cos((dist / c.radius()) * Math.PI) * c.height();
                double ridge = simplex.ridge(x + c.x(), z + c.z(), 0.04, 8);
                relHeight = Math.max(relHeight, blob + ridge);
            }
        }

        // ---------- 2) Lagunen-Brücken (optional) ----------
        if (lagoons) {
            double lagoonHeight = computeLagoonHeight(x, z);
            relHeight = Math.max(relHeight, lagoonHeight);
        }

        if (relHeight < 0) {
            return -1;
        }

        // ---------- 3) Basis-Rauheit ----------
        double noise = simplex.noise2(x * 0.05, z * 0.05) * 2.0;
        double worldHeight = baseHeight + relHeight + noise;

        // ---------- 4) Harte Kanten nach unten entschärfen ----------
        double norm = outerDist / islandRadiusBlocks; // 0 im Zentrum, 1 am Rand

        if (norm > 0.80) {
            double t = Math.min(1.0, (norm - 0.80) / 0.20); // 0.0 bei 0.80, 1.0 bei 1.0

            int floor = sampleSeafloorHeight(x, z);
            // Ziel: nicht tiefer als ein weicher Küstenabfall
            double target = Math.max(floor + 3.0, seaLevel - 2.0);

            worldHeight = lerp(worldHeight, target, t);
        }

        return (int) Math.round(worldHeight);
    }

    private double computeLagoonHeight(int x, int z) {
        if (clusters == null || clusters.length < 2) {
            return Double.NEGATIVE_INFINITY;
        }

        double bestHeight = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < clusters.length; i++) {
            Cluster a = clusters[i];
            Cluster b = clusters[(i + 1) % clusters.length];

            double t = projectPointOnSegmentT(x, z, a.x(), a.z(), b.x(), b.z());
            if (t <= 0.0 || t >= 1.0) continue;

            double px = a.x() + (b.x() - a.x()) * t;
            double pz = a.z() + (b.z() - a.z()) * t;

            double distToBridge = Math.hypot(x - px, z - pz);
            double maxWidth = 4.5; // ~9 Blöcke

            if (distToBridge < maxWidth) {
                double factor = 1.0 - (distToBridge / maxWidth);
                // flache Sandbank knapp über Meeresspiegel
                double h = (seaLevel - 1 - baseHeight) * factor;
                bestHeight = Math.max(bestHeight, h);
            }
        }

        return bestHeight;
    }

    private static double projectPointOnSegmentT(double x, double z,
                                                 double ax, double az,
                                                 double bx, double bz) {
        double abx = bx - ax;
        double abz = bz - az;

        double apx = x - ax;
        double apz = z - az;

        double abLenSq = abx * abx + abz * abz;
        if (abLenSq <= 0.0001) return 0.0;

        return (apx * abx + apz * abz) / abLenSq;
    }

    // ================== Terrassen / Plateaus ==================

    /**
     * Quantisierte Terrassen oberhalb des Meeresspiegels.
     */
    private double applyTerraces(double worldHeight) {
        // nur oberhalb Meeresspiegel sinnvoll
        if (worldHeight <= seaLevel + 3) {
            return worldHeight;
        }

        double rel = worldHeight - seaLevel;

        double step = 4.0;      // 4-Blöcke-Terrassen
        double blend = 1.2;     // weiche Übergänge +/- 1.2 Block

        double terraced = Math.round(rel / step) * step;
        double diff = terraced - rel;

        if (Math.abs(diff) >= blend) {
            // außerhalb Blendzone => harte Stufe
            return seaLevel + terraced;
        }

        // weicher Übergang innerhalb der Blendzone
        double t = Math.abs(diff) / blend;   // 0..1
        double smooth = 1.0 - t * t;         // simple ease-out
        return seaLevel + rel + diff * smooth;
    }

    // ================== Bodenaufbau ==================

    public static void applyGroundProfile(ServerWorld world,
                                          IslandHeightmap heightmap,
                                          IslandStyle style,
                                          int seaLevel) {

        var ground = style.groundProfile();
        int radius = heightmap.radius();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                int h = heightmap.sampleHeight(x, z);
                if (h < 0) {
                    continue; // Wasser / kein Land
                }

                var random = world.getRandom();
                BiomeType biome = heightmap.pickBiome(x, z);
                double beachMask = heightmap.beachMask(x, z, h);
                double rel = h - seaLevel;

                BlockPos top = new BlockPos(x, h, z);

                // Default aus dem Style
                BlockState surface = ground.surface();
                BlockState subsurface = ground.subsurface();
                BlockState deep = ground.deep();

                // -----------------------------
                // 1) Beach-Zone rund um Meer
                // -----------------------------
                if (beachMask > 0.55 && h <= seaLevel + 2) {
                    // Biome-abhängige "Farbe" des Strandes
                    double noise = (OpenSimplexNoise.fast2d(x * 0.18, z * 0.18) + 1.0) * 0.5;
                    surface = beachSurfaceForBiome(biome, noise);

                    // Unter dem Sand: Sandstone / Sand als Bindeschicht
                    subsurface = (surface.isOf(Blocks.RED_SAND)
                            ? Blocks.RED_SANDSTONE.getDefaultState()
                            : Blocks.SANDSTONE.getDefaultState());
                    deep = Blocks.STONE.getDefaultState();

                    // -----------------------------
                    // 2) Lowlands / flach (Wiesen)
                    // -----------------------------
                } else if (rel <= 6) {
                    // viel Gras, hier und da nackter Boden
                    double r = random.nextDouble();
                    if (r < 0.18) {
                        surface = Blocks.DIRT.getDefaultState();
                    } else if (r < 0.26) {
                        surface = Blocks.COARSE_DIRT.getDefaultState();
                    } else {
                        surface = Blocks.GRASS_BLOCK.getDefaultState();
                    }

                    // Unterbau: klassisch Soil
                    subsurface = ground.subsurface(); // meist Dirt
                    deep = ground.deep();             // z.B. Stone

                    // -----------------------------
                    // 3) Midlands (Hügel, normal)
                    // -----------------------------
                } else if (rel <= 22) {
                    // leicht raueres Terrain, aber noch kein massives Gestein
                    double r = random.nextDouble();
                    surface = Blocks.GRASS_BLOCK.getDefaultState();
                    if (r < ground.coarseDirtChance()) {
                        surface = Blocks.COARSE_DIRT.getDefaultState();
                    } else if (r < ground.coarseDirtChance() + ground.podzolChance()) {
                        surface = Blocks.PODZOL.getDefaultState();
                    }

                    subsurface = ground.subsurface();
                    deep = ground.deep();

                    // -----------------------------
                    // 4) Highlands / Berge (Stone + Andesit/Kies)
                    // -----------------------------
                } else {
                    // Stone-Dominanz + Andesit/Kies-Mix
                    double hNoise = (OpenSimplexNoise.fast2d(x * 0.06, z * 0.06) + 1.0) * 0.5;

                    surface = mountainSurfaceBlock(hNoise, random);
                    subsurface = Blocks.STONE.getDefaultState();
                    deep = Blocks.STONE.getDefaultState();
                }

                // -----------------------------
                // Surface-Block setzen
                // -----------------------------
                world.setBlockState(top, surface, Block.NOTIFY_LISTENERS);

                // -----------------------------
                // 2–3 Blöcke Unterbau pro Säule
                // -----------------------------
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos p = top.down(dy);
                    if (p.getY() < world.getBottomY()) break;

                    BlockState fill;
                    if (dy == 1) {
                        fill = subsurface;
                    } else if (dy == 2) {
                        fill = subsurface; // noch Soil / Bindeschicht
                    } else {
                        fill = deep;       // tieferes Gestein
                    }

                    world.setBlockState(p, fill, Block.NOTIFY_LISTENERS);
                }

                // -----------------------------
                // Punktuelle Stein-Patches im Low-/Midland
                // -----------------------------
                if (rel > 4 && rel < 24 &&
                        ground.stonePatchChance() > 0.0f &&
                        world.getRandom().nextFloat() < ground.stonePatchChance()) {
                    carveStonePatch(world, top.down(1));
                }
            }
        }
    }

    // ================== Helper für Oberflächen-Blöcke ==================

    /**
     * Biome-abhängige "Strand-Farbe".
     */
    private static BlockState beachSurfaceForBiome(BiomeType biome, double noise) {
        // noise: 0..1 für leichte Variation
        switch (biome) {
            case SAVANNA, JUNGLE -> {
                // warme Strände: primär Sand, gelegentlich Red Sand
                if (noise > 0.78) {
                    return Blocks.RED_SAND.getDefaultState();
                }
                return Blocks.SAND.getDefaultState();
            }
            case TAIGA, FROSTLAND -> {
                // kalte Küsten: mehr Kies/Mix
                if (noise > 0.65) {
                    return Blocks.SAND.getDefaultState();
                }
                return Blocks.GRAVEL.getDefaultState();
            }
            default -> {
                // Standard-Strand: Sand mit leichtem Gravel-Anteil
                if (noise < 0.12) {
                    return Blocks.GRAVEL.getDefaultState();
                }
                return Blocks.SAND.getDefaultState();
            }
        }
    }

    /**
     * Oberflächenblock für Hochgebirge (Stone + Andesit + Kies).
     */
    private static BlockState mountainSurfaceBlock(double noise, net.minecraft.util.math.random.Random random) {
        // noise: 0..1 – grobe Struktur
        // Kombiniert mit leichter Zufallskomponente → natürlicher Mix
        double r = (noise * 0.7) + (random.nextDouble() * 0.3);

        if (r < 0.60) {
            return Blocks.STONE.getDefaultState();
        } else if (r < 0.82) {
            return Blocks.ANDESITE.getDefaultState();
        } else {
            return Blocks.GRAVEL.getDefaultState();
        }
    }

    private static void carveStonePatch(ServerWorld world, BlockPos center) {
        int radius = 1 + world.getRandom().nextInt(2); // 1–2
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.5);
                    if (dist > radius + 0.3) continue;

                    BlockPos p = center.add(dx, dy, dz);
                    BlockState s = world.getBlockState(p);
                    if (s.isOf(Blocks.GRASS_BLOCK) ||
                            s.isOf(Blocks.DIRT) ||
                            s.isOf(Blocks.COARSE_DIRT) ||
                            s.isOf(Blocks.SAND)) {
                        world.setBlockState(p, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    // ================== Biome-Noise ==================

    public double temperature(int x, int z) {
        double t = simplex.noise2(x * 0.005, z * 0.005);
        t += simplex.noise2(x * 0.02, z * 0.02) * 0.25;
        return (t + 1.0) / 2.0; // 0–1
    }

    public double humidity(int x, int z) {
        double h = simplex.noise2((x + 999) * 0.004, (z - 123) * 0.004);
        h += simplex.noise2(x * 0.02, z * 0.02) * 0.25;
        return (h + 1.0) / 2.0;
    }

    public BiomeType pickBiome(int x, int z) {
        double t = temperature(x, z);
        double h = humidity(x, z);

        if (t > 0.7 && h < 0.4) return BiomeType.SAVANNA;
        if (t > 0.6 && h > 0.6) return BiomeType.JUNGLE;
        if (t < 0.3 && h < 0.5) return BiomeType.TAIGA;
        if (t < 0.2) return BiomeType.FROSTLAND;

        return BiomeType.MEADOW;
    }

    // ================== Erosion / Canyon-Noise ==================

    public double applyHydraulicErosion(double height, int x, int z) {
        double hCenter = height;

        double hPosX = neighborBaseHeightOrSelf(hCenter, x + 1, z);
        double hNegX = neighborBaseHeightOrSelf(hCenter, x - 1, z);
        double hPosZ = neighborBaseHeightOrSelf(hCenter, x, z + 1);
        double hNegZ = neighborBaseHeightOrSelf(hCenter, x, z - 1);

        double slope = (
                (hCenter - hPosX) +
                        (hCenter - hNegX) +
                        (hCenter - hPosZ) +
                        (hCenter - hNegZ)
        ) * 0.25;

        double erosion = Math.max(0, slope) * 0.08;
        return height - erosion;
    }

    public double applyThermalErosion(double height, int x, int z) {

        double hCenter = height;

        double hPosX = neighborBaseHeightOrSelf(hCenter, x + 1, z);
        double hNegX = neighborBaseHeightOrSelf(hCenter, x - 1, z);
        double hPosZ = neighborBaseHeightOrSelf(hCenter, x, z + 1);
        double hNegZ = neighborBaseHeightOrSelf(hCenter, x, z - 1);

        double maxDrop = 0;
        maxDrop = Math.max(maxDrop, hCenter - hPosX);
        maxDrop = Math.max(maxDrop, hCenter - hNegX);
        maxDrop = Math.max(maxDrop, hCenter - hPosZ);
        maxDrop = Math.max(maxDrop, hCenter - hNegZ);

        if (maxDrop > 2.5) {
            height -= maxDrop * 0.15;
        }

        return height;
    }

    /**
     * Hilfsfunktion: Nachbarhöhe aus sampleBaseHeight, bei Wasser (-1) fallback auf current.
     */
    private double neighborBaseHeightOrSelf(double selfHeight, int x, int z) {
        int h = sampleBaseHeight(x, z);
        return (h < 0) ? selfHeight : h;
    }

    public double canyonNoise(int x, int z) {
        double n = simplex.ridge(x, z, 0.005, 1);
        return Math.pow(n, 4) * 18; // Canyon-Höhe
    }

    // ================== Beaches / Shelves / Coral Ridges ==================

    /**
     * 0..1: Wahrscheinlichkeit/Maske für Sandstrand an diesem Punkt.
     * Kann z.B. in applyGroundProfile oder IslandDecorator benutzt werden.
     */
    public double beachMask(int x, int z, int terrainHeight) {
        // nur in der Nähe des Meeresspiegels
        int dy = Math.abs(terrainHeight - seaLevel);
        if (dy > 4) return 0.0;

        double dist = Math.sqrt(x * x + z * z);
        // Fokus auf Randbereich der Insel
        double radial = 1.0 - Math.min(1.0, Math.abs(dist - islandRadiusBlocks) / 6.0);

        if (radial <= 0.0) return 0.0;

        double heightFactor = 1.0 - (dy / 4.0); // 1 bei seaLevel, 0 bei +/-4
        double noise = (simplex.noise2(x * 0.08, z * 0.08) + 1.0) * 0.5; // 0..1

        return Math.max(0.0, radial * heightFactor * (0.4 + noise * 0.6));
    }

    /**
     * Seafloor-Höhe (Bathymetrie) um die Insel herum.
     * Kann im ChunkGenerator genutzt werden, um Underwater Shelves zu bauen.
     */
    public int sampleSeafloorHeight(int x, int z) {
        double dist = Math.sqrt(x * x + z * z);

        // maximaler interessanter Ozean-Radius (z.B. bis Worldborder)
        double maxOceanRadius = islandRadiusBlocks * 2.0;

        // Normalisierte Distanz vom Inselrand Richtung Ozean
        double t = 0.0;
        if (dist > islandRadiusBlocks) {
            t = Math.min(1.0, (dist - islandRadiusBlocks) / Math.max(1.0, maxOceanRadius - islandRadiusBlocks));
        }

        // Tiefenprofil: Küstennah = flach, weiter draußen = tief
        double baseDepth = 4.0 + t * 20.0; // 4 .. 24 Blöcke
        double rough = simplex.noise2(x * 0.07, z * 0.07) * 3.0;

        double depth = baseDepth + rough;
        double seafloor = seaLevel - depth;

        return (int) Math.round(seafloor);
    }

    /**
     * 0..1 Maske für Shelf-Zonen (typisch 6–12 Blöcke unter Wasser).
     */
    public double shelfMask(int x, int z) {
        int floor = sampleSeafloorHeight(x, z);
        double depth = seaLevel - floor;
        if (depth < 3 || depth > 20) return 0.0;

        // Gauss-artiger Peak um ~8 Blöcke Tiefe
        double center = 8.0;
        double sigma = 4.0;
        double band = Math.exp(-Math.pow((depth - center) / sigma, 2));

        double noise = simplex.noise2(x * 0.05, z * 0.05) * 0.25;

        return Math.max(0.0, band + noise);
    }

    /**
     * Heuristik, ob an der Position ein Coral Ridge sinnvoll wäre.
     * Nutzt Tiefe + warm/feucht-Biome + Noise.
     */
    public boolean isCoralRidge(int x, int z) {
        int floor = sampleSeafloorHeight(x, z);
        double depth = seaLevel - floor;

        // typische Korallen-Tiefe
        if (depth < 4 || depth > 18) return false;

        BiomeType biome = pickBiome(x, z);
        // nur in "warm/neutralen" Biomen
        if (biome == BiomeType.TAIGA || biome == BiomeType.FROSTLAND) {
            return false;
        }

        double m = (simplex.noise2((x + 2048) * 0.09, (z - 4096) * 0.09) + 1.0) * 0.5;
        return m > 0.75;
    }

    /**
     * 0..1 Maske für einen Ring rund um die Küste.
     * Hoch, wenn wir im „Küstenband“ arbeiten wollen.
     */
    private double coastalBandMask(int x, int z) {
        double dist = Math.sqrt(x * x + z * z);

        double inner = islandRadiusBlocks * 0.55;   // etwas innerhalb der Küste
        double outer = islandRadiusBlocks * 1.05;   // leicht darüber hinaus (wave-wash)
        if (dist < inner || dist > outer) {
            return 0.0;
        }

        double t = (dist - inner) / Math.max(1.0, (outer - inner)); // 0..1
        double band = 1.0 - Math.abs(t - 0.5) * 2.0;                // Dreiecksprofil 0..1..0

        double noise = (simplex.noise2(x * 0.035, z * 0.035) + 1.0) * 0.5; // 0..1

        return Math.max(0.0, band * (0.4 + noise * 0.6));
    }

    /**
     * Küsten-Smoothing: reduziert extreme Kliffs, macht einen
     * halbwegs linearen Übergang von "etwas über Meer" -> Meeresspiegel.
     */
    private double flattenCoast(double height, int x, int z, double mask) {
        double dist = Math.sqrt(x * x + z * z);

        double inner = islandRadiusBlocks * 0.60;
        double outer = islandRadiusBlocks * 1.00;

        double t = (dist - inner) / Math.max(1.0, (outer - inner));
        t = Math.min(1.0, Math.max(0.0, t)); // clamp 0..1

        // inner: +6 Blöcke über Meer, outer: Meeresspiegel
        double target = seaLevel + 6.0 * (1.0 - t);

        double alpha = 0.35 * mask; // wie stark wir hinziehen
        return lerp(height, target, alpha);
    }

    /**
     * Küsten-Struktur: kleine Stufen / Terrassen im Küstenbereich.
     */
    private double sculptCoast(double height, int x, int z, double mask) {
        if (height <= seaLevel + 2) {
            // unter / knapp über Meer: nichts tun
            return height;
        }

        double rel = height - seaLevel;
        double step = 3.0; // 3-Blöcke Levels

        double terraced = Math.round(rel / step) * step;
        double baseDiff = terraced - rel;

        // kleine Variation, damit's nicht zu grid-mäßig ist
        double noise = simplex.noise2(x * 0.08, z * 0.08);
        terraced += noise * 0.6;

        double alpha = 0.25 * mask;

        return lerp(seaLevel + rel, seaLevel + terraced, alpha);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * 0..1 Feld, das „Tunnelintensität“ repräsentiert.
     * Hohe Werte = guter Kandidat für einen Unterwasser-Tunnel der durch die Insel geht.
     */
    private double underwaterCaveField(int x, int z) {
        double dist = Math.sqrt(x * x + z * z);

        // Wir wollen Tunnel, die ungefähr am Inselrand starten und durch die Insel gehen.
        double fromCoast = Math.abs(dist - islandRadiusBlocks);
        if (fromCoast > islandRadiusBlocks) {
            return 0.0;
        }

        // Shelf-Band (flache Unterwasserplattformen) bevorzugen
        double shelf = shelfMask(x, z); // 0..1

        // Richtungsabhängige Ridge-Noise → lange Bänder, die durch die Insel gehen
        double ridge = (simplex.ridge(x, z, 0.012, 6) + 1.0) * 0.5; // grob 0..1
        double dirNoise = (simplex.noise2((x + 1024) * 0.004, (z - 2048) * 0.004) + 1.0) * 0.5;
        double dirBand = 1.0 - Math.abs(dirNoise - 0.5) * 2.0; // 0..1, Peak bei ~0.5

        double base = ridge * (0.3 + 0.7 * shelf) * dirBand;

        return Math.max(0.0, base);
    }

    /**
     * Heuristik, ob an (x,y,z) ein Unterwasser-Tunnel verlaufen soll.
     * Nutzt Seafloor, Meereshöhe und das Tunnel-Feld.
     */
    public boolean isUnderwaterCaveAt(int x, int y, int z) {
        int floor = sampleSeafloorHeight(x, z);
        double depth = seaLevel - floor;

        // nur wenn wir genügend Wassersäule haben
        if (depth < 6) {
            return false;
        }

        double field = underwaterCaveField(x, z);
        if (field < 0.65) {
            return false;
        }

        int minCaveY = floor + 2;          // etwas über Meeresboden
        int maxCaveY = seaLevel - 3;       // knapp unterhalb der Oberfläche

        if (y < minCaveY || y > maxCaveY) {
            return false;
        }

        // Tunnelhöhe: elliptischer Querschnitt um centerY
        int centerY = (minCaveY + maxCaveY) / 2;
        int dy = y - centerY;

        double radius = 2.5 + field * 1.5; // 2.5–4.0 Blöcke

        return (dy * dy) <= radius * radius;
    }

    /**
     * 0..1 Maske, wie "cliff-artig" die Säule zwischen Terrain und Meeresboden ist.
     * Hoch = guter Kandidat für Erosion / Caves an der Wand.
     */
    public double cliffWallMask(int x, int z) {
        int terrain = sampleBaseHeight(x, z);
        if (terrain < 0) return 0.0;

        int floor = sampleSeafloorHeight(x, z);
        int heightDiff = terrain - floor;

        // Nur "echte" Kliffs: ausreichender Höhenunterschied
        if (heightDiff < 8) return 0.0;

        double dist = Math.sqrt(x * x + z * z);

        // Fokus auf Küstenbereich
        double inner = islandRadiusBlocks * 0.75;
        double outer = islandRadiusBlocks * 1.05;
        if (dist < inner || dist > outer) {
            return 0.0;
        }

        double t = (dist - inner) / Math.max(1.0, (outer - inner)); // 0..1
        double coastBand = 1.0 - Math.abs(t - 0.5) * 2.0;           // 0..1..0

        // Steilheit (je höher die Wand, desto stärker)
        double steepness = Math.min(1.0, (heightDiff - 8) / 24.0);

        // Struktur-Noise
        double noise = (simplex.noise2(x * 0.04, z * 0.04) + 1.0) * 0.5;

        return Math.max(0.0, coastBand * steepness * (0.35 + noise * 0.65));
    }

    /**
     * Heuristik, ob an (x,y,z) eine Höhle / ein Riss in der Kliffwand verlaufen soll.
     * Nutzt Cliff-Maske + vertikalen Verlauf.
     */
    public boolean isCliffCavity(int x, int y, int z) {
        int terrain = sampleBaseHeight(x, z);
        if (terrain < 0) return false;

        int floor = sampleSeafloorHeight(x, z);
        if (y <= floor + 2 || y >= terrain - 2) {
            return false; // oben/unten geschlossen lassen
        }

        double wall = cliffWallMask(x, z);
        if (wall < 0.35) return false;

        // vertikale Position in der Wand (0 = Boden, 1 = Oberkante)
        double rel = (double) (y - floor) / Math.max(1.0, terrain - floor);
        // Cave-Band ungefähr im mittleren Drittel der Wand
        double band = 1.0 - Math.abs(rel - 0.5) * 2.0; // Peak bei 0.5

        // lokaler Noise für Riss-Struktur
        double n = (simplex.noise2(x * 0.11, (z + y * 17) * 0.11) + 1.0) * 0.5;

        double value = wall * band * n;
        return value > 0.65;
    }

    /**
     * Heuristik, ob an (x,y,z) eine Felsnase / Spike entstehen soll,
     * die aus der Wand bzw. vom Grund herausragt.
     */
    public boolean isCliffSpike(int x, int y, int z) {
        int floor = sampleSeafloorHeight(x, z);
        double depth = seaLevel - y;

        // Spikes nur unter Wasser, nicht direkt an der Oberfläche
        if (y <= floor + 1 || depth < 3 || depth > 18) {
            return false;
        }

        double wall = cliffWallMask(x, z);
        if (wall < 0.3) return false;

        // Shelf bevorzugen, damit Spikes v. a. an Unterwasser-Terrassen entstehen
        double shelf = shelfMask(x, z);
        if (shelf < 0.25) return false;

        // "streifenartige" Noise-Struktur entlang der Wand
        double n = (simplex.noise2((x + 819) * 0.09, (z + y * 13) * 0.09) + 1.0) * 0.5;

        double score = wall * (0.4 + 0.6 * shelf) * n;
        return score > 0.78;
    }
}
