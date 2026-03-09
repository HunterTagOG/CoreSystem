package dev.huntertagog.coresystem.common.friends.follow;

import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class RedisFollowRequestStore {

    private static final String KEY_PREFIX = "cs:follow:req:"; // + targetUuid
    private static final int TTL_SECONDS = (int) Duration.ofMinutes(10).getSeconds();

    public boolean putRequest(UUID target,
                              UUID requester,
                              String requesterName,
                              long nowEpochMs) {

        String key = KEY_PREFIX + target;
        try (Jedis jedis = RedisClient.get().getResource()) {
            // value compact: "<ms>|<name>"
            String val = nowEpochMs + "|" + requesterName;
            jedis.hset(key, requester.toString(), val);
            jedis.expire(key, TTL_SECONDS);
            return true;
        }
    }

    public boolean hasRequest(UUID target, UUID requester) {
        String key = KEY_PREFIX + target;
        try (Jedis jedis = RedisClient.get().getResource()) {
            return jedis.hexists(key, requester.toString());
        }
    }

    public boolean removeRequest(UUID target, UUID requester) {
        String key = KEY_PREFIX + target;
        try (Jedis jedis = RedisClient.get().getResource()) {
            return jedis.hdel(key, requester.toString()) > 0;
        }
    }

    public Map<UUID, String> listRequests(UUID target) {
        String key = KEY_PREFIX + target;
        try (Jedis jedis = RedisClient.get().getResource()) {
            Map<String, String> raw = jedis.hgetAll(key);
            if (raw == null || raw.isEmpty()) return Collections.emptyMap();

            Map<UUID, String> out = new LinkedHashMap<>();
            for (var e : raw.entrySet()) {
                try {
                    out.put(UUID.fromString(e.getKey()), e.getValue());
                } catch (Exception ignored) {
                }
            }
            return out;
        }
    }
}
