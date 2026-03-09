package dev.huntertagog.coresystem.common.ratelimit;

import dev.huntertagog.coresystem.platform.ratelimit.RateLimitRule;

public final class RateLimitRules {

    private RateLimitRules() {
    }

    // Beispiel: maximal 3 Island-Teleports in 30 Sekunden
    public static final RateLimitRule ISLAND_TELEPORT =
            new RateLimitRule("teleport:island", 3, 30_000L);

    // Beispiel: 1 Health-Check alle 10 Sekunden
    public static final RateLimitRule CORE_HEALTH_CHECK =
            new RateLimitRule("command:cs_health", 1, 10_000L);

    // Beispiel: generischer Teleport
    public static final RateLimitRule GENERIC_TELEPORT =
            new RateLimitRule("teleport:generic", 5, 20_000L);

    // Beispiel: Chat-Flood pro Spieler (kannst du im ChatListener nutzen)
    public static final RateLimitRule CHAT_MESSAGE =
            new RateLimitRule("chat:message", 5, 5_000L);
}
