package dev.huntertagog.coresystem.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.huntertagog.coresystem.platform.ratelimit.RateLimitRule;
import dev.huntertagog.coresystem.platform.ratelimit.RateLimitService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryRateLimitService implements RateLimitService {

    private final Cache<String, List<Long>> buckets;

    public InMemoryRateLimitService() {
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(5))
                .maximumSize(50_000)
                .build();
    }

    private String bucketKey(String key, RateLimitRule rule) {
        return rule.id() + "|" + key;
    }

    @Override
    public boolean tryAcquire(String key, RateLimitRule rule) {
        long now = System.currentTimeMillis();
        String bucketKey = bucketKey(key, rule);

        List<Long> list = buckets.getIfPresent(bucketKey);
        if (list == null) {
            list = new ArrayList<>();
            buckets.put(bucketKey, list);
        }

        prune(list, now, rule.intervalMillis());

        if (list.size() >= rule.maxActions()) {
            return false;
        }

        list.add(now);
        return true;
    }

    @Override
    public long getRetryAfterMillis(String key, RateLimitRule rule) {
        long now = System.currentTimeMillis();
        String bucketKey = bucketKey(key, rule);

        List<Long> list = buckets.getIfPresent(bucketKey);
        if (list == null || list.isEmpty()) {
            return 0L;
        }

        prune(list, now, rule.intervalMillis());
        if (list.size() < rule.maxActions()) {
            return 0L;
        }

        long oldest = list.get(0);
        long allowedAt = oldest + rule.intervalMillis();
        long diff = allowedAt - now;
        return Math.max(diff, 0L);
    }

    private void prune(List<Long> list, long now, long intervalMillis) {
        long cutoff = now - intervalMillis;

        int idx = 0;
        int size = list.size();
        while (idx < size && list.get(idx) < cutoff) {
            idx++;
        }

        if (idx > 0) {
            list.subList(0, idx).clear();
        }
    }
}
