package dev.huntertagog.coresystem.platform.clans;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClanService extends Service {

    Optional<Clan> findById(UUID clanId);

    Optional<Clan> findByMember(UUID playerId);

    Optional<Clan> findByTag(String tag);

    Clan createClan(UUID ownerId,
                    String tag,
                    String name);

    boolean disbandClan(UUID clanId, UUID requester);

    boolean inviteMember(UUID clanId,
                         UUID inviter,
                         UUID target);

    boolean acceptInvite(UUID clanId,
                         UUID playerId);

    boolean kickMember(UUID clanId,
                       UUID requester,
                       UUID target);

    boolean setRole(UUID clanId,
                    UUID requester,
                    UUID target,
                    ClanRole role);

    List<UUID> getMembers(UUID clanId);

    /**
     * Für Island-/Region-Integration: darf Player "Management"-Aktionen für den Clan ausführen?
     */
    boolean canManageIsland(UUID clanId, UUID playerId);

    /**
     * Holt alle offenen Einladungen für den Spieler
     * und markiert sie als "zugestellt" / entfernt sie (je nach Design).
     */
    List<ClanInvite> pollPendingInvites(UUID memberId);
}
