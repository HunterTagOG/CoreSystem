package dev.huntertagog.coresystem.client.net;

import dev.huntertagog.coresystem.client.region.RegionImageClientCache;
import dev.huntertagog.coresystem.client.region.RegionImageClientMapper;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageRemoveS2CPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSetS2CPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSyncRequestC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RegionImageNetClient {

    private RegionImageNetClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                RegionImageSetS2CPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    var def = RegionImageClientMapper.fromPayload(payload);
                    RegionImageClientCache.upsert(def);
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(RegionImageRemoveS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> RegionImageClientCache.remove(payload.regionId()));
        });

        // Initial Sync beim Join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.world == null) return;
            sender.sendPacket(new RegionImageSyncRequestC2SPayload(client.world.getRegistryKey().getValue()));
        });

        // Optional: Clear beim Leave
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RegionImageClientCache.clear());
    }
}
