package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.region.RegionDefinitionDto;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class WorldPersistentRegionService implements RegionService {

    private static final Logger LOG = LoggerFactory.get("Regions-WorldState");

    private final DefaultRegionService delegate = new DefaultRegionService();

    private final MinecraftServer server;

    public WorldPersistentRegionService(MinecraftServer server) {
        this.server = server;
        for (ServerWorld world : this.server.getWorlds()) {
            loadForWorld(world);
        }
    }

    public void loadForWorld(ServerWorld world) {
        try {
            RegionPersistentState state = RegionPersistentState.get(world);
            int count = 0;

            for (RegionDefinitionDto dto : state.all()) {
                // mapper nutzt bei dir Identifier im Domain-Objekt
                RegionDefinition region =
                        RegionDefinitionMapper.fromDto(dto);

                delegate.registerRegion(region);
                count++;
            }

            LOG.info("Loaded {} region(s) for world={}", count, world.getRegistryKey().getValue());
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.REGION_WORLDSTATE_LOAD_FAILED, // neuen Code anlegen
                            CoreErrorSeverity.WARN,
                            "Failed to load regions from PersistentState"
                    )
                    .withCause(e)
                    .withContextEntry("world", world.getRegistryKey().getValue().toString())
                    .log();
        }
    }

    @Override
    public void registerRegion(@NotNull RegionDefinition region) {
        delegate.registerRegion(region);

        ServerWorld world = resolveWorld(region.worldId());
        if (world == null) return;

        RegionPersistentState state = RegionPersistentState.get(world);
        RegionDefinitionDto dto =
                RegionDefinitionMapper.toDto(region);

        state.put(dto);
    }

    @Override
    public void removeRegion(@NotNull String id) {
        Optional<RegionDefinition> existing = delegate.findById(id);
        delegate.removeRegion(id);

        existing.ifPresent(region -> {
            ServerWorld world = resolveWorld(region.worldId());
            if (world == null) return;
            RegionPersistentState.get(world).remove(id);
        });
    }

    @Override
    public Optional<RegionDefinition> findById(@NotNull String id) {
        return delegate.findById(id);
    }

    @Override
    public @NotNull List<RegionDefinition> findRegionsAt(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        return delegate.findRegionsAt(world, pos);
    }

    @Override
    public boolean hasFlagAt(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull RegionFlag flag) {
        return delegate.hasFlagAt(world, pos, flag);
    }

    @Override
    public List<RegionDefinition> getAllRegions() {
        return delegate.getAllRegions();
    }

    private ServerWorld resolveWorld(Identifier worldId) {
        var key = RegistryKey.of(RegistryKeys.WORLD, worldId);
        return server.getWorld(key);
    }
}
