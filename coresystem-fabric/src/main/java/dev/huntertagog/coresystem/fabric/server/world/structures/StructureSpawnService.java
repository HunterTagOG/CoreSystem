package dev.huntertagog.coresystem.fabric.server.world.structures;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.world.structures.config.StructureSpawnConfig;
import dev.huntertagog.coresystem.common.world.structures.config.StructureSpawnConfig.Group;
import dev.huntertagog.coresystem.common.world.structures.config.StructureSpawnConfig.StructureEntry;
import dev.huntertagog.coresystem.common.world.structures.config.StructureSpawnConfigLoader;
import dev.huntertagog.coresystem.fabric.server.world.structures.state.StructureSpawnState;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.util.*;

/**
 * Steuert die Placement-Logik für konfigurierbare NBT-Strukturen:
 * - gruppenbasierter Mindestabstand
 * - Biome-Filter
 * - y_offset relativ zur Terrainhöhe
 * - Terrain-Slope-Checks
 */
public final class StructureSpawnService implements Service {

    private static final Logger LOG = LoggerFactory.get("StructureSpawnService");

    private final File configDir;
    private StructureSpawnConfig config;

    public StructureSpawnService(File configDir) {
        this.configDir = configDir;

        try {
            this.config = StructureSpawnConfigLoader.load(configDir);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Failed to load StructureSpawnConfig on startup"
                    )
                    .withContextEntry("configDir", safePath(configDir))
                    .withContextEntry("exception", e.toString());
            error.log();

            LOG.error("Failed to load structure spawn config from {} during service init.", safePath(configDir), e);
            // Re-throw, damit das Fehlverhalten sichtbar bleibt
            throw e;
        }

