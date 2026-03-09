package dev.huntertagog.coresystem.client.region;

import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlinePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RegionOutlineClientNetworking {

    private RegionOutlineClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                RegionOutlinePayload.TYPE,
                (payload, context) -> {
                    ClientRegionOutlineManager.getInstance().addOutline(
                            payload.worldId(),
                            payload.min(),
                            payload.max(),
                            payload.color(),
                            payload.durationTicks()
                    );
                }
        );
    }
}
