package dev.huntertagog.coresystem.fabric.server.region.visual;

import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlinePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class RegionOutlinePackets {

    private RegionOutlinePackets() {
    }

    public static void sendOutline(ServerPlayerEntity player,
                                   Identifier worldId,
                                   BlockPos min,
                                   BlockPos max,
                                   int colorArgb,
                                   int durationTicks) {

        RegionOutlinePayload payload = new RegionOutlinePayload(
                worldId, min, max, colorArgb, durationTicks
        );

        // NEUES API (1.20.5+)
        ServerPlayNetworking.send(player, payload);
    }
}
