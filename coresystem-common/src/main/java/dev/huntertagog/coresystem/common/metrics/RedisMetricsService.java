package dev.huntertagog.coresystem.common.metrics;

import com.google.gson.Gson;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.StringJoiner;

public final class RedisMetricsService implements MetricsService {

    private static final Logger LOG = LoggerFactory.get("RedisMetricsService");

    private static final String PREFIX_COUNTER = "cs:metrics:counter:";
    private static final String PREFIX_GAUGE = "cs:metrics:gauge:";
    private static final String PREFIX_TIMER = "cs:metrics:timer:";

    private final Gson gson = new Gson(); // derzeit nur für evtl. Debug/Extensions interessant

    @Override
    public void incrementCounter(String name,
                                 long delta,
                                 Map<String, String> tags) {

        if (delta == 0) return;

        String key = PREFIX_COUNTER + buildMetricKey(name, tags);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.incrBy(key, delta);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.METRICS_WRITE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to increment counter metric in Redis"
                    )
                    .withCause(e)
                    .withContextEntry("metric", name)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("delta", String.valueOf(delta))
                    .log();

            LOG.debug("Metrics counter write degraded for '{}'", key, e);
        }
    }

    @Override
    public void setGauge(String name,
                         long value,
                         Map<String, String> tags) {

        String key = PREFIX_GAUGE + buildMetricKey(name, tags);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.set(key, String.valueOf(value));
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.METRICS_WRITE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to set gauge metric in Redis"
                    )
                    .withCause(e)
                    .withContextEntry("metric", name)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("value", String.valueOf(value))
                    .log();
        }
    }

    @Override
    public void recordTimer(String name,
                            long durationMillis,
                            Map<String, String> tags) {

        if (durationMillis < 0) {
            durationMillis = 0;
        }

        String key = PREFIX_TIMER + buildMetricKey(name, tags);

        try (Jedis jedis = RedisClient.get().getResource()) {
            // Hash-Felder: count, totalMs, maxMs
            jedis.hincrBy(key, "count", 1L);
            jedis.hincrBy(key, "totalMs", durationMillis);

            String currentMaxStr = jedis.hget(key, "maxMs");
            long currentMax = currentMaxStr != null ? Long.parseLong(currentMaxStr) : 0L;
            if (durationMillis > currentMax) {
                jedis.hset(key, "maxMs", String.valueOf(durationMillis));
            }
        } catch (NumberFormatException e) {
            CoreError.of(
                            CoreErrorCode.METRICS_PARSE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to parse timer metric fields from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("metric", name)
                    .withContextEntry("redisKey", key)
                    .log();
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.METRICS_WRITE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to record timer metric in Redis"
                    )
                    .withCause(e)
                    .withContextEntry("metric", name)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("durationMs", String.valueOf(durationMillis))
                    .log();
        }
    }

    // ------------------------------------------------------
    // Helper: simple Tag-Encoding in Metric-Key
    // ------------------------------------------------------

    private String buildMetricKey(String name, Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return name;
        }
        StringJoiner joiner = new StringJoiner(",");
        tags.forEach((k, v) -> joiner.add(k + "=" + v));
        return name + "|" + joiner;
    }
}
