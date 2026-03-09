package dev.huntertagog.coresystem.common.cache;

import dev.huntertagog.coresystem.common.islands.PrivateIslandWorldOwnerRepositoryRedis;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;

import java.util.UUID;

public final class IslandOwnerSuggestionCache {
    private static volatile java.util.List<String> snapshot = java.util.List.of();
    private static volatile long nextRefreshAt = 0L;

    private IslandOwnerSuggestionCache() {
    }

    public static java.util.List<String> current() {
        return snapshot;
    }

    // async refresh trigger (call from a scheduler)
    public static void refreshAsync(TaskScheduler scheduler) {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAt) return;
        nextRefreshAt = now + 30_000; // TTL 30s

        scheduler.runAsync(() -> {
            try {
                var repo = PrivateIslandWorldOwnerRepositoryRedis.get();
                snapshot = repo.getAllOwners(200).stream().map(UUID::toString).toList();
            } catch (Exception ignored) {
                // optional: log warn once
            }
        });
    }
}

