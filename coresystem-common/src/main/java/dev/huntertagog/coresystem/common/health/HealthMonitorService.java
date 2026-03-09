package dev.huntertagog.coresystem.common.health;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.health.HealthCheck;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;
import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.*;

public final class HealthMonitorService implements Service {

    private static final Logger LOG = LoggerFactory.get("HealthMonitor");

    private final Map<String, HealthCheck> checks = new LinkedHashMap<>();

    public void register(HealthCheck check) {
        this.checks.put(check.name(), check);
        LOG.info("Registered health check '{}'", check.name());
    }

    public Collection<HealthCheck> getChecks() {
        return Collections.unmodifiableCollection(checks.values());
    }

    public List<HealthCheckResult> runAll() {
        List<HealthCheckResult> results = new ArrayList<>();
        for (HealthCheck check : checks.values()) {
            try {
                HealthCheckResult result = check.check();
                results.add(result);
            } catch (Exception e) {
                LOG.warn("Health check '{}' threw exception, mapping to DOWN", check.name(), e);
                results.add(
                        HealthCheckResult.of(
                                check.name(),
                                HealthStatus.DOWN,
                                "Check threw exception: " + e.getClass().getSimpleName(),
                                Map.of("error", String.valueOf(e.getMessage()))
                        )
                );
            }
        }
        return results;
    }

    public HealthStatus aggregateStatus(List<HealthCheckResult> results) {
        boolean anyDown = results.stream().anyMatch(r -> r.status() == HealthStatus.DOWN);
        if (anyDown) return HealthStatus.DOWN;

        boolean anyDegraded = results.stream().anyMatch(r -> r.status() == HealthStatus.DEGRADED);
        if (anyDegraded) return HealthStatus.DEGRADED;

        return HealthStatus.UP;
    }

    public static HealthMonitorService get() {
        return ServiceProvider.getService(HealthMonitorService.class);
    }
}
