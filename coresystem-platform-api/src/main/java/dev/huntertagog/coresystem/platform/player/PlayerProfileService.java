package dev.huntertagog.coresystem.platform.player;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileService extends Service {


    PlayerProfile getOrCreate(
            UUID uniqueId,
            String fallbackName
    );


    Optional<PlayerProfile> find(UUID uniqueId);

    /**
     * Wird beim Join aufgerufen
     */

    PlayerProfile updateOnJoin(
            UUID uniqueId,
            String playerName,
            String currentServerName,
            String nodeId
    );

    /**
     * Wird beim Quit aufgerufen
     */

    Optional<PlayerProfile> updateOnQuit(
            UUID uniqueId,
            String currentServerName,
            String nodeId
    );
}
