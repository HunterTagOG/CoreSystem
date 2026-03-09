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

/**
 * Leerer Anfrage-Payload:
 * client → server : "Bitte sende mir die Liste der privaten Inseln"
 * <p>
 * Wird serverseitig beantwortet mit {@link PrivateIslandListPayload}.
 */
public record RequestPrivateIslandListPayload() implements CustomPayload {

    private static final Logger LOG = LoggerFactory.get("ReqPrivateIslandList");

    public static final Id<RequestPrivateIslandListPayload> ID =
            new Id<>(Identifier.of("coresystem", "serverswitch/request_private_island_list"));

    public static final PacketCodec<RegistryByteBuf, RequestPrivateIslandListPayload> CODEC =
            CustomPayload.codecOf(
                    RequestPrivateIslandListPayload::encode,
                    RequestPrivateIslandListPayload::decode
            );

    // ---------------------------------------------------------------------
    // ENCODE
    // ---------------------------------------------------------------------
    private static void encode(RequestPrivateIslandListPayload payload, RegistryByteBuf buf) {
        try {
            // Payload ist leer → noop
        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to encode RequestPrivateIslandListPayload."
            ).withCause(e);

            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    // ---------------------------------------------------------------------
    // DECODE
    // ---------------------------------------------------------------------
    private static RequestPrivateIslandListPayload decode(RegistryByteBuf buf) {
        try {
            // keine Daten → Direkt neue Instanz
            return new RequestPrivateIslandListPayload();

        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to decode RequestPrivateIslandListPayload."
            ).withCause(e);

            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
