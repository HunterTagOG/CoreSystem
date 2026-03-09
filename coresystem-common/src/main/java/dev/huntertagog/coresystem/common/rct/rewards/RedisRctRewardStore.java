package dev.huntertagog.coresystem.common.rct.rewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.huntertagog.coresystem.common.rct.rewards.dto.RctRewardSpecDto;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.util.Optional;

public final class RedisRctRewardStore {

    private static final String KEY_PREFIX = "cs:rct:rewards:";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public Optional<RctRewardSpecDto> find(String trainerId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String json = jedis.get(KEY_PREFIX + trainerId.toLowerCase());
            if (json == null || json.isBlank()) return Optional.empty();
            return Optional.ofNullable(GSON.fromJson(json, RctRewardSpecDto.class));
        }
    }

    public void upsert(String trainerId, RctRewardSpecDto dto) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.set(KEY_PREFIX + trainerId.toLowerCase(), GSON.toJson(dto));
        }
    }

    public boolean exists(String trainerId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            return jedis.exists(KEY_PREFIX + trainerId.toLowerCase());
        }
    }

    public void seedDefaultsIfMissing() {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String defaultTrainerId = "default";
            String key = KEY_PREFIX + defaultTrainerId.toLowerCase();

            if (!jedis.exists(key)) {
                RctRewardSpecDto defaultDto = RctRewardSpecDto.defaultSpec();
                jedis.set(key, GSON.toJson(defaultDto));
            }
        }
    }
}
