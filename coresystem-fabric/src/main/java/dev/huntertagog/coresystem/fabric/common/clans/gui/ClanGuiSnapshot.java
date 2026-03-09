package dev.huntertagog.coresystem.fabric.common.clans.gui;

import net.minecraft.network.PacketByteBuf;

import java.util.*;

public record ClanGuiSnapshot(
        boolean inClan,
        UUID clanId,
        String tag,
        String name,

        ClanSettings settings,
        List<MemberEntry> members,
        List<RoleEntry> roles,

        boolean canManageMembers,
        boolean canManageRoles,
        boolean canManageSettings
) {

    public record ClanSettings(
            boolean openInvites,
            boolean friendlyFire,
            boolean clanChatDefault
    ) {
    }

    public record MemberEntry(
            UUID uuid,
            String name,
            boolean online,
            long lastSeenEpochMillis,
            String roleId // z.B. "OWNER","ADMIN","MEMBER" oder custom
    ) {
    }

    public record RoleEntry(
            String roleId,
            String displayName,
            Set<String> permissions // Strings, damit du serverseitig frei erweitern kannst
    ) {
    }

    // ------------------------------------------------------------
    // Codec (einfach + robust)
    // ------------------------------------------------------------

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(inClan);
        buf.writeUuid(clanId == null ? new UUID(0, 0) : clanId);
        buf.writeString(tag == null ? "" : tag);
        buf.writeString(name == null ? "" : name);

        buf.writeBoolean(settings != null && settings.openInvites());
        buf.writeBoolean(settings != null && settings.friendlyFire());
        buf.writeBoolean(settings != null && settings.clanChatDefault());

        buf.writeVarInt(members == null ? 0 : members.size());
        if (members != null) {
            for (var m : members) {
                buf.writeUuid(m.uuid());
                buf.writeString(m.name());
                buf.writeBoolean(m.online());
                buf.writeLong(m.lastSeenEpochMillis());
                buf.writeString(m.roleId() == null ? "" : m.roleId());
            }
        }

        buf.writeVarInt(roles == null ? 0 : roles.size());
        if (roles != null) {
            for (var r : roles) {
                buf.writeString(r.roleId());
                buf.writeString(r.displayName());
                buf.writeVarInt(r.permissions().size());
                for (String p : r.permissions()) buf.writeString(p);
            }
        }

        buf.writeBoolean(canManageMembers);
        buf.writeBoolean(canManageRoles);
        buf.writeBoolean(canManageSettings);
    }

    public static ClanGuiSnapshot read(PacketByteBuf buf) {
        boolean inClan = buf.readBoolean();
        UUID clanId = buf.readUuid();
        String tag = buf.readString();
        String name = buf.readString();

        ClanSettings settings = new ClanSettings(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );

        int mSize = buf.readVarInt();
        List<MemberEntry> members = new ArrayList<>(mSize);
        for (int i = 0; i < mSize; i++) {
            members.add(new MemberEntry(
                    buf.readUuid(),
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readLong(),
                    buf.readString()
            ));
        }

        int rSize = buf.readVarInt();
        List<RoleEntry> roles = new ArrayList<>(rSize);
        for (int i = 0; i < rSize; i++) {
            String roleId = buf.readString();
            String displayName = buf.readString();

            int pSize = buf.readVarInt();
            Set<String> perms = new HashSet<>();
            for (int p = 0; p < pSize; p++) perms.add(buf.readString());

            roles.add(new RoleEntry(roleId, displayName, perms));
        }

        boolean canManageMembers = buf.readBoolean();
        boolean canManageRoles = buf.readBoolean();
        boolean canManageSettings = buf.readBoolean();

        // wenn nicht inClan -> clanId kann dummy sein
        if (!inClan) clanId = null;

        return new ClanGuiSnapshot(
                inClan, clanId, tag, name,
                settings, members, roles,
                canManageMembers, canManageRoles, canManageSettings
        );
    }
}
