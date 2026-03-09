package dev.huntertagog.coresystem.fabric.common.clans.gui;

import dev.huntertagog.coresystem.common.clans.gui.ClanRolePermissionStore;
import dev.huntertagog.coresystem.common.clans.gui.ClanSettingsStore;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.clans.Clan;
import dev.huntertagog.coresystem.platform.clans.ClanRole;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClanGuiServerNet {

    private ClanGuiServerNet() {
    }

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(ClanGuiPackets.C2SOpen.ID, (payload, ctx) -> {
            ctx.server().execute(() -> sendSnapshot(ctx.server(), ctx.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ClanGuiPackets.C2SKick.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                ClanService clans = ServiceProvider.getService(ClanService.class);
                if (clans == null) return;

                var self = ctx.player().getUuid();
                var target = payload.target();

                var clanOpt = clans.findByMember(self);
                if (clanOpt.isEmpty()) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                Clan clan = clanOpt.get();

                if (!clans.canManageIsland(clan.id(), self)) { // reuse "manage" gate
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                clans.kickMember(clan.id(), self, target);
                sendSnapshot(ctx.server(), ctx.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ClanGuiPackets.C2SSetRole.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                ClanService clans = ServiceProvider.getService(ClanService.class);
                if (clans == null) return;

                UUID self = ctx.player().getUuid();
                var clanOpt = clans.findByMember(self);
                if (clanOpt.isEmpty()) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                Clan clan = clanOpt.get();

                if (!clans.canManageIsland(clan.id(), self)) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                // Mapping: roleId string -> ClanRole (minimal)
                ClanRole role = parseRole(payload.roleId());
                clans.setRole(clan.id(), self, payload.target(), role);

                sendSnapshot(ctx.server(), ctx.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ClanGuiPackets.C2SSetSettings.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                ClanService clans = ServiceProvider.getService(ClanService.class);
                if (clans == null) return;

                UUID self = ctx.player().getUuid();
                var clanOpt = clans.findByMember(self);
                if (clanOpt.isEmpty()) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                Clan clan = clanOpt.get();
                if (!clans.canManageIsland(clan.id(), self)) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                ClanSettingsStore.set(clan.id(), payload.openInvites(), payload.friendlyFire(), payload.clanChatDefault());
                sendSnapshot(ctx.server(), ctx.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ClanGuiPackets.C2SSetRolePermission.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                ClanService clans = ServiceProvider.getService(ClanService.class);
                if (clans == null) return;

                UUID self = ctx.player().getUuid();
                var clanOpt = clans.findByMember(self);
                if (clanOpt.isEmpty()) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                Clan clan = clanOpt.get();
                if (!clans.canManageIsland(clan.id(), self)) {
                    sendSnapshot(ctx.server(), ctx.player());
                    return;
                }

                ClanRolePermissionStore.setPerm(clan.id(), payload.roleId(), payload.permissionKey(), payload.enabled());
                sendSnapshot(ctx.server(), ctx.player());
            });
        });
    }

    private static void sendSnapshot(MinecraftServer server, ServerPlayerEntity player) {
        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) return;

        PlayerProfileService profiles = ServiceProvider.getService(PlayerProfileService.class);

        UUID self = player.getUuid();
        var clanOpt = clans.findByMember(self);

        if (clanOpt.isEmpty()) {
            ClanGuiSnapshot snap = new ClanGuiSnapshot(
                    false, null, "", "",
                    new ClanGuiSnapshot.ClanSettings(false, false, false),
                    List.of(),
                    List.of(),
                    false, false, false
            );
            ServerPlayNetworking.send(player, new ClanGuiPackets.S2CSnapshot(snap));
            return;
        }

        Clan clan = clanOpt.get();

        boolean canManage = clans.canManageIsland(clan.id(), self);
        boolean canManageMembers = canManage;
        boolean canManageRoles = canManage;
        boolean canManageSettings = canManage;

        var settings = ClanSettingsStore.get(clan.id());

        List<UUID> members = clans.getMembers(clan.id());
        List<ClanGuiSnapshot.MemberEntry> memberEntries = new ArrayList<>(members.size());

        for (UUID id : members) {
            boolean online = server.getPlayerManager().getPlayer(id) != null;
            String name = resolveName(server, id, profiles);

            long lastSeen = 0L;
            if (profiles != null && !online) {
                PlayerProfile p = profiles.find(id).orElse(null);
                if (p != null) lastSeen = p.getLastSeenAt();
            }

            // minimal: Owner/Member ableiten (wenn du mehr Rollen hast -> Clan.members() muss Role liefern)
            String roleId = clan.isOwner(id) ? "OWNER" : "MEMBER";

            memberEntries.add(new ClanGuiSnapshot.MemberEntry(id, name, online, lastSeen, roleId));
        }

        // Rollen: minimal fest verdrahtet + perms aus Store
        List<ClanGuiSnapshot.RoleEntry> roles = new ArrayList<>();
        roles.add(new ClanGuiSnapshot.RoleEntry("OWNER", "Owner", ClanRolePermissionStore.getPerms(clan.id(), "OWNER")));
        roles.add(new ClanGuiSnapshot.RoleEntry("ADMIN", "Admin", ClanRolePermissionStore.getPerms(clan.id(), "ADMIN")));
        roles.add(new ClanGuiSnapshot.RoleEntry("MEMBER", "Member", ClanRolePermissionStore.getPerms(clan.id(), "MEMBER")));

        ClanGuiSnapshot snap = new ClanGuiSnapshot(
                true, clan.id(), clan.tag(), clan.name(),
                new ClanGuiSnapshot.ClanSettings(settings.openInvites(), settings.friendlyFire(), settings.clanChatDefault()),
                memberEntries,
                roles,
                canManageMembers,
                canManageRoles,
                canManageSettings
        );

        ServerPlayNetworking.send(player, new ClanGuiPackets.S2CSnapshot(snap));
    }

    private static String resolveName(MinecraftServer server, UUID id, PlayerProfileService profiles) {
        var online = server.getPlayerManager().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();

        if (profiles != null) {
            var p = profiles.find(id).orElse(null);
            if (p != null && p.getName() != null && !p.getName().isBlank()) return p.getName();
        }
        return id.toString().substring(0, 8);
    }

    private static ClanRole parseRole(String roleId) {
        // deine ClanRole Enum kenn ich nicht exakt -> minimal robust
        if (roleId == null) return ClanRole.MEMBER;
        return switch (roleId.toUpperCase()) {
            case "OWNER" -> ClanRole.OWNER;
            case "ADMIN" -> ClanRole.ADMIN;
            default -> ClanRole.MEMBER;
        };
    }

    public static void openFor(MinecraftServer server, ServerPlayerEntity player) {
        server.execute(() -> sendSnapshot(server, player));
    }
}
