package dev.huntertagog.coresystem.fabric.common.net.payload;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.model.ServerTarget;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record OpenMenuPayload(
        boolean adminMode,
        List<ServerTarget> targets
) implements CustomPayload {

    private static final Logger LOG = LoggerFactory.get("OpenMenuPayload");

    public static final Identifier ID_OPEN_MENU =
            Identifier.of("coresystem", "serverswitch/open_menu");
    public static final CustomPayload.Id<OpenMenuPayload> ID =
            new CustomPayload.Id<>(ID_OPEN_MENU);

    // B = RegistryByteBuf, T = OpenMenuPayload
    public static final PacketCodec<RegistryByteBuf, OpenMenuPayload> CODEC =
            CustomPayload.codecOf(
                    OpenMenuPayload::encode,
                    OpenMenuPayload::decode
            );

    // >>> value first: (T value, B buf) – so wie du es aktuell nutzt
    private static void encode(OpenMenuPayload payload, RegistryByteBuf buf) {
        List<ServerTarget> list = payload.targets();

        if (list == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                    CoreErrorSeverity.WARN,
                    "OpenMenuPayload.targets is null; encoding as empty list."
            ).withContextEntry("payloadAdminMode", payload.adminMode());

            LOG.warn(error.toLogString());
            list = Collections.emptyList();
        }

        buf.writeBoolean(payload.adminMode());
        buf.writeVarInt(list.size());

        for (ServerTarget t : list) {
            if (t == null) {
                CoreError error = CoreError.of(
                        CoreErrorCode.NETWORK_PAYLOAD_ENCODE_FAILED,
                        CoreErrorSeverity.WARN,
                        "Null ServerTarget entry in OpenMenuPayload; skipping."
                );
                LOG.warn(error.toLogString());
                continue;
            }

            buf.writeString(t.id());
            buf.writeString(t.displayName());
            buf.writeString(t.commandTarget());
            buf.writeEnumConstant(t.category());
        }
    }

    // decoder: (buf) -> T
    private static OpenMenuPayload decode(RegistryByteBuf buf) {
        try {
            boolean admin = buf.readBoolean();
            int size = buf.readVarInt();

            if (size < 0) {
                CoreError error = CoreError.of(
                        CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Negative targets size in OpenMenuPayload."
                ).withContextEntry("size", size);

                LOG.error(error.toLogString());
                throw new IllegalStateException(error.toLogString());
            }

            List<ServerTarget> list = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                String id = buf.readString();
                String displayName = buf.readString();
                String commandTarget = buf.readString();
                ServerTarget.Category category =
                        buf.readEnumConstant(ServerTarget.Category.class);

                list.add(new ServerTarget(id, displayName, commandTarget, category));
            }

            // Immutability nach außen
            return new OpenMenuPayload(admin, List.copyOf(list));
        } catch (Exception e) {
            CoreError error = CoreError.of(
                    CoreErrorCode.NETWORK_PAYLOAD_DECODE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to decode OpenMenuPayload."
            ).withCause(e);

            LOG.error(error.toLogString(), e);
            // Hart abbrechen, damit die Connection sauber gefailt wird
            throw new IllegalStateException(error.toLogString(), e);
        }
    }

    @Override
    public Id<OpenMenuPayload> getId() {
        return ID;
    }
}
