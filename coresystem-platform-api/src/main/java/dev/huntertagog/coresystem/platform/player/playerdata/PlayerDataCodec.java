package dev.huntertagog.coresystem.platform.player.playerdata;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.io.IOException;
import java.util.UUID;

public interface PlayerDataCodec extends Service {

    /**
     * Encode current inventory of a player into base64 (or bytes)
     */
    String encodeInventoryToBase64(UUID playerId) throws IOException;

    /**
     * Apply inventory payload to a player
     */
    void applyInventoryFromBase64(UUID playerId, String payloadBase64) throws IOException;
}
