package dev.huntertagog.coresystem.common.friends.gui;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendSettingsStore {

    public record Settings(boolean allowRequests, boolean allowFollow, boolean showLastSeen) {
    }

    private static final ConcurrentHashMap<UUID, Settings> MEM = new ConcurrentHashMap<>();

    private FriendSettingsStore() {
    }

    public static Settings get(UUID playerId) {
        // Defaults
        return MEM.getOrDefault(playerId, new Settings(true, true, true));
    }

    public static void set(UUID playerId, boolean allowRequests, boolean allowFollow, boolean showLastSeen) {
        MEM.put(playerId, new Settings(allowRequests, allowFollow, showLastSeen));
        // TODO: persist in Redis/PlayerSettings KV
    }
}
