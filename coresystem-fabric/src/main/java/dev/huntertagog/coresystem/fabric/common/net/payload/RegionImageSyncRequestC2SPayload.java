package dev.huntertagog.coresystem.fabric.common.net.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RegionImageSyncRequestC2SPayload(
        Identifier worldId
) implements CustomPayload {

    public static final Identifier RAW_ID = Identifier.of("coresystem", "region_image_sync_request_c2s");
    public static final CustomPayload.Id<RegionImageSyncRequestC2SPayload> ID = new CustomPayload.Id<>(RAW_ID);

    public static final PacketCodec<PacketByteBuf, RegionImageSyncRequestC2SPayload> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, RegionImageSyncRequestC2SPayload::worldId,
                    RegionImageSyncRequestC2SPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
