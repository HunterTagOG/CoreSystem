package dev.huntertagog.coresystem.platform.ratelimit;

import dev.huntertagog.coresystem.platform.provider.Service;

public interface RateLimitService extends Service {

    /**
     * Versucht, eine Aktion für "key" nach Regel "rule" zu registrieren.
     *
     * @return true, wenn erlaubt; false, wenn Rate-Limit greift.
     */
    boolean tryAcquire(String key, RateLimitRule rule);

    /**
     * Liefert (geschätzt) die verbleibende Wartezeit in ms, bis wieder
     * eine Aktion für "key" nach "rule" erlaubt wäre.
     * 0 oder <0 bedeutet: sofort wieder erlaubt.
     */
    long getRetryAfterMillis(String key, RateLimitRule rule);
}
