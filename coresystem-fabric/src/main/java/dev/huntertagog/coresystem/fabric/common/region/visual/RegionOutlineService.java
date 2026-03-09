package dev.huntertagog.coresystem.fabric.common.region.visual;

import dev.huntertagog.coresystem.fabric.server.region.RegionDefinition;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface RegionOutlineService extends Service {

    void showSelection(ServerPlayerEntity player,
                       Identifier worldId,
                       BlockPos min,
                       BlockPos max,
                       int ticks);

    void showRegion(ServerPlayerEntity player,
                    RegionDefinition region,
                    int ticks);

    /**
     * Bei der Netzwerk-Lösung no-op, aber fürs Interface konsistent.
     */
    void tick(ServerWorld world);
}
