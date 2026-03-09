package dev.huntertagog.coresystem.fabric.common.net.payload;

import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RegionImageSetS2CPayload(
        String regionId,
        Identifier worldId,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        String mode,
        String facing,
        int baseY,
        String textureId
) implements CustomPayload {

    public static final Id<RegionImageSetS2CPayload> ID =
            new Id<>(Identifier.of("coresystem", "region_image_set_s2c"));

    public static final PacketCodec<PacketByteBuf, RegionImageSetS2CPayload> CODEC =
            PacketCodec.of(RegionImageSetS2CPayload::encode, RegionImageSetS2CPayload::decode);

    // ✅ Encoder: (value, buf)
    private static void encode(RegionImageSetS2CPayload p, PacketByteBuf buf) {
        buf.writeString(p.regionId());
        buf.writeIdentifier(p.worldId());

        buf.writeDouble(p.minX());
        buf.writeDouble(p.minY());
        buf.writeDouble(p.minZ());
        buf.writeDouble(p.maxX());
        buf.writeDouble(p.maxY());
        buf.writeDouble(p.maxZ());

        buf.writeString(p.mode());
        buf.writeString(p.facing() == null ? "" : p.facing());
        buf.writeInt(p.baseY());

        buf.writeString(p.textureId());
    }

    // ✅ Decoder: (buf) -> value
    private static RegionImageSetS2CPayload decode(PacketByteBuf buf) {
        String regionId = buf.readString();
        Identifier worldId = buf.readIdentifier();

        double minX = buf.readDouble(), minY = buf.readDouble(), minZ = buf.readDouble();
        double maxX = buf.readDouble(), maxY = buf.readDouble(), maxZ = buf.readDouble();

        String mode = buf.readString();
        String facing = buf.readString();
        int baseY = buf.readInt();

        String textureId = buf.readString();

        return new RegionImageSetS2CPayload(
                regionId, worldId,
                minX, minY, minZ, maxX, maxY, maxZ,
                mode, facing, baseY, textureId
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static RegionImageSetS2CPayload fromDef(RegionImageDef def) {
        return new RegionImageSetS2CPayload(
                def.regionId(),
                def.worldId(),
                def.bounds().minX,
                def.bounds().minY,
                def.bounds().minZ,
                def.bounds().maxX,
                def.bounds().maxY,
                def.bounds().maxZ,
                def.mode().name(),
                def.facing().name(),
                def.baseY(),
                def.textureId()
        );
    }
}
