package dev.huntertagog.coresystem.fabric.server.region.visual;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlineService;
import dev.huntertagog.coresystem.fabric.server.region.RegionDefinition;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class NetworkRegionOutlineService implements RegionOutlineService {

    private static final Logger LOG = LoggerFactory.get("RegionOutline");

    // halbtransparentes hellgrün (ARGB)
    private static final int DEFAULT_COLOR = 0x80_00FF00;

    public NetworkRegionOutlineService() {
        LOG.info("NetworkRegionOutlineService initialized (client-side region outlines).");
    }

    @Override
    public void showSelection(ServerPlayerEntity player,
                              Identifier worldId,
                              BlockPos min,
                              BlockPos max,
                              int ticks) {
        RegionOutlinePackets.sendOutline(player, worldId, min, max, DEFAULT_COLOR, ticks);
    }

    @Override
    public void showRegion(ServerPlayerEntity player,
                           RegionDefinition region,
                           int ticks) {
        BlockPos min = new BlockPos(region.minX(), region.minY(), region.minZ());
        BlockPos max = new BlockPos(region.maxX(), region.maxY(), region.maxZ());
        RegionOutlinePackets.sendOutline(player, region.worldId(), min, max, DEFAULT_COLOR, ticks);
    }

    @Override
    public void tick(ServerWorld world) {
        // bei Netzwerk-Variante keine Server-Logik notwendig
    }
}
