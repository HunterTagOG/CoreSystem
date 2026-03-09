package dev.huntertagog.coresystem.fabric.common.ratelimit;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import dev.huntertagog.coresystem.platform.ratelimit.RateLimitRule;
import dev.huntertagog.coresystem.platform.ratelimit.RateLimitService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class RateLimitUtil {

    private RateLimitUtil() {
    }

    /**
     * Rate-Limit check per Spieler. Wenn geblockt:
     * - schickt Feedback an den Spieler
     * - optional: Metrics
     *
     * @return true, wenn Aktion erlaubt; false, wenn geblockt.
     */
    public static boolean checkPlayerRateLimit(ServerCommandSource source,
                                               ServerPlayerEntity player,
                                               RateLimitRule rule,
                                               String logicalKeySuffix) {

        RateLimitService rl = ServiceProvider.getService(RateLimitService.class);
        if (rl == null) {
            // kein RateLimitService registriert -> kein Limit aktiv
            return true;
        }

        String key = player.getUuid() + ":" + logicalKeySuffix;

        boolean allowed = rl.tryAcquire(key, rule);
        if (allowed) {
            return true;
        }

        long retryMs = rl.getRetryAfterMillis(key, rule);
        long retrySec = TimeUnit.MILLISECONDS.toSeconds(retryMs);

        // Optionale Metrics
        MetricsService metrics = ServiceProvider.getService(MetricsService.class);
        if (metrics != null) {
            metrics.incrementCounter("ratelimit.blocked", 1L, Map.of(
                    "rule", rule.id(),
                    "player", player.getUuidAsString()
            ));
        }

        // Spieler-Feedback – definiere z. B. CoreMessage.RATE_LIMIT_HIT
        // "Du führst diese Aktion zu häufig aus. Bitte warte {0} Sekunden."
        source.sendFeedback(
                () -> Messages.tp(CoreMessage.RATE_LIMIT_HIT, retrySec),
                false
        );

        return false;
    }
}
