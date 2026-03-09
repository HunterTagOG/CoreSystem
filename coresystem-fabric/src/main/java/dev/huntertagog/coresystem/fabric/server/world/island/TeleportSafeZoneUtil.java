package dev.huntertagog.coresystem.fabric.server.world.island;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class TeleportSafeZoneUtil {

    private static final Logger LOG = LoggerFactory.get("TeleportSafeZoneUtil");

    private static final int PLATFORM_RADIUS = 3; // Radius in Blöcken
    private static final int CLEAR_ABOVE = 4;     // wie viele Blöcke über der Plattform frei geräumt werden
    private static final int CLEAR_BELOW = 1;     // minimal etwas unter der Oberfläche stabilisieren

    private TeleportSafeZoneUtil() {
    }

    /**
     * Erzeugt eine runde, teleport-sichere Plattform nahe Inselzentrum
     * und setzt den World-Spawn exakt auf diese Position.
     * Muss auf dem Serverthread laufen.
     */
    public static void createTeleportSafeZone(ServerWorld world,
                                              IslandHeightmap heightmap,
                                              IslandStyle style,
                                              int seaLevel) {

        if (world == null || heightmap == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "TeleportSafeZoneUtil.createTeleportSafeZone called with null world or heightmap"
                    )
                    .withContextEntry("worldNull", String.valueOf(world == null))
                    .withContextEntry("heightmapNull", String.valueOf(heightmap == null))
                    .withContextEntry("seaLevel", seaLevel);
            error.log();

            LOG.error("[TeleportSafeZone] Aborting safe zone creation: world or heightmap is null.");
            return;
        }

        // 1) Bestes Zentrum anhand Heightmap suchen
        BlockPos center;
        try {
            center = findBestTeleportCenter(heightmap, seaLevel);
        } catch (Throwable t) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "TeleportSafeZoneUtil.findBestTeleportCenter threw an exception"
                    )
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("radius", heightmap.radius())
                    .withContextEntry("exception", t.toString());
            error.log();

            LOG.error("[TeleportSafeZone] findBestTeleportCenter failed, falling back to (0, {}, 0).", seaLevel + 4, t);
            center = new BlockPos(0, seaLevel + 4, 0);
        }

        int centerX = center.getX();
        int centerZ = center.getZ();
        int platformY = center.getY();

        LOG.info("[TeleportSafeZone] Creating teleport safe zone in world {} at {} (terrainY={}, seaLevel={})",
                world.getRegistryKey().getValue(),
                center,
                platformY,
                seaLevel
        );

        // Surface-Block aus Style verwenden, Fallback: Gras
        BlockState platformBlock = Blocks.GRASS_BLOCK.getDefaultState();
        try {
            if (style != null && style.groundProfile() != null && style.groundProfile().surface() != null) {
                platformBlock = style.groundProfile().surface();
            } else {
                if (style == null || style.groundProfile() == null) {
                    CoreError error = CoreError.of(
                                    CoreErrorCode.MESSAGE_ERROR,
                                    CoreErrorSeverity.WARN,
                                    "TeleportSafeZoneUtil.createTeleportSafeZone using fallback surface block"
                            )
                            .withContextEntry("styleNull", String.valueOf(style == null))
                            .withContextEntry("seaLevel", seaLevel)
                            .withContextEntry("center", center.toShortString());
                    error.log();

                    LOG.warn("[TeleportSafeZone] style or groundProfile is null – using GRASS_BLOCK as platform surface.");
                }
            }
        } catch (Throwable t) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "TeleportSafeZoneUtil.createTeleportSafeZone failed to resolve platform surface block"
                    )
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("center", center.toShortString())
                    .withContextEntry("exception", t.toString());
            error.log();

            LOG.warn("[TeleportSafeZone] Exception while resolving platform surface – using GRASS_BLOCK.", t);
            platformBlock = Blocks.GRASS_BLOCK.getDefaultState();
        }

        // 2) Zylinderbereich vorbereiten: runde Plattform + Luftraum
        int radius = PLATFORM_RADIUS;
        int radiusSq = radius * radius;

        int minY = Math.max(world.getBottomY(), platformY - CLEAR_BELOW);
        int maxY = platformY + CLEAR_ABOVE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                // Kreis-Maske: nur innerhalb des Radius
                int distSq = dx * dx + dz * dz;
                if (distSq > radiusSq) {
                    continue;
                }

                int blockX = centerX + dx;
                int blockZ = centerZ + dz;

                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(blockX, y, blockZ);

                    if (y < platformY) {
                        // Stabilisierung unter der Plattform
                        world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                    } else if (y == platformY) {
                        // Plattformoberfläche
                        world.setBlockState(pos, platformBlock, Block.NOTIFY_LISTENERS);
                    } else {
                        // Luftraum darüber
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        // 3) Leichte „Rundung“ der Kante
        int innerRadiusSq = (radius - 1) * (radius - 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= innerRadiusSq || distSq > radiusSq) {
                    continue;
                }

                int blockX = centerX + dx;
                int blockZ = centerZ + dz;

                BlockPos edgePos = new BlockPos(blockX, platformY, blockZ);
                if (world.getRandom().nextFloat() < 0.3f) {
                    world.setBlockState(edgePos, Blocks.COBBLESTONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }

        // 4) Spawn exakt in der Mitte der Plattform setzen
        world.setSpawnPos(center, 0.0F);

        LOG.info("[TeleportSafeZone] World spawn for {} set to {}",
                world.getRegistryKey().getValue(),
                center
        );

        LOG.info("[TeleportSafeZone] Teleport safe zone creation completed. Spawn set to {}",
                world.getSpawnPos()
        );
    }

    /**
     * Sucht in einem kleinen Radius um (0,0) eine sinnvolle, möglichst flache Position.
     */
    private static BlockPos findBestTeleportCenter(IslandHeightmap heightmap, int seaLevel) {
        if (heightmap == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "TeleportSafeZoneUtil.findBestTeleportCenter called with null heightmap"
                    )
                    .withContextEntry("seaLevel", seaLevel);
            error.log();

            return new BlockPos(0, seaLevel + 4, 0);
        }

        int bestScore = Integer.MIN_VALUE;
        BlockPos bestPos = new BlockPos(0, seaLevel + 4, 0);

        int searchRadius = Math.min(32, heightmap.radius() - 8); // nicht bis ganz zum Rand
        if (searchRadius <= 0) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "TeleportSafeZoneUtil.findBestTeleportCenter got non-positive searchRadius"
                    )
                    .withContextEntry("heightmapRadius", heightmap.radius())
                    .withContextEntry("seaLevel", seaLevel);
            error.log();

            LOG.warn("[TeleportSafeZone] searchRadius <= 0 (radius={}) – using fallback (0, {}, 0).",
                    heightmap.radius(), seaLevel + 4);
            return bestPos;
        }

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {

                int h = heightmap.sampleHeight(x, z);
                if (h < seaLevel) continue; // keine Unterwasserplattform

                // Check 3x3 Plateau
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                boolean allLand = true;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int hh = heightmap.sampleHeight(x + dx, z + dz);
                        if (hh < 0) {
                            allLand = false;
                            break;
                        }
                        minY = Math.min(minY, hh);
                        maxY = Math.max(maxY, hh);
                    }
                    if (!allLand) break;
                }

                if (!allLand) continue;

                int variation = maxY - minY;
                if (variation > 2) continue; // zu uneben

                // Score: Nähe zu (0,0) + leichte Höhenpräferenz
                int distPenalty = (int) Math.round(Math.sqrt(x * x + z * z));
                int heightBonus = (h - seaLevel); // leicht höher als Meer bevorzugen
                int score = heightBonus * 4 - distPenalty;

                if (score > bestScore) {
                    bestScore = score;
                    bestPos = new BlockPos(x, h, z);
                }
            }
        }

        if (bestScore == Integer.MIN_VALUE) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "TeleportSafeZoneUtil.findBestTeleportCenter found no valid plateau, using fallback"
                    )
                    .withContextEntry("seaLevel", seaLevel)
                    .withContextEntry("radius", heightmap.radius())
                    .withContextEntry("searchRadius", searchRadius);
            error.log();

            LOG.warn("[TeleportSafeZone] No suitable plateau found – using fallback (0, {}, 0).", seaLevel + 4);
            return new BlockPos(0, seaLevel + 4, 0);
        }

        return bestPos;
    }
}
