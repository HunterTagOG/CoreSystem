package dev.huntertagog.coresystem.fabric.common.region;

import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.fabric.server.region.RegionDefinition;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface RegionService extends Service {

    void registerRegion(@NotNull RegionDefinition region);

    void removeRegion(@NotNull String id);

    Optional<RegionDefinition> findById(@NotNull String id);

    /**
     * Alle Regionen an einer Position (für Stacking / Priorisierung).
     */
    @NotNull
    List<RegionDefinition> findRegionsAt(@NotNull ServerWorld world, @NotNull BlockPos pos);

    /**
     * Convenience: gibt true zurück, wenn mindestens eine Region am BlockPos den Flag gesetzt hat.
     */
    boolean hasFlagAt(@NotNull ServerWorld world,
                      @NotNull BlockPos pos,
                      @NotNull RegionFlag flag);

    /**
     * Alle registrierten Regionen.
     */
    List<RegionDefinition> getAllRegions();

}
