package dev.huntertagog.coresystem.fabric.common.net.payload;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SelectServerPayload(String target) implements CustomPayload {

    private static final Logger LOG = LoggerFactory.get("SelectServerPayload");

    public static final Identifier ID_SELECT_SERVER =
            Identifier.of("coresystem", "serverswitch/select");
    public static final CustomPayload.Id<SelectServerPayload> ID =
            new CustomPayload.Id<>(ID_SELECT_SERVER);

    public static final PacketCodec<RegistryByteBuf, SelectServerPayload> CODEC =
            CustomPayload.codecOf(
                    SelectServerPayload::encode,
                    SelectServerPayload::decode
            );

    // ---------------------------------------------------------------------
    // ENCODE
    // ---------------------------------------------------------------------
    private static void encode(SelectServerPayload payload, RegistryByteBuf buf) {
        try {
            buf.writeString(payload.target());
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to encode SelectServerPayload."
                    )
                    .withContextEntry("target", String.valueOf(payload.target()))
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    // ---------------------------------------------------------------------
    // DECODE
    // ---------------------------------------------------------------------
    private static SelectServerPayload decode(RegistryByteBuf buf) {
        try {
            String target = buf.readString();
            return new SelectServerPayload(target);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to decode SelectServerPayload."
            ).withCause(e);

            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    @Override
    public Id<SelectServerPayload> getId() {
        return ID;
    }
}
