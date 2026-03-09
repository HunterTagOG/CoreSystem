package dev.huntertagog.coresystem.common.health;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.health.HealthCheck;
import dev.huntertagog.coresystem.platform.health.HealthCheckResult;
import dev.huntertagog.coresystem.platform.health.HealthStatus;
import redis.clients.jedis.Jedis;

import java.util.Map;

public final class RedisHealthCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.get("RedisHealthCheck");

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public HealthCheckResult check() {
        long start = System.nanoTime();

        try (Jedis jedis = RedisClient.get().getResource()) {
            String pong = jedis.ping();
            long durationMs = (System.nanoTime() - start) / 1_000_000L;

            if (!"PONG".equalsIgnoreCase(pong)) {
                return HealthCheckResult.of(
                        name(),
                        HealthStatus.DEGRADED,
                        "Redis responded with unexpected PING reply",
                        Map.of("reply", pong, "latencyMs", String.valueOf(durationMs))
                );
            }

            return HealthCheckResult.of(
                    name(),
                    HealthStatus.UP,
                    "Redis reachable",
                    Map.of("latencyMs", String.valueOf(durationMs))
            );

        } catch (Exception e) {
            LOG.warn("Redis health check failed", e);
            return HealthCheckResult.of(
                    name(),
                    HealthStatus.DOWN,
                    "Redis not reachable: " + e.getClass().getSimpleName(),
                    Map.of("error", String.valueOf(e.getMessage()))
            );
        }
    }
}
