package dev.huntertagog.coresystem.fabric.common.clans.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class ClanGuiPackets {

    public static final Identifier ID_C2S_OPEN = Identifier.of("coresystem", "clan_gui/open");
    public static final Identifier ID_S2C_SNAPSHOT = Identifier.of("coresystem", "clan_gui/snapshot");

    public static final Identifier ID_C2S_KICK = Identifier.of("coresystem", "clan_gui/kick");
    public static final Identifier ID_C2S_SET_ROLE = Identifier.of("coresystem", "clan_gui/set_role");
    public static final Identifier ID_C2S_SETTINGS = Identifier.of("coresystem", "clan_gui/settings");
    public static final Identifier ID_C2S_ROLE_PERM = Identifier.of("coresystem", "clan_gui/role_perm");

    public record C2SOpen() implements CustomPayload {
        public static final Id<C2SOpen> ID = new Id<>(ID_C2S_OPEN);
        public static final PacketCodec<RegistryByteBuf, C2SOpen> CODEC =
                PacketCodec.of((v, buf) -> {
                }, buf -> new C2SOpen());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record S2CSnapshot(ClanGuiSnapshot snapshot) implements CustomPayload {
        public static final Id<S2CSnapshot> ID = new Id<>(ID_S2C_SNAPSHOT);
        public static final PacketCodec<RegistryByteBuf, S2CSnapshot> CODEC =
                PacketCodec.of(
                        (value, buf) -> value.snapshot().write(buf),
                        buf -> new S2CSnapshot(ClanGuiSnapshot.read(buf))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SKick(UUID target) implements CustomPayload {
        public static final Id<C2SKick> ID = new Id<>(ID_C2S_KICK);
        public static final PacketCodec<RegistryByteBuf, C2SKick> CODEC =
                PacketCodec.of((v, buf) -> buf.writeUuid(v.target), buf -> new C2SKick(buf.readUuid()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SSetRole(UUID target, String roleId) implements CustomPayload {
        public static final Id<C2SSetRole> ID = new Id<>(ID_C2S_SET_ROLE);
        public static final PacketCodec<RegistryByteBuf, C2SSetRole> CODEC =
                PacketCodec.of(
                        (v, buf) -> {
                            buf.writeUuid(v.target);
                            buf.writeString(v.roleId);
                        },
                        buf -> new C2SSetRole(buf.readUuid(), buf.readString())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SSetSettings(boolean openInvites, boolean friendlyFire,
                                 boolean clanChatDefault) implements CustomPayload {
        public static final Id<C2SSetSettings> ID = new Id<>(ID_C2S_SETTINGS);
        public static final PacketCodec<RegistryByteBuf, C2SSetSettings> CODEC =
                PacketCodec.of(
                        (v, buf) -> {
                            buf.writeBoolean(v.openInvites);
                            buf.writeBoolean(v.friendlyFire);
                            buf.writeBoolean(v.clanChatDefault);
                        },
                        buf -> new C2SSetSettings(buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record C2SSetRolePermission(String roleId, String permissionKey, boolean enabled) implements CustomPayload {
        public static final Id<C2SSetRolePermission> ID = new Id<>(ID_C2S_ROLE_PERM);
        public static final PacketCodec<RegistryByteBuf, C2SSetRolePermission> CODEC =
                PacketCodec.of(
                        (v, buf) -> {
                            buf.writeString(v.roleId);
                            buf.writeString(v.permissionKey);
                            buf.writeBoolean(v.enabled);
                        },
                        buf -> new C2SSetRolePermission(buf.readString(), buf.readString(), buf.readBoolean())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private ClanGuiPackets() {
    }
}
