package dev.huntertagog.coresystem.fabric.common.teleport;

import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TeleportManagerService extends Service {

    /**
     * Cross-Server-Teleport:
     * 1) Inventory-Snapshot persistieren (via PlayerDataSyncService)
     * 2) PendingTeleport in Redis hinterlegen
     * 3) Proxy-Command ausführen (Velocity / Bungee / etc.)
     */
    boolean teleportPlayer(@NotNull ServerPlayerEntity player,
                           @NotNull String targetServer,
                           @Nullable String inventoryContext,
                           @Nullable String reason);

    /**
     * Wird vom Join-Listener aufgerufen:
     * - Prüft, ob ein PendingTeleport für diesen Spieler existiert
     * - Validiert, ob dieser Server der Zielserver ist
     * - Wendet InventorySnapshot an (PlayerDataSyncService)
     * - Räumt Redis auf
     *
     * @return true, wenn ein PendingTeleport verarbeitet wurde.
     */
    boolean handleJoinOnThisServer(@NotNull ServerPlayerEntity player);
}
