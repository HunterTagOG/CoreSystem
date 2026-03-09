package dev.huntertagog.coresystem.fabric.common.net.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RegionImageRemoveS2CPayload(String regionId, Identifier worldId) implements CustomPayload {

    public static final Id<RegionImageRemoveS2CPayload> ID =
            new Id<>(Identifier.of("coresystem", "region_image_remove_s2c"));

    public static final PacketCodec<PacketByteBuf, RegionImageRemoveS2CPayload> CODEC =
            PacketCodec.of(RegionImageRemoveS2CPayload::encode, RegionImageRemoveS2CPayload::decode);

    private static void encode(RegionImageRemoveS2CPayload p, PacketByteBuf buf) {
        buf.writeString(p.regionId());
        buf.writeIdentifier(p.worldId());
    }

    private static RegionImageRemoveS2CPayload decode(PacketByteBuf buf) {
        return new RegionImageRemoveS2CPayload(buf.readString(), buf.readIdentifier());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
