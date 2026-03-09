package dev.huntertagog.coresystem.platform.ratelimit;

public record RateLimitRule(
        String id,          // z. B. "teleport:island"
        int maxActions,     // z. B. 3 Aktionen ...
        long intervalMillis // ... pro 30_000 ms (30s)
) {
}
