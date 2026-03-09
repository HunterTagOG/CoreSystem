package dev.huntertagog.coresystem.fabric.common.net.payload;

import dev.huntertagog.coresystem.fabric.server.bridge.BridgeChannel;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record BridgePayload(byte[] bytes) implements CustomPayload {

    public static final Id<BridgePayload> ID =
            new Id<>(BridgeChannel.ID);

    public static final PacketCodec<RegistryByteBuf, BridgePayload> CODEC =
            PacketCodec.of(
                    // ✅ ENCODER: (value, buf)
                    (BridgePayload payload, RegistryByteBuf buf) -> {
                        byte[] data = payload.bytes();
                        buf.writeVarInt(data.length);
                        buf.writeBytes(data);
                    },
                    // ✅ DECODER: (buf) -> value
                    (RegistryByteBuf buf) -> {
                        int len = buf.readVarInt();
                        byte[] data = new byte[len];
                        buf.readBytes(data);
                        return new BridgePayload(data);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
