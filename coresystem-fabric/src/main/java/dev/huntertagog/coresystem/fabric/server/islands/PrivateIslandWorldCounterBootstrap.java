package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;

public final class PrivateIslandWorldCounterBootstrap {

    private static final Logger LOG = LoggerFactory.get("IslandCounter");

    private PrivateIslandWorldCounterBootstrap() {
    }

    public static void register() {
        ServerWorldEvents.LOAD.register(PrivateIslandWorldCounterBootstrap::onWorldLoad);
        ServerWorldEvents.UNLOAD.register(PrivateIslandWorldCounterBootstrap::onWorldUnload);
    }

    // ---------------------------------------------------------
    // LOAD EVENT
    // ---------------------------------------------------------
    private static void onWorldLoad(MinecraftServer server, ServerWorld world) {
        try {
            PrivateIslandWorldManager manager = PrivateIslandWorldManager.get(server);
            if (manager.isIslandWorld(world)) {
                manager.incrementIslandWorldCount();
            }

        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_COUNTER_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to process world-load event for island counter.",
                    e,
                    Map.of(
                            "world", world != null ? world.getRegistryKey().getValue().toString() : "null",
                            "event", "load"
                    )
            );

            LOG.error(error.toLogString(), e);
        }
    }

    // ---------------------------------------------------------
    // UNLOAD EVENT
    // ---------------------------------------------------------
    private static void onWorldUnload(MinecraftServer server, ServerWorld world) {
        try {
            PrivateIslandWorldManager manager = PrivateIslandWorldManager.get(server);
            if (manager.isIslandWorld(world)) {
                manager.decrementIslandWorldCount();
            }

        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_COUNTER_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to process world-unload event for island counter.",
                    e,
                    Map.of(
                            "world", world != null ? world.getRegistryKey().getValue().toString() : "null",
                            "event", "unload"
                    )
            );

            LOG.error(error.toLogString(), e);
        }
    }
}
