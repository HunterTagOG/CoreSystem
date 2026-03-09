package dev.huntertagog.coresystem.common.feature;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleKey;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleService;
import redis.clients.jedis.Jedis;

import java.util.EnumMap;
import java.util.Map;

public final class RedisFeatureToggleService implements FeatureToggleService {

    private static final Logger LOG = LoggerFactory.get("FeatureToggleService");
    private static final String KEY_PREFIX = "cs:feature-toggle:";

    // lokale Overrides (z. B. zur Laufzeit gesetzt, ohne Redis)
    private final Map<FeatureToggleKey, Boolean> localOverrides = new EnumMap<>(FeatureToggleKey.class);

    @Override
    public boolean isEnabled(FeatureToggleKey key) {
        // 1) Local override
        Boolean local = localOverrides.get(key);
        if (local != null) {
            return local;
        }

        // 2) Redis override
        String redisKey = KEY_PREFIX + key.key();
        try (Jedis jedis = RedisClient.get().getResource()) {
            String val = jedis.get(redisKey);
            if (val != null) {
                if ("1".equals(val) || "true".equalsIgnoreCase(val)) {
                    return true;
                }
                if ("0".equals(val) || "false".equalsIgnoreCase(val)) {
                    return false;
                }
            }
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.FEATURE_TOGGLE_READ_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to read feature toggle from Redis"
                    )
                    .withCause(e)
                    .withContextEntry("key", key.key())
                    .withContextEntry("redisKey", redisKey)
                    .log();
        }

        // 3) ENV Override
        String envKey = "CORESYSTEM_FEATURE_" + key.key().toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null) {
            return Boolean.parseBoolean(envVal);
        }

        // 4) Default
        return key.defaultEnabled();
    }

    @Override
    public void setOverride(FeatureToggleKey key, Boolean enabled) {
        if (enabled == null) {
            localOverrides.remove(key);
            LOG.info("FeatureToggle override removed for {}", key.key());
        } else {
            localOverrides.put(key, enabled);
            LOG.info("FeatureToggle override set for {} => {}", key.key(), enabled);
        }
    }
}
