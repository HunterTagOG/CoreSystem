package dev.huntertagog.coresystem.common.islands;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.clans.Clan;
import dev.huntertagog.coresystem.platform.clans.ClanService;

import java.util.Optional;
import java.util.UUID;

/**
 * Zentrale Permission-Utility für Island-Management.
 * Basis-Regel:
 * - Island-Owner selbst: immer erlaubt
 * - Clan-Owner/Officer des Owner-Clans: erlaubt (über ClanService.canManageIsland)
 */
public final class PrivateIslandWorldPermissionUtil {

    private PrivateIslandWorldPermissionUtil() {
    }

    /**
     * @param islandOwnerId UUID des Island-Besitzers
     * @param actorId       UUID des Spielers, der eine Aktion durchführen möchte
     */
    public static boolean canManageIsland(UUID islandOwnerId, UUID actorId) {
        if (islandOwnerId == null || actorId == null) {
            return false;
        }

        // Owner selbst
        if (islandOwnerId.equals(actorId)) {
            return true;
        }

        // Clan-Integration
        ClanService clans = ServiceProvider.getService(ClanService.class);
        if (clans == null) {
            return false;
        }

        // Clan des Island-Owners ermitteln
        Optional<Clan> ownerClan = clans.findByMember(islandOwnerId);
        return ownerClan.filter(clan -> clans.canManageIsland(clan.id(), actorId)).isPresent();

        // ClanService kann für das Clan-Id + ActorId entscheiden, ob Management-Rechte vorhanden sind
    }
}
