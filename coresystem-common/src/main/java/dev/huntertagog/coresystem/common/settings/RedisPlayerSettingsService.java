package dev.huntertagog.coresystem.common.settings;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.settings.PlayerSettingKey;
import dev.huntertagog.coresystem.platform.settings.PlayerSettingsService;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.*;

public final class RedisPlayerSettingsService implements PlayerSettingsService {

    private static final Logger LOG = LoggerFactory.get("PlayerSettings");
    private static final String KEY_PREFIX = "cs:playersettings:";

    private final Cache<UUID, Map<String, String>> cache;

    public RedisPlayerSettingsService() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(50_000)
                .build();
    }

    private String redisKey(UUID playerId) {
        return KEY_PREFIX + playerId;
    }

    @Override
    public <T> Optional<T> get(UUID playerId, PlayerSettingKey<T> key) {
        Map<String, String> all = loadSettings(playerId);
        String raw = all.get(key.key());
        if (raw == null) {
            return Optional.empty();
        }

        try {
            T value = key.parse(raw);
            return Optional.ofNullable(value);
        } catch (Exception ex) {
            CoreError.of(
                            CoreErrorCode.PLAYERSETTINGS_PARSE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to parse PlayerSetting value"
                    )
                    .withCause(ex)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("settingKey", key.key())
                    .withContextEntry("raw", raw)
                    .log();
            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrDefault(UUID playerId, PlayerSettingKey<T> key) {
        return get(playerId, key).orElse(key.defaultValue());
    }

    @Override
    public <T> void set(UUID playerId, PlayerSettingKey<T> key, T value) {
        String redisKey = redisKey(playerId);
        String serialized = key.serialize(value);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.hset(redisKey, key.key(), serialized);
        } catch (Exception ex) {
            CoreError.of(
                            CoreErrorCode.PLAYERSETTINGS_REDIS_SAVE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to save PlayerSetting to Redis"
                    )
                    .withCause(ex)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("settingKey", key.key())
                    .withContextEntry("value", serialized)
                    .log();
        }

        // Cache aktualisieren
        cache.asMap().compute(playerId, (id, existing) -> {
            Map<String, String> map = existing != null ? new HashMap<>(existing) : new HashMap<>();
            map.put(key.key(), serialized);
            return map;
        });
    }

    @Override
    public void clear(UUID playerId, PlayerSettingKey<?> key) {
        String redisKey = redisKey(playerId);

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.hdel(redisKey, key.key());
        } catch (Exception ex) {
            CoreError.of(
                            CoreErrorCode.PLAYERSETTINGS_REDIS_DELETE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to delete PlayerSetting from Redis"
                    )
                    .withCause(ex)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("settingKey", key.key())
                    .log();
        }

        cache.asMap().computeIfPresent(playerId, (id, existing) -> {
            Map<String, String> map = new HashMap<>(existing);
            map.remove(key.key());
            return map;
        });
    }

    @Override
    public Map<String, String> getAllRaw(UUID playerId) {
        return Collections.unmodifiableMap(loadSettings(playerId));
    }

    // ---------------------- intern ----------------------

    private Map<String, String> loadSettings(UUID playerId) {
        Map<String, String> cached = cache.getIfPresent(playerId);
        if (cached != null) {
            return cached;
        }

        String redisKey = redisKey(playerId);
        Map<String, String> loaded = new HashMap<>();
        try (Jedis jedis = RedisClient.get().getResource()) {
            Map<String, String> fromRedis = jedis.hgetAll(redisKey);
            if (fromRedis != null) {
                loaded.putAll(fromRedis);
            }
        } catch (Exception ex) {
            CoreError.of(
                            CoreErrorCode.PLAYERSETTINGS_REDIS_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load PlayerSettings from Redis"
                    )
                    .withCause(ex)
                    .withContextEntry("playerUuid", playerId.toString())
                    .withContextEntry("redisKey", redisKey)
                    .log();
        }

        cache.put(playerId, loaded);
        return loaded;
    }
}
