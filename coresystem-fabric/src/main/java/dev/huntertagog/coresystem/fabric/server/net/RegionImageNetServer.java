package dev.huntertagog.coresystem.fabric.server.net;

import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSyncRequestC2SPayload;
import dev.huntertagog.coresystem.fabric.server.region.image.RegionImageServerService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;

public final class RegionImageNetServer {

    private RegionImageNetServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RegionImageSyncRequestC2SPayload.ID, (payload, context) -> {
            context.player().server.execute(() -> {
                var server = context.player().server;

                RegistryKey<net.minecraft.world.World> key = RegistryKey.of(RegistryKeys.WORLD, payload.worldId());
                ServerWorld world = server.getWorld(key);
                if (world == null) return;

                // ✅ Antwort: alles aus PersistentState an genau diesen Player senden
                RegionImageServerService.syncToPlayer(world, context.player());
            });
        });
    }
}
