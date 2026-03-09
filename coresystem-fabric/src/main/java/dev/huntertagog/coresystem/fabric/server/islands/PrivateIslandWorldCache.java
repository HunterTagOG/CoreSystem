package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory-Cache, der speichert, ob ein Player sich aktuell auf
 * einer privaten Insel befindet. Wird von Movement/Teleport-Events
 * oder dem Island-Manager selbst gepflegt.
 */
public final class PrivateIslandWorldCache {

    private static final Logger LOG = LoggerFactory.get("IslandWorldCache");

    private static final Set<UUID> ON_PRIVATE_ISLAND =
            ConcurrentHashMap.newKeySet();

    private PrivateIslandWorldCache() {
    }

    // ------------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------------

    public static void setOnPrivateIsland(ServerPlayerEntity player, boolean onPrivateIsland) {
        try {
            UUID id = player.getUuid();

            if (onPrivateIsland) {
                ON_PRIVATE_ISLAND.add(id);
            } else {
                ON_PRIVATE_ISLAND.remove(id);
            }

        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_CACHE_FAILED,
                    CoreErrorSeverity.WARN,
                    "Failed to update island-cache entry.",
                    e,
                    Map.of(
                            "player", safeName(player),
                            "uuid", player != null ? player.getUuid().toString() : "null",
                            "targetState", onPrivateIsland
                    )
            );

            LOG.warn(error.toLogString(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Read: Einzelner Spieler
    // ------------------------------------------------------------------------

    public static boolean isOnPrivateIsland(ServerPlayerEntity player) {
        try {
            return ON_PRIVATE_ISLAND.contains(player.getUuid());
        } catch (Exception e) {

            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_CACHE_FAILED,
                    CoreErrorSeverity.WARN,
                    "Failed to read island-cache state.",
                    e,
                    Map.of(
                            "player", safeName(player),
                            "uuid", player != null ? player.getUuid().toString() : "null"
                    )
            );

            LOG.warn(error.toLogString(), e);
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Read: Liste aller Spieler auf Inseln
    // ------------------------------------------------------------------------

    public static List<UUID> getOnlinePlayersOnPrivateIslands(@NotNull MinecraftServer server) {
        try {
            List<UUID> result = new ArrayList<>();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (ON_PRIVATE_ISLAND.contains(p.getUuid())) {
                    result.add(p.getUuid());
                }
            }
            return result;

        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.ISLAND_CACHE_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to enumerate players on private islands.",
                    e,
                    Map.of(
                            "onlinePlayers", server.getPlayerManager().getCurrentPlayerCount()
                    )
            );

            LOG.error(error.toLogString(), e);
            return Collections.emptyList();
        }
    }

    private static String safeName(ServerPlayerEntity p) {
        return (p == null ? "null" : p.getGameProfile().getName());
    }
}
