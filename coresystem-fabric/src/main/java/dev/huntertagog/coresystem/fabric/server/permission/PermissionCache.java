package dev.huntertagog.coresystem.fabric.server.permission;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.model.ServerTargets;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionsUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kleiner In-Memory-Cache für Spieler-Permissions (Level).
 * <p>
 * Backend:
 * - Level wird über das zentrale PermissionService/PermissionsUtil ermittelt
 * - hier nur kurzzeitig gecacht, um permanente Permission-Checks zu vermeiden
 */
public final class PermissionCache {

    private static final Logger LOG = LoggerFactory.get("PermissionCache");

    /**
     * TTL in Millisekunden.
     * 5 Sekunden sind kurz genug für dynamische Änderungen,
     * reduzieren aber Permission-Backend-Calls spürbar.
     */
    private static final long TTL_MILLIS = 5_000L;

    private static final Map<UUID, CachedEntry> CACHE = new ConcurrentHashMap<>();

    private PermissionCache() {
    }

    private record CachedEntry(ServerTargets.Level level, long expiresAt) {
        boolean isExpired(long now) {
            return now >= expiresAt;
        }
    }

    /**
     * Liefert den gecachten Permission-Level für den Spieler
     * bzw. berechnet ihn neu, wenn kein Eintrag oder TTL abgelaufen.
     */
    @NotNull
    public static ServerTargets.Level getLevel(@NotNull ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();

        CachedEntry entry = CACHE.get(uuid);
        if (entry != null && !entry.isExpired(now)) {
            return entry.level();
        }

        // neu auflösen über das zentrale Permission-System
        ServerTargets.Level resolved;
        try {
            resolved = PermissionsUtil.resolveLevel(player);

            if (resolved == null) {
                CoreError error = CoreError.of(
                        CoreErrorCode.PERMISSION_CHECK_FAILED,   // oder ein spezieller Code für „Level-Resolve“
                        CoreErrorSeverity.WARN,
                        "PermissionsUtil.resolveLevel returned null – falling back to DEFAULT."
                ).withContextEntry("player", player.getGameProfile().getName());

                LOG.warn(error.toLogString());
                resolved = ServerTargets.Level.DEFAULT;
            }

        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while resolving permission level for player."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            // konservativer Fallback
            resolved = ServerTargets.Level.DEFAULT;
        }

        CACHE.put(uuid, new CachedEntry(resolved, now + TTL_MILLIS));
        return resolved;
    }

    // --------------------------------------------------------
    // Convenience-Abfragen
    // --------------------------------------------------------

    public static boolean isAdmin(@NotNull ServerPlayerEntity player) {
        return getLevel(player) == ServerTargets.Level.ADMIN;
    }

    public static boolean isVip(@NotNull ServerPlayerEntity player) {
        ServerTargets.Level level = getLevel(player);
        return level == ServerTargets.Level.VIP || level == ServerTargets.Level.ADMIN;
    }

    public static boolean isDefault(@NotNull ServerPlayerEntity player) {
        return getLevel(player) == ServerTargets.Level.DEFAULT;
    }

    // --------------------------------------------------------
    // Invalidation-Hooks
    // --------------------------------------------------------

    /**
     * Invalidate für einen Spieler (z. B. bei LP-Reload / Dimension-Change).
     */
    public static void invalidate(@NotNull UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void invalidate(@NotNull ServerPlayerEntity player) {
        invalidate(player.getUuid());
    }

    /**
     * Kann z. B. beim Server-Stop oder globalem Reload aufgerufen werden.
     */
    public static void clear() {
        CACHE.clear();
    }
}
