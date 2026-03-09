package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultRegionService implements RegionService {

    private static final Logger LOG = LoggerFactory.get("Regions");

    // worldId -> regions
    private final Map<Identifier, List<RegionDefinition>> regionsByWorld = new ConcurrentHashMap<>();
    private final Map<String, RegionDefinition> byId = new ConcurrentHashMap<>();

    @Override
    public void registerRegion(@NotNull RegionDefinition region) {
        byId.put(region.id(), region);
        regionsByWorld.compute(region.worldId(), (id, list) -> {
            List<RegionDefinition> newList = list != null ? new ArrayList<>(list) : new ArrayList<>();
            newList.removeIf(r -> r.id().equals(region.id()));
            newList.add(region);
            return List.copyOf(newList);
        });

        LOG.info("Registered region: {}", region);
    }

    @Override
    public void removeRegion(@NotNull String id) {
        RegionDefinition removed = byId.remove(id);
        if (removed == null) {
            return;
        }
        regionsByWorld.computeIfPresent(removed.worldId(), (w, list) -> {
            List<RegionDefinition> filtered = list.stream()
                    .filter(r -> !r.id().equals(id))
                    .toList();
            return List.copyOf(filtered);
        });
        LOG.info("Removed region: {}", id);
    }

    @Override
    public Optional<RegionDefinition> findById(@NotNull String id) {
        if (id.isBlank()) return Optional.empty();

        // Beispiel: wenn du Map nutzt
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public @NotNull List<RegionDefinition> findRegionsAt(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        List<RegionDefinition> list = regionsByWorld.get(world.getRegistryKey().getValue());
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        List<RegionDefinition> result = new ArrayList<>();
        for (RegionDefinition r : list) {
            if (r.contains(world, pos)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public boolean hasFlagAt(@NotNull ServerWorld world,
                             @NotNull BlockPos pos,
                             @NotNull RegionFlag flag) {

        List<RegionDefinition> list = findRegionsAt(world, pos);
        if (list.isEmpty()) return false;

        for (RegionDefinition r : list) {
            if (r.hasFlag(flag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<RegionDefinition> getAllRegions() {
        return new ArrayList<>(byId.values());
    }
}
