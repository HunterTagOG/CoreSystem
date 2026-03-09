package dev.huntertagog.coresystem.fabric.common.region.visual;

import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RegionOutlinePayload(
        Identifier worldId,
        BlockPos min,
        BlockPos max,
        int color,
        int durationTicks
) implements CustomPayload {

    public static final Id<RegionOutlinePayload> TYPE =
            new Id<>(Identifier.of(CoresystemCommon.MOD_ID, "region_outline"));

    // Codec beschreibt, wie der Payload kodiert/decodiert wird.
    public static final PacketCodec<RegistryByteBuf, RegionOutlinePayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeIdentifier(payload.worldId);
                        buf.writeBlockPos(payload.min);
                        buf.writeBlockPos(payload.max);
                        buf.writeInt(payload.color);
                        buf.writeVarInt(payload.durationTicks);
                    },
                    buf -> new RegionOutlinePayload(
                            buf.readIdentifier(),
                            buf.readBlockPos(),
                            buf.readBlockPos(),
                            buf.readInt(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
