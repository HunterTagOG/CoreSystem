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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record PrivateIslandListPayload(@NotNull List<UUID> uuids)
        implements CustomPayload {

    private static final Logger LOG = LoggerFactory.get("PrivateIslandListPayload");

    public static final Identifier ID_Private_Island_List =
            Identifier.of("coresystem", "serverswitch/private_island_list");
    public static final CustomPayload.Id<PrivateIslandListPayload> ID =
            new CustomPayload.Id<>(ID_Private_Island_List);

    public static final PacketCodec<RegistryByteBuf, PrivateIslandListPayload> CODEC =
            CustomPayload.codecOf(
                    PrivateIslandListPayload::encode,
                    PrivateIslandListPayload::decode
            );

    private static void encode(
            @NotNull PrivateIslandListPayload payload,
            @NotNull RegistryByteBuf buf
    ) {
        List<UUID> source = payload.uuids();
        if (source == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                    CoreErrorSeverity.WARN,
                    "PrivateIslandListPayload.uuids is null; encoding as empty list."
            );
            LOG.warn(error.toLogString());
            source = Collections.emptyList();
        }

        // Defensiv: null-Entries überspringen
        List<UUID> effective = new ArrayList<>(source.size());
        for (UUID uuid : source) {
            if (uuid == null) {
                CoreError error = CoreError.of(
                        CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                        CoreErrorSeverity.WARN,
                        "Null UUID entry in PrivateIslandListPayload; skipping."
                );
                LOG.warn(error.toLogString());
                continue;
            }
            effective.add(uuid);
        }

        buf.writeVarInt(effective.size());
        for (UUID uuid : effective) {
            buf.writeUuid(uuid);
        }
    }

    @NotNull
    private static PrivateIslandListPayload decode(
            @NotNull RegistryByteBuf buf
    ) {
        try {
            int size = buf.readVarInt();
            if (size < 0) {
                CoreError error = CoreError.of(
                        CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Negative uuid list size in PrivateIslandListPayload."
                ).withContextEntry("size", size);

                LOG.error(error.toLogString());
                throw new IllegalStateException(error.toLogString());
            }

            List<UUID> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(buf.readUuid());
            }

            return new PrivateIslandListPayload(List.copyOf(list));
        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to decode PrivateIslandListPayload."
            ).withCause(e);

            LOG.error(error.toLogString(), e);
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    @Override
    public Id<PrivateIslandListPayload> getId() {
        return ID;
    }
}
