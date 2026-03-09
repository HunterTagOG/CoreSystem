package dev.huntertagog.coresystem.platform.clans;

import java.util.UUID;

public record ClanMember(
        UUID playerId,
        ClanRole role,
        long joinedAt
) {
}
