package dev.huntertagog.coresystem.platform.player.playerdata;

import java.util.Optional;
import java.util.UUID;

public interface OnlinePlayerResolver {
    Optional<UUID> resolveOnline(UUID playerId); // trivial placeholder, du kannst auch boolean isOnline(UUID)
}
