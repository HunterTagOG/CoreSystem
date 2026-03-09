package dev.huntertagog.coresystem.common.text;

public enum CoreMessage {

    // ---- COMMON ----
    ONLY_PLAYERS(CoreMessageGroup.COMMON, "coresystem.common.only_players"),
    STRUCTURE_RELOAD_SUCCESS(CoreMessageGroup.COMMON, "coresystem.command.cs.structures.reload.success"),

    // ---- WHEREAMI ----
    WHEREAMI_HEADER(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.header"),
    WHEREAMI_FOOTER(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.footer"),
    WHEREAMI_WORLD_ID(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.world_id"),
    WHEREAMI_DIM_TYPE(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.dim_type"),
    WHEREAMI_IS_ISLAND(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.is_island"),
    WHEREAMI_ISLAND_OWNER(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.island_owner"),
    WHEREAMI_ISLAND_FILESYSTEM(CoreMessageGroup.COMMAND_WHEREAMI, "coresystem.command.whereami.filesystem"),

    // ---- ISLAND NODES ----
    ISLAND_NODES_NONE(CoreMessageGroup.COMMAND_ISLAND_NODES, "coresystem.command.islandnodes.none"),
    ISLAND_NODES_HEADER(CoreMessageGroup.COMMAND_ISLAND_NODES, "coresystem.command.islandnodes.header"),
    ISLAND_NODES_LINE(CoreMessageGroup.COMMAND_ISLAND_NODES, "coresystem.command.islandnodes.line"),

    // ---- ISLAND DELETE ----
    ISLAND_DELETE_ONLY_PLAYERS(CoreMessageGroup.COMMAND_ISLAND_DELETE, "coresystem.command.rmisland.only_players"),
    ISLAND_DELETE_SUCCESS(CoreMessageGroup.COMMAND_ISLAND_DELETE, "coresystem.command.rmisland.success"),

    // ---- ISLAND ERROR / STATUS ----
    ISLAND_NO_WORLD_AVAILABLE(CoreMessageGroup.ISLAND_ERRORS, "coresystem.island.error.no_world_available"),
    ISLAND_DELETE_KICK(CoreMessageGroup.ISLAND_ERRORS, "coresystem.island.delete.kick"),
    ISLAND_NO_NODE_CAPACITY(CoreMessageGroup.ISLAND_ERRORS, "coresystem.island.node.no_capacity"),
    ISLAND_NODE_INTERNAL_ERROR(CoreMessageGroup.ISLAND_ERRORS, "coresystem.island.node.internal_error"),
    ISLAND_PREPARE_START(CoreMessageGroup.ISLAND_STATUS, "coresystem.island.prepare.start"),
    ISLAND_PREPARE_FAILED(CoreMessageGroup.ISLAND_STATUS, "coresystem.island.prepare.failed"),

    // --- PERMISSION DEBUG (/csperms) ---
    PERMDEBUG_ALL_HEADER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.all.header"),
    PERMDEBUG_USED_HEADER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.used.header"),
    PERMDEBUG_USED_NONE(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.used.none"),
    PERMDEBUG_MAPPING_HEADER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.mapping.header"),
    PERMDEBUG_MAPPING_EMPTY(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.mapping.empty"),
    PERMDEBUG_LINE(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.line"),
    PERMDEBUG_MAPPING_PERM_HEADER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.mapping.perm_header"),
    PERMDEBUG_MAPPING_COMMAND_LINE(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.mapping.command_line"),

    // NEU: Profil + Level Debug
    PERMDEBUG_PROFILE_HEADER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.header"),
    PERMDEBUG_PROFILE_NAME(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.name"),
    PERMDEBUG_PROFILE_UUID(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.uuid"),
    PERMDEBUG_PROFILE_FIRST_SEEN(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.first_seen"),
    PERMDEBUG_PROFILE_LAST_SEEN(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.last_seen"),
    PERMDEBUG_PROFILE_TOTAL_JOINS(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.total_joins"),
    PERMDEBUG_PROFILE_LAST_SERVER(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.last_server"),
    PERMDEBUG_PROFILE_LAST_NODE(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.last_node"),
    PERMDEBUG_PROFILE_SWITCH_LEVEL(CoreMessageGroup.COMMAND_PERMDEBUG, "coresystem.command.csperms.profile.switch_level"),

    // --- ITEMGROUPS ---
    ITEMGROUP_CORE_TAB(CoreMessageGroup.ITEMGROUPS, "coresystem.itemgroup.core_tab"),

    // --- ITEMS ---
    ITEM_SERVER_KEY_NAME(CoreMessageGroup.ITEMS, "item.coresystem.server_key"),
    ITEM_SERVER_KEY_LORE_1(CoreMessageGroup.ITEMS, "item.coresystem.server_key.lore_1"),
    ITEM_SERVER_KEY_LORE_2(CoreMessageGroup.ITEMS, "item.coresystem.server_key.lore_2"),

    // --- PlayerLifeCycle Events ---
    FIRST_JOIN_WELCOME_MESSAGE(CoreMessageGroup.PLAYER_LIFECYCLE, "coresystem.player.lifecycle.first_join_welcome_message"),
    JOIN_WELCOME_BACK_MESSAGE(CoreMessageGroup.PLAYER_LIFECYCLE, "coresystem.player.lifecycle.join_welcome_back_message"),
    HEALTH_HEADER(CoreMessageGroup.HEALTH, "coresystem.health.header"),
    HEALTH_NONE(CoreMessageGroup.HEALTH, "coresystem.health.none"),
    HEALTH_LINE(CoreMessageGroup.HEALTH, "coresystem.health.line"),
    HEALTH_FOOTER(CoreMessageGroup.HEALTH, "coresystem.health.footer"),

    FEATURE_DISABLED(CoreMessageGroup.FEATURE, "coresystem.common.feature.disabled"),
    FEATURE_DEVTOOLS_DISABLED(CoreMessageGroup.FEATURE, "coresystem.common.feature.devtools.disabled"),

    DEV_ENV_HEADER(CoreMessageGroup.DEV_ENV, "coresystem.devtools.env.header"),
    DEV_ENV_LINE(CoreMessageGroup.DEV_ENV, "coresystem.devtools.env.line"),
    DEV_SIM_JOIN_OK(CoreMessageGroup.DEV_ENV, "coresystem.devtools.sim.join.ok"),
    DEV_SIM_QUIT_OK(CoreMessageGroup.DEV_ENV, "coresystem.devtools.sim.quit.ok"),
    DEV_ECONOMY_SMOKE_HEADER(CoreMessageGroup.DEV_ENV, "coresystem.devtools.economy.smoke.header"),
    DEV_SIM_BURST_OK(CoreMessageGroup.DEV_ENV, "coresystem.devtools.sim.burst.ok"),
    DEV_REDIS_PING_FAILED(CoreMessageGroup.DEV_ENV, "coresystem.devtools.redis.ping.failed"),
    DEV_REDIS_PING_RESULT(CoreMessageGroup.DEV_ENV, "coresystem.devtools.redis.ping.result"),
    ISLAND_TELEPORTING(CoreMessageGroup.ISLAND_STATUS, "coresystem.island.teleporting"),
    RATE_LIMIT_HIT(CoreMessageGroup.COMMON, "coresystem.common.rate_limit.hit"),
    SERVICE_UNAVAILABLE(CoreMessageGroup.COMMON, "coresystem.common.service.unavailable"),
    NICK_CLEARED(CoreMessageGroup.NICK, "coresystem.nick.cleared"),
    NICK_INVALID_LENGTH(CoreMessageGroup.NICK, "coresystem.nick.invalid_length"),
    NICK_SET(CoreMessageGroup.NICK, "coresystem.nick.set"),
    CHATFILTER_LIST_ENTRY(CoreMessageGroup.CHATFILTER, "coresystem.chatfilter.list.entry"),
    CHATFILTER_LIST_HEADER(CoreMessageGroup.CHATFILTER, "coresystem.chatfilter.list.header"),
    CHATFILTER_LIST_EMPTY(CoreMessageGroup.CHATFILTER, "coresystem.chatfilter.list.empty"),
    CHATFILTER_REMOVED(CoreMessageGroup.CHATFILTER, "coresystem.chatfilter.removed"),
    CHATFILTER_ADDED(CoreMessageGroup.CHATFILTER, "coresystem.chatfilter.added"),
    PROTECT_TNT_BLOCKED(CoreMessageGroup.PROTECT, "coresystem.protect.tnt.blocked"),
    REGION_BLOCK_PLACE_DENY(CoreMessageGroup.REGION, "coresystem.region.block_place.deny"),
    REGION_BLOCK_BREAK_DENY(CoreMessageGroup.REGION, "coresystem.region.block_break.deny"),
    CLAN_INFO_MEMBERS(CoreMessageGroup.CLAN, "coresystem.clan.info.members"),
    CLAN_INFO_OWNER(CoreMessageGroup.CLAN, "coresystem.clan.info.owner"),
    CLAN_INFO_HEADER(CoreMessageGroup.CLAN, "coresystem.clan.info.header"),
    CLAN_NOT_IN_CLAN(CoreMessageGroup.CLAN, "coresystem.clan.not_in_clan"),
    CLAN_LEAVE_SUCCESS(CoreMessageGroup.CLAN, "coresystem.clan.leave_success"),
    CLAN_LEAVE_FAILED(CoreMessageGroup.CLAN, "coresystem.clan.leave_failed_owner"),
    CLAN_OWNER_CANNOT_LEAVE(CoreMessageGroup.CLAN, "coresystem.clan.owner_cannot_leave"),
    CLAN_JOIN_SUCCESS(CoreMessageGroup.CLAN, "coresystem.clan.join_success"),
    CLAN_JOIN_FAILED_NO_INVITE(CoreMessageGroup.CLAN, "coresystem.clan.join_failed_no_invite"),
    CLAN_NOT_FOUND_BY_TAG(CoreMessageGroup.CLAN, "coresystem.clan.not_found_by_tag"),
    CLAN_ALREADY_IN_CLAN(CoreMessageGroup.CLAN, "coresystem.clan.already_in_clan"),
    CLAN_INVITE_RECEIVED(CoreMessageGroup.CLAN, "coresystem.clan.invite_received"),
    CLAN_INVITE_SENT(CoreMessageGroup.CLAN, "coresystem.clan.invite_sent"),
    CLAN_INVITE_FAILED(CoreMessageGroup.CLAN, "coresystem.clan.invite_failed"),
    CLAN_INVITE_TARGET_OFFLINE(CoreMessageGroup.CLAN, "coresystem.clan.invite_target_offline"),
    CLAN_CREATE_SUCCESS(CoreMessageGroup.CLAN, "coresystem.clan.create_success"),
    CLAN_CREATE_FAILED(CoreMessageGroup.CLAN, "coresystem.clan.create_failed"),
    FRIEND_OFFLINE_HEADER(CoreMessageGroup.FRIEND, "coresystem.friend.offline.header"),
    FRIEND_OFFLINE_TOAST_TITLE(CoreMessageGroup.FRIEND, "coresystem.friend.offline.toast_title"),
    FRIEND_OFFLINE_LINE(CoreMessageGroup.FRIEND, "coresystem.friend.offline.line"),
    FRIEND_OFFLINE_MORE(CoreMessageGroup.FRIEND, "coresystem.friend.offline.more"),

    CLAN_INVITES_HEADER(CoreMessageGroup.CLAN, "coresystem.clan.invites.header"),
    CLAN_INVITES_TOAST_TITLE(CoreMessageGroup.CLAN, "coresystem.clan.invites.toast_title"),
    CLAN_INVITES_LINE(CoreMessageGroup.CLAN, "coresystem.clan.invites.line"),
    CLAN_INVITES_MORE(CoreMessageGroup.CLAN, "coresystem.clan.invites.more"),
    FOLLOW_DENIED(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.denied"),
    FOLLOW_DENIED_TARGET(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.denied_target"),
    FOLLOW_NO_REQUEST(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.no_request"),
    PLAYER_NOT_ONLINE(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.player_not_online"),
    FOLLOW_ACCEPTED_TARGET(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.accepted_target"),
    FOLLOW_ACCEPTED(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.accepted"),
    FOLLOW_ACCEPTED_BUT_UNKNOWN(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.accepted_but_unknown"),
    FOLLOW_TARGET_LOCATION_UNKNOWN(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.target.location_unknown"),
    FOLLOW_ACCEPT_RACE_DENY(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.accept_race_deny"),
    FOLLOW_REQUEST_SENT(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.request_sent"),
    FOLLOW_REQUEST_RECEIVED(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.request_received"),
    FOLLOW_ALREADY_REQUESTED(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.already_requested"),
    FOLLOW_DAILY_LIMIT(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.daily_limit"),
    FOLLOW_NOT_FRIENDS(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.not_friends"),
    FOLLOW_SELF_DENY(CoreMessageGroup.FRIEND_FOLLOW, "coresystem.friend.follow.self_deny"),
    NO_PERMISSION(CoreMessageGroup.COMMON, "coresystem.common.no_permission"),

    FRIEND_MSG_QUEUED(CoreMessageGroup.FRIEND, "coresystem.friend.msg.queued"),
    FRIEND_MSG_SENT(CoreMessageGroup.FRIEND, "coresystem.friend.msg.sent"),
    FRIEND_MSG_NOT_FRIENDS(CoreMessageGroup.FRIEND, "coresystem.friend.msg.not_friends"),
    PLAYER_UNKNOWN(CoreMessageGroup.PLAYER, "coresystem.player.unknown"),
    FRIEND_MSG_EMPTY(CoreMessageGroup.FRIEND, "coresystem.friend.msg.empty"),
    FRIEND_REQUESTS_OUTGOING(CoreMessageGroup.FRIEND, "coresystem.friend.requests.outgoing"),
    FRIEND_REQUESTS_INCOMING(CoreMessageGroup.FRIEND, "coresystem.friend.requests.incoming"),
    FRIEND_REQUESTS_HEADER(CoreMessageGroup.FRIEND, "coresystem.friend.requests.header"),
    FRIEND_LIST_HEADER(CoreMessageGroup.FRIEND, "coresystem.friend.list.header"),
    FRIEND_LIST_EMPTY(CoreMessageGroup.FRIEND, "coresystem.friend.list.empty"),
    FRIEND_REMOVED(CoreMessageGroup.FRIEND, "coresystem.friend.removed"),
    FRIEND_REMOVE_FAILED(CoreMessageGroup.FRIEND, "coresystem.friend.remove_failed"),
    FRIEND_DENIED_OTHER(CoreMessageGroup.FRIEND, "coresystem.friend.denied_other"),
    FRIEND_DENIED(CoreMessageGroup.FRIEND, "coresystem.friend.denied"),
    FRIEND_DENY_FAILED(CoreMessageGroup.FRIEND, "coresystem.friend.deny_failed"),
    FRIEND_ACCEPTED_OTHER(CoreMessageGroup.FRIEND, "coresystem.friend.accepted_other"),
    FRIEND_ACCEPTED(CoreMessageGroup.FRIEND, "coresystem.friend.accepted"),
    FRIEND_ACCEPT_FAILED(CoreMessageGroup.FRIEND, "coresystem.friend.accept_failed"),
    FRIEND_REQUEST_RECEIVED(CoreMessageGroup.FRIEND, "coresystem.friend.request_received"),
    FRIEND_REQUEST_SENT(CoreMessageGroup.FRIEND, "coresystem.friend.request_sent"),
    FRIEND_REQUEST_FAILED(CoreMessageGroup.FRIEND, "coresystem.friend.request_failed"),
    FRIEND_ALREADY(CoreMessageGroup.FRIEND, "coresystem.friend.already"),
    FRIEND_SELF_DENY(CoreMessageGroup.FRIEND, "coresystem.friend.self_deny"),
    CLAN_CHAT_DISABLED(CoreMessageGroup.CLAN, "coresystem.clan.chat.disabled"),
    CLAN_CHAT_ENABLED(CoreMessageGroup.CLAN, "coresystem.clan.chat.enabled"),
    INVALID_ARGUMENT(CoreMessageGroup.CLAN, "coresystem.common.invalid_argument");

    private final CoreMessageGroup group;
    private final String key;

    CoreMessage(CoreMessageGroup group, String key) {
        this.group = group;
        this.key = key;
    }

    public String key() {
        return key;
    }

    public CoreMessageGroup group() {
        return group;
    }

}
