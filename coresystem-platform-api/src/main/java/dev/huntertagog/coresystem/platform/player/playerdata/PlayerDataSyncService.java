package dev.huntertagog.coresystem.platform.player.playerdata;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Optional;
import java.util.UUID;

public interface PlayerDataSyncService extends Service {

    void saveInventorySnapshot(UUID playerId,
                               String context,
                               String reason);

    Optional<PlayerInventorySnapshot> findInventorySnapshot(UUID playerId,
                                                            String context);

    boolean applyInventorySnapshot(UUID playerId,
                                   String context,
                                   boolean clearAfter);

    void deleteInventorySnapshot(UUID playerId,
                                 String context);
}
