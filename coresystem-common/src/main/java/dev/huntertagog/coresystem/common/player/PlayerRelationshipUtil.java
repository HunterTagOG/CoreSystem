package dev.huntertagog.coresystem.common.player;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.clans.Clan;
import dev.huntertagog.coresystem.platform.clans.ClanService;
import dev.huntertagog.coresystem.platform.friends.FriendService;

import java.util.Optional;
import java.util.UUID;

/**
 * Zentraler Relationship-Layer:
 * - Owner selbst
 * - Friends (FriendService.areFriends)
 * - Clan-Member mit Management-Rechten (ClanService.findByMember + canManageIsland)
 */
public final class PlayerRelationshipUtil {

    private PlayerRelationshipUtil() {
    }

    /**
     * "Darf other für owner Dinge managen?" – allgemeiner Trust-Check.
     */
    public static boolean isTrustedForOwner(UUID ownerId, UUID otherId) {
        if (ownerId == null || otherId == null) {
            return false;
        }

        // Owner selbst
        if (ownerId.equals(otherId)) {
            return true;
        }

        // 1) Friend-Relation
        FriendService friends = ServiceProvider.getService(FriendService.class);
        if (friends != null && friends.areFriends(ownerId, otherId)) {
            return true;
        }

        // 2) Clan-Relation
        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            return false;
        }

        // Clan des Owners ermitteln
        Optional<Clan> ownerClan = clans.findByMember(ownerId);
        return ownerClan.filter(clan -> clans.canManageIsland(clan.id(), otherId)).isPresent();

        // Option A: gleicher Clan + Rollenmodell via canManageIsland

        // Falls du canManageIsland intern anders implementierst, kannst du hier
        // optional noch "same clan only" abbilden:
        //
        // Optional<Clan> otherClan = clans.findByMember(otherId);
        // if (otherClan.isPresent() && otherClan.get().id().equals(ownerClan.get().id())) {
        //     return true;
        // }
    }
}