        ServerWorldEvents.LOAD.register(this::onWorldLoad);
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
    }

    // -----------------------------
    // Hot-Reload API
    // -----------------------------

    public void reload(MinecraftServer server) {
        StructureSpawnConfig newConfig;
        try {
            newConfig = StructureSpawnConfigLoader.load(this.configDir);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Failed to reload StructureSpawnConfig"
                    )
                    .withContextEntry("configDir", safePath(configDir))
                    .withContextEntry("exception", e.toString());
            error.log();

            LOG.error("Failed to reload structure spawn config from {}. Keeping previous config.", safePath(configDir), e);
            return;
        }

        this.config = newConfig;

        for (ServerWorld world : server.getWorlds()) {
            if (!world.getRegistryKey().equals(World.OVERWORLD)) continue;

            try {
                StructureSpawnState state = StructureSpawnState.get(world);
                state.getStructures().clear();
                state.markDirty();

                planWorldTargets(world);
            } catch (Exception e) {
                CoreError error = CoreError.of(
                                CoreErrorCode.MESSAGE_ERROR,
                                CoreErrorSeverity.WARN,
                                "Failed to re-plan structure spawn targets on reload"
                        )
                        .withContextEntry("world", world.getRegistryKey().getValue().toString())
                        .withContextEntry("exception", e.toString());
                error.log();

                LOG.warn("Error while re-planning structure targets for world {} on reload.",
                        world.getRegistryKey().getValue(), e);
            }
        }
    }

    private static String safePath(File file) {
        return file != null ? file.getPath() : "<null>";
    }

    // -----------------------------
    // World-Load: Ziele generieren
    // -----------------------------

    private void onWorldLoad(MinecraftServer server, ServerWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;

        try {
            planWorldTargets(world);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "Failed to plan structure spawn targets on world load"
                    )
                    .withContextEntry("world", world.getRegistryKey().getValue().toString())
                    .withContextEntry("exception", e.toString());
            error.log();

            LOG.warn("Error while planning structure targets for world {} on load.",
                    world.getRegistryKey().getValue(), e);
        }
    }

    private void planWorldTargets(ServerWorld world) {
        if (config == null || config.groups == null || config.groups.isEmpty()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "StructureSpawnService.planWorldTargets called with missing or empty config"
                    )
                    .withContextEntry("world", world.getRegistryKey().getValue().toString())
                    .withContextEntry("configDir", safePath(configDir));
            error.log();

            LOG.warn("Skipping structure planning for world {}: config missing or has no groups.",
                    world.getRegistryKey().getValue());
            return;
        }

        StructureSpawnState state = StructureSpawnState.get(world);
        net.minecraft.util.math.random.Random random = world.getRandom();

        for (Group group : config.groups) {
            if (group.structures == null || group.structures.isEmpty()) continue;

            int radius = Math.max(1, group.world_radius);
            int minDistance = Math.max(0, group.min_distance);
            int minDistanceSq = minDistance * minDistance;

            List<BlockPos> groupTargets = collectGroupTargets(state, group);

            for (StructureEntry entry : group.structures) {
                if (entry.id == null || entry.id.isBlank() || entry.count <= 0) continue;

                Identifier structId = Identifier.of(entry.id);
                StructureSpawnState.StructureData data = state.getOrCreate(structId);

                while (data.targets.size() < entry.count) {
                    BlockPos candidate = findValidSpawnPosition(
                            world, random, radius, entry, groupTargets, minDistanceSq
                    );

                    if (candidate == null) {
                        CoreError error = CoreError.of(
                                        CoreErrorCode.MESSAGE_ERROR,
                                        CoreErrorSeverity.WARN,
                                        "Could not find valid spawn position for structure"
                                )
                                .withContextEntry("structureId", entry.id)
                                .withContextEntry("world", world.getRegistryKey().getValue().toString())
                                .withContextEntry("radius", String.valueOf(radius))
                                .withContextEntry("minDistance", String.valueOf(minDistance))
                                .withContextEntry("requestedCount", String.valueOf(entry.count))
                                .withContextEntry("currentPlanned", String.valueOf(data.targets.size()));
                        error.log();

                        LOG.warn("No valid spawn position found for structure '{}' in world {} (planned {}/{}).",
                                entry.id, world.getRegistryKey().getValue(),
                                data.targets.size(), entry.count);
                        break;
                    }

                    data.targets.add(candidate);
                    groupTargets.add(candidate);
                    state.markDirty();
                }
            }
        }
    }

    private List<BlockPos> collectGroupTargets(StructureSpawnState state, Group group) {
        List<BlockPos> result = new ArrayList<>();
        if (group.structures == null) return result;

        for (StructureEntry entry : group.structures) {
            if (entry.id == null || entry.id.isBlank()) continue;
            Identifier structId = Identifier.of(entry.id);
            StructureSpawnState.StructureData data = state.getStructures().get(structId);
            if (data == null) continue;
            result.addAll(data.targets);
        }

        return result;
    }

    // -----------------------------
    // Chunk-Load: Platzierung
    // -----------------------------

    private void onChunkLoad(ServerWorld world, Chunk chunk) {
        StructureSpawnState state = StructureSpawnState.get(world);
        Map<Identifier, StructureSpawnState.StructureData> map = state.getStructures();

        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getStartX();
        int maxX = chunkPos.getEndX();
        int minZ = chunkPos.getStartZ();
        int maxZ = chunkPos.getEndZ();

        boolean changed = false;

        for (Map.Entry<Identifier, StructureSpawnState.StructureData> entry : map.entrySet()) {
            Identifier structId = entry.getKey();
            StructureSpawnState.StructureData data = entry.getValue();
            if (data.isFullyPlaced()) continue;

            for (int i = 0; i < data.targets.size(); i++) {
                if (data.placed.get(i)) continue;

                BlockPos target = data.targets.get(i);

                boolean isInThisChunk =
                        target.getX() >= minX && target.getX() <= maxX &&
                                target.getZ() >= minZ && target.getZ() <= maxZ;

                if (!isInThisChunk) continue;

                try {
                    if (placeStructure(world, target, structId)) {
                        data.placed.set(i);
                        changed = true;
                    }
                } catch (Exception e) {
                    CoreError error = CoreError.of(
                                    CoreErrorCode.MESSAGE_ERROR,
                                    CoreErrorSeverity.WARN,
                                    "Exception while placing structure"
                            )
                            .withContextEntry("structureId", structId.toString())
                            .withContextEntry("world", world.getRegistryKey().getValue().toString())
                            .withContextEntry("target", target.toShortString())
                            .withContextEntry("exception", e.toString());
                    error.log();

                    LOG.warn("Error while placing structure '{}' at {} in world {}.",
                            structId, target.toShortString(), world.getRegistryKey().getValue(), e);
                }
            }
        }

        if (changed) {
            state.markDirty();
        }
    }

    // -----------------------------
    // Strukturplatzierung
    // -----------------------------

    private boolean placeStructure(ServerWorld world, BlockPos origin, Identifier structureId) {
        var manager = world.getStructureTemplateManager();
        Optional<StructureTemplate> templateOpt = manager.getTemplate(structureId);

        if (templateOpt.isEmpty()) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.WARN,
                            "Structure template not found"
                    )
                    .withContextEntry("structureId", structureId.toString())
                    .withContextEntry("world", world.getRegistryKey().getValue().toString())
                    .withContextEntry("origin", origin.toShortString());
            error.log();

            LOG.warn("Structure template '{}' not found for placement in world {} at {}.",
                    structureId, world.getRegistryKey().getValue(), origin.toShortString());
            return false;
        }

        StructureTemplate template = templateOpt.get();
        net.minecraft.util.math.random.Random random = world.getRandom();

        BlockRotation rotation = BlockRotation.values()[random.nextInt(BlockRotation.values().length)];
        BlockMirror mirror = random.nextBoolean() ? BlockMirror.NONE : BlockMirror.FRONT_BACK;

        StructurePlacementData placement = new StructurePlacementData()
                .setIgnoreEntities(false)
                .setRotation(rotation)
                .setMirror(mirror);
        // .setIgnoreAirBlocks(true); // falls du später Terrain schützen willst

        return template.place(world, origin, origin, placement, random, 2);
    }

    // -----------------------------
    // Positionsermittlung & Checks
    // -----------------------------

    private BlockPos findValidSpawnPosition(ServerWorld world,
                                            net.minecraft.util.math.random.Random random,
                                            int radius,
                                            StructureEntry entry,
                                            List<BlockPos> groupTargets,
                                            int minDistanceSq) {

        for (int attempt = 0; attempt < 256; attempt++) {
            int x = MathHelper.nextInt(random, -radius, radius);
            int z = MathHelper.nextInt(random, -radius, radius);

            int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z);
            int originY = surfaceY + entry.y_offset; // zentrale y_offset-Logik

            BlockPos origin = new BlockPos(x, originY, z);

            if (!isBiomeAllowedForEntry(world, origin, entry)) continue;
            if (!isTerrainSlopeOk(world, new BlockPos(x, surfaceY, z))) continue;
            if (!isSurfaceBlockOk(world, new BlockPos(x, surfaceY - 1, z))) continue;
            if (!world.getFluidState(new BlockPos(x, surfaceY, z)).isEmpty()) continue;
            if (!isFarEnoughFromOthers(origin, groupTargets, minDistanceSq)) continue;

            return origin;
        }

        return null;
    }

    private boolean isBiomeAllowedForEntry(ServerWorld world, BlockPos pos, StructureEntry entry) {
        if (entry.biomes == null || entry.biomes.isEmpty()) {
            return true;
        }

        var biome = world.getBiome(pos).value();
        var biomeKeyOpt = world.getRegistryManager()
                .get(RegistryKeys.BIOME)
                .getKey(biome);

        if (biomeKeyOpt.isEmpty()) return false;

        Identifier biomeId = biomeKeyOpt.get().getValue();

        for (String allowed : entry.biomes) {
            if (biomeId.toString().equals(allowed)) {
                return true;
            }
        }

        return false;
    }

    private boolean isTerrainSlopeOk(ServerWorld world, BlockPos surfacePos) {
        int x = surfacePos.getX();
        int z = surfacePos.getZ();

        int yCenter = surfacePos.getY();
        int yN = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z - 4);
        int yS = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z + 4);
        int yW = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x - 4, z);
        int yE = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x + 4, z);

        int maxDelta = Math.max(
                Math.max(Math.abs(yCenter - yN), Math.abs(yCenter - yS)),
                Math.max(Math.abs(yCenter - yW), Math.abs(yCenter - yE))
        );

        return maxDelta <= 4;
    }

    private boolean isSurfaceBlockOk(ServerWorld world, BlockPos posBelowSurface) {
        var state = world.getBlockState(posBelowSurface);

        if (!state.getFluidState().isEmpty()) return false;

        var block = state.getBlock();
        String name = block.getName().getString().toLowerCase(Locale.ROOT);

        if (name.contains("leaves") || name.contains("ice")) return false;

        return true;
    }

    private boolean isFarEnoughFromOthers(BlockPos candidate, List<BlockPos> existingTargets, int minDistanceSq) {
        if (minDistanceSq <= 0) return true;

        for (BlockPos other : existingTargets) {
            if (candidate.getSquaredDistance(other) < minDistanceSq) {
                return false;
            }
        }

        return true;
    }
}
