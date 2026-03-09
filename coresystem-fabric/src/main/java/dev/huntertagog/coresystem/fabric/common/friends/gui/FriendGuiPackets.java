package dev.huntertagog.coresystem.fabric.common.friends.gui;

import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class FriendGuiPackets {

    public static final Identifier ID_ACCEPT = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/accept");
    public static final Identifier ID_DENY = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/deny");
    public static final Identifier ID_REMOVE = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/remove");
    public static final Identifier ID_CANCEL = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/cancel");
    public static final Identifier ID_SETTINGS = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/settings");
    public static final Identifier ID_S2C_SNAPSHOT = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/snapshot");
    public static final Identifier ID_C2S_OPEN = Identifier.of(CoresystemCommon.MOD_ID, "friends_gui/open");

    private FriendGuiPackets() {
    }

    public record C2SAccept(UUID from) implements CustomPayload {
        public static final Id<C2SAccept> ID = new Id<>(ID_ACCEPT);
        public static final PacketCodec<RegistryByteBuf, C2SAccept> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeUuid(value.from),
                        buf -> new C2SAccept(buf.readUuid())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SDeny(UUID from) implements CustomPayload {
        public static final Id<C2SDeny> ID = new Id<>(ID_DENY);
        public static final PacketCodec<RegistryByteBuf, C2SDeny> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeUuid(value.from),
                        buf -> new C2SDeny(buf.readUuid())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SRemove(UUID other) implements CustomPayload {
        public static final Id<C2SRemove> ID = new Id<>(ID_REMOVE);
        public static final PacketCodec<RegistryByteBuf, C2SRemove> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeUuid(value.other),
                        buf -> new C2SRemove(buf.readUuid())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SCancel(UUID target) implements CustomPayload {
        public static final Id<C2SCancel> ID = new Id<>(ID_CANCEL);
        public static final PacketCodec<RegistryByteBuf, C2SCancel> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeUuid(value.target),
                        buf -> new C2SCancel(buf.readUuid())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SSetSettings(boolean allowRequests, boolean allowFollow,
                                 boolean showLastSeen) implements CustomPayload {
        public static final Id<C2SSetSettings> ID = new Id<>(ID_SETTINGS);
        public static final PacketCodec<RegistryByteBuf, C2SSetSettings> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                            buf.writeBoolean(value.allowRequests);
                            buf.writeBoolean(value.allowFollow);
                            buf.writeBoolean(value.showLastSeen);
                        },
                        buf -> new C2SSetSettings(buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record S2CSnapshot(FriendGuiSnapshot snapshot) implements CustomPayload {
        public static final Id<S2CSnapshot> ID = new Id<>(ID_S2C_SNAPSHOT);

        public static final PacketCodec<RegistryByteBuf, S2CSnapshot> CODEC =
                PacketCodec.of(
                        (payload, buf) -> payload.snapshot.write(buf),
                        buf -> new S2CSnapshot(FriendGuiSnapshot.read(buf))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SOpen() implements CustomPayload {
        public static final Id<C2SOpen> ID = new Id<>(ID_C2S_OPEN);

        public static final PacketCodec<RegistryByteBuf, C2SOpen> CODEC =
                PacketCodec.of(
                        (value, buf) -> {
                        }, // leer
                        buf -> new C2SOpen()
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
