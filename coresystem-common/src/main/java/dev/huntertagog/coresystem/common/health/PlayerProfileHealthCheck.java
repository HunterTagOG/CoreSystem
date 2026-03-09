package dev.huntertagog.coresystem.common.health;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.health.HealthCheck;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;

import java.util.Map;
import java.util.UUID;

public final class PlayerProfileHealthCheck implements HealthCheck {

    @Override
    public String name() {
        return "playerProfile";
    }

    @Override
    public HealthCheckResult check() {
        PlayerProfileService service = ServiceProvider.getService(PlayerProfileService.class);
        if (service == null) {
            return HealthCheckResult.of(
                    name(),
                    HealthStatus.DOWN,
                    "PlayerProfileService not registered",
                    Map.of()
            );
        }

        UUID testId = UUID.nameUUIDFromBytes("coresystem-healthcheck-playerprofile".getBytes());

        try {
            long start = System.nanoTime();
            PlayerProfile profile = service.getOrCreate(testId, "healthcheck-profile");
            long durationMs = (System.nanoTime() - start) / 1_000_000L;

            return HealthCheckResult.of(
                    name(),
                    HealthStatus.UP,
                    "PlayerProfile backend reachable",
                    Map.of(
                            "latencyMs", String.valueOf(durationMs),
                            "profileName", profile.getName()
                    )
            );
        } catch (Exception e) {
            return HealthCheckResult.of(
                    name(),
                    HealthStatus.DEGRADED,
                    "PlayerProfileService getOrCreate failed",
                    Map.of("error", String.valueOf(e.getMessage()))
            );
        }
    }
}
