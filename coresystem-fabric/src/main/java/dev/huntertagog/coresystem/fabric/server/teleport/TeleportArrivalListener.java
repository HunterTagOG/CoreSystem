package dev.huntertagog.coresystem.fabric.server.teleport;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.teleport.TeleportManagerService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TeleportArrivalListener {

    private static final Logger LOG = LoggerFactory.get("TeleportArrivalListener");

    private TeleportArrivalListener() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            TeleportManagerService teleport = ServiceProvider.getService(TeleportManagerService.class);
            if (teleport == null) {
                return;
            }

            boolean handled = teleport.handleJoinOnThisServer(player);
            if (handled) {
                LOG.debug("TeleportArrival handled for {} ({})",
                        player.getGameProfile().getName(),
                        player.getUuid());
            }
        });

        LOG.info("TeleportArrivalListener registered.");
    }
}
