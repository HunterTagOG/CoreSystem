package dev.huntertagog.coresystem.platform.clans;

import java.util.Map;
import java.util.UUID;

public record Clan(
        UUID id,
        String tag,
        String name,
        UUID ownerId,
        Map<UUID, ClanMember> members,
        long createdAt,
        String motd
) {
    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }
}
