package dev.huntertagog.coresystem.common.clans.chat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.clans.chat.ClanChatService;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.UUID;

public final class RedisClanChatService implements ClanChatService {

    private static final Logger LOG = LoggerFactory.get("ClanChat");
    private static final String KEY_ENABLED = "cs:clan:chat:enabled";

    private final Cache<UUID, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(50_000)
            .build();

    @Override
    public boolean isClanChatEnabled(UUID playerId) {
        Boolean cached = cache.getIfPresent(playerId);
        if (cached != null) return cached;

        try (Jedis jedis = RedisClient.get().getResource()) {
            boolean enabled = jedis.sismember(KEY_ENABLED, playerId.toString());
            cache.put(playerId, enabled);
            return enabled;
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.REDIS_FAILURE, CoreErrorSeverity.WARN, "Failed to read clan chat state")
                    .withCause(e)
                    .withContextEntry("playerId", playerId.toString())
                    .withContextEntry("redisKey", KEY_ENABLED)
                    .log();
            return false;
        }
    }

    @Override
    public void setClanChatEnabled(UUID playerId, boolean enabled) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            if (enabled) {
                jedis.sadd(KEY_ENABLED, playerId.toString());
            } else {
                jedis.srem(KEY_ENABLED, playerId.toString());
            }
            cache.put(playerId, enabled);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.REDIS_FAILURE, CoreErrorSeverity.WARN, "Failed to write clan chat state")
                    .withCause(e)
                    .withContextEntry("playerId", playerId.toString())
                    .withContextEntry("enabled", String.valueOf(enabled))
                    .withContextEntry("redisKey", KEY_ENABLED)
                    .log();
        }
    }
}
