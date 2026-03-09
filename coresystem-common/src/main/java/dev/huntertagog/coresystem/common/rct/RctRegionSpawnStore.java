package dev.huntertagog.coresystem.common.rct;

import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;

public final class RctRegionSpawnStore {

    private static final String KEY_PREFIX = "cs:rct:region_spawned:";

    private RctRegionSpawnStore() {
    }

    public static boolean isSpawned(String worldId, String regionId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            return jedis.exists(KEY_PREFIX + worldId + ":" + regionId);
        }
    }

    public static void markSpawned(String worldId, String regionId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.set(KEY_PREFIX + worldId + ":" + regionId, "1");
        }
    }
}
