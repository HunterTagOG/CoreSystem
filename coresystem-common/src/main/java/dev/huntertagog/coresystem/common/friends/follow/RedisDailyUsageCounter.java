package dev.huntertagog.coresystem.common.friends.follow;

import dev.huntertagog.coresystem.common.redis.RedisClient;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class RedisDailyUsageCounter {

    private static final String KEY_PREFIX = "cs:follow:uses:"; // + yyyyMMdd + ":" + uuid
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    public int incrementAndGet(UUID requester) {
        String day = LocalDate.now(ZoneId.systemDefault()).format(DAY);
        String key = KEY_PREFIX + day + ":" + requester;

        try (Jedis jedis = RedisClient.get().getResource()) {
            long val = jedis.incr(key);

            // TTL bis Tagesende
            long ttl = secondsUntilEndOfDay();
            if (ttl > 0) jedis.expire(key, (int) ttl);

            return (int) val;
        }
    }

    public int get(UUID requester) {
        String day = LocalDate.now(ZoneId.systemDefault()).format(DAY);
        String key = KEY_PREFIX + day + ":" + requester;

        try (Jedis jedis = RedisClient.get().getResource()) {
            String s = jedis.get(key);
            if (s == null) return 0;
            try {
                return Integer.parseInt(s);
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static long secondsUntilEndOfDay() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime end = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        return Duration.between(now, end).getSeconds();
    }
}
