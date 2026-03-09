package dev.huntertagog.coresystem.common.config;

import dev.huntertagog.coresystem.platform.config.ConfigKey;

public final class CoreConfigKeys {

    private CoreConfigKeys() {
    }

    // Islands
    public static final ConfigKey<Integer> ISLANDS_MAX_PER_NODE =
            ConfigKey.integer("islands.max_islands_per_node", 50);

    public static final ConfigKey<Integer> ISLANDS_MAX_PLAYERS_PER_ISLAND =
            ConfigKey.integer("islands.max_players_per_island", 4);

    // Node Identity
    public static final ConfigKey<String> NODE_ID =
            ConfigKey.string("node.id", "node-1");

    public static final ConfigKey<String> NODE_ROLE =
            ConfigKey.string("node.role", "island"); // z.B. "proxy", "lobby", "island"

    // PlayerProfile / Analytics Flags
    public static final ConfigKey<Boolean> PLAYER_PROFILE_ENABLED =
            ConfigKey.bool("player_profile.enabled", true);

    public static final ConfigKey<Long> PLAYER_PROFILE_RETENTION_DAYS =
            ConfigKey.longKey("player_profile.retention_days", 90L);
}
