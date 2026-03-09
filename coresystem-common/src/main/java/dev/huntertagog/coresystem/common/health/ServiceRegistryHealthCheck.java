package dev.huntertagog.coresystem.common.health;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.economy.EconomyService;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.health.HealthCheck;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;
import dev.huntertagog.coresystem.platform.message.PlayerMessageService;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServiceRegistryHealthCheck implements HealthCheck {

    @Override
    public String name() {
        return "serviceRegistry";
    }

    @Override
    public HealthCheckResult check() {
        Map<String, String> details = new LinkedHashMap<>();

        boolean profileOk = ServiceProvider.getService(PlayerProfileService.class) != null;
        boolean economyOk = ServiceProvider.getService(EconomyService.class) != null;
        boolean eventBusOk = ServiceProvider.getService(DomainEventBus.class) != null;
        boolean messagingOk = ServiceProvider.getService(PlayerMessageService.class) != null;

        details.put("PlayerProfileService", String.valueOf(profileOk));
        details.put("EconomyService", String.valueOf(economyOk));
        details.put("DomainEventBus", String.valueOf(eventBusOk));
        details.put("PlayerMessageService", String.valueOf(messagingOk));

        boolean allOk = profileOk && economyOk && eventBusOk && messagingOk;

        return HealthCheckResult.of(
                name(),
                allOk ? HealthStatus.UP : HealthStatus.DEGRADED,
                allOk ? "All core services registered" : "Some core services missing",
                details
        );
    }
}
