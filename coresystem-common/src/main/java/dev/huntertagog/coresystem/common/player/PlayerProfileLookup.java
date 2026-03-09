package dev.huntertagog.coresystem.common.player;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;

import java.util.Optional;
import java.util.UUID;

public final class PlayerProfileLookup {

    private PlayerProfileLookup() {
    }

    public record LastLocation(String lastServer,
                               String lastNodeId) {
    }

    public static Optional<LastLocation> lastLocation(UUID playerId) {
        PlayerProfileService svc = ServiceProvider.getService(PlayerProfileService.class);
        if (svc == null) return Optional.empty();

        return svc.find(playerId)
                .map(p -> new LastLocation(
                        p.getLastServer() != null ? p.getLastServer() : "unknown",
                        p.getLastNodeId()
                ));
    }
}
