package dev.huntertagog.coresystem.fabric.server.player;

import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FabricPlayerProfileLifecycle {

    private static PlayerProfileService profiles = null;
    private static String serverName = "";
    private static String nodeId = "";

    public FabricPlayerProfileLifecycle(PlayerProfileService profiles, String serverName) {
        FabricPlayerProfileLifecycle.profiles = profiles;
        FabricPlayerProfileLifecycle.serverName = serverName;
        nodeId = System.getenv("SERVER_ID");
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            profiles.updateOnJoin(
                    p.getUuid(),
                    p.getGameProfile().getName(),
                    serverName,
                    nodeId
            );
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity p = handler.player;
            profiles.updateOnQuit(
                    p.getUuid(),
                    serverName,
                    nodeId
            );
        });
    }
}
