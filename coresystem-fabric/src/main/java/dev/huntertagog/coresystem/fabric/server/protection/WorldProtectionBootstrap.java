package dev.huntertagog.coresystem.fabric.server.protection;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public final class WorldProtectionBootstrap {

    private static final Logger LOG = LoggerFactory.get("WorldProtectionBootstrap");

    private WorldProtectionBootstrap() {
    }

    public static void register(MinecraftServer server) {
        WorldProtectionService protection = ServiceProvider.getService(WorldProtectionService.class);
        if (protection == null) {
            LOG.warn("WorldProtectionService not available – world protection disabled.");
            return;
        }

        ServerWorldEvents.LOAD.register((MinecraftServer srv, ServerWorld world) -> {
            protection.applyOnWorldLoad(world);
        });

        LOG.info("WorldProtectionBootstrap wired to ServerWorldEvents.LOAD");
    }
}
