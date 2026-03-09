package dev.huntertagog.coresystem.common.health;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.economy.CurrencyId;
import dev.huntertagog.coresystem.platform.economy.EconomyService;
import dev.huntertagog.coresystem.platform.economy.Money;
import dev.huntertagog.coresystem.platform.health.HealthCheck;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;

import java.util.Map;
import java.util.UUID;

public final class EconomyHealthCheck implements HealthCheck {

    @Override
    public String name() {
        return "economy";
    }

    @Override
    public HealthCheckResult check() {
        EconomyService service = ServiceProvider.getService(EconomyService.class);
        if (service == null) {
            return HealthCheckResult.of(
                    name(),
                    HealthStatus.DOWN,
                    "EconomyService not registered",
                    Map.of()
            );
        }

        UUID testId = UUID.nameUUIDFromBytes("coresystem-healthcheck-economy".getBytes());
        CurrencyId currency = CurrencyId.COINS;

        try {
            long start = System.nanoTime();
            Money balance = service.getBalance(testId, currency);
            long durationMs = (System.nanoTime() - start) / 1_000_000L;

            return HealthCheckResult.of(
                    name(),
                    HealthStatus.UP,
                    "Economy backend reachable",
                    Map.of(
                            "latencyMs", String.valueOf(durationMs),
                            "balance", String.valueOf(balance.amount()),
                            "currency", currency.value()
                    )
            );
        } catch (Exception e) {
            return HealthCheckResult.of(
                    name(),
                    HealthStatus.DEGRADED,
                    "EconomyService getBalance failed",
                    Map.of("error", String.valueOf(e.getMessage()))
            );
        }
    }
}
