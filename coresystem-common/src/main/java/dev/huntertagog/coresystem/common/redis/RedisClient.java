package dev.huntertagog.coresystem.common.redis;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

public final class RedisClient {

    private static final Logger LOG = LoggerFactory.get("Redis");
    private static final RedisClient INSTANCE = new RedisClient();

    private final JedisPool pool;

    private RedisClient() {
        String host = getenvOrDefault("REDIS_HOST", "redis-main");
        int port = Integer.parseInt(getenvOrDefault("REDIS_PORT", "6379"));
        // ACL defaults: user=default
        String user = getenvOrDefault("REDIS_USER", "default");
        // KEIN Hardcoded Default-Passwort mehr
        String password = getenvOrDefault("REDIS_PASSWORD", "");
        int timeoutMs = Integer.parseInt(getenvOrDefault("REDIS_TIMEOUT_MS", "2000"));

        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(64);
        cfg.setMaxIdle(32);
        cfg.setMinIdle(4);
        cfg.setBlockWhenExhausted(true);
        cfg.setMaxWait(Duration.ofSeconds(2));
        cfg.setTestOnBorrow(false);
        cfg.setTestWhileIdle(true);
        cfg.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        cfg.setMinEvictableIdleDuration(Duration.ofMinutes(2));

        try {
            if (password != null && !password.isBlank()) {
                // ✅ Jedis 7.x: ACL Auth (user + password)
                pool = new JedisPool(cfg, host, port, timeoutMs, user, password);
            } else {
                // ohne Auth
                pool = new JedisPool(cfg, host, port, timeoutMs);
            }

            // Fail-fast: Auth/Conn sofort prüfen
            try (var j = pool.getResource()) {
                j.ping();
            }

            LOG.info("Initialized RedisClient for {}:{} (user={})", host, port, user);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.REDIS_INIT_FAILED,
                            CoreErrorSeverity.CRITICAL,
                            "Failed to initialize Redis connection pool"
                    )
                    .withCause(e)
                    .withContextEntry("host", host)
                    .withContextEntry("port", port)
                    .withContextEntry("user", user);

            LOG.error(error.toLogString());
            throw new IllegalStateException(error.technicalMessage(), e);
        }
    }

    public static RedisClient get() {
        return INSTANCE;
    }

    public Jedis getResource() {
        try {
            return pool.getResource();
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.REDIS_NO_CONNECTION,
                            CoreErrorSeverity.ERROR,
                            "Failed to fetch Redis resource from pool"
                    )
                    .withCause(e)
                    .withContextEntry("poolActive", pool != null)
                    .withContextEntry("poolClosed", pool == null || pool.isClosed());

            LOG.error(error.toLogString());
            throw new IllegalStateException(error.technicalMessage(), e);
        }
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        LOG.warn("Environment variable {} = {}", key, v);
        return v != null && !v.isBlank() ? v : def;
    }

    public void shutdown() {
        try {
            if (!pool.isClosed()) {
                pool.close();
                LOG.info("Redis pool closed");
            }
        } catch (Exception e) {
            LOG.warn("Error while closing Redis pool", e);
        }
    }
}
