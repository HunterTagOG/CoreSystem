package dev.huntertagog.coresystem.common.error;

public enum CoreErrorCode {

    // ---------------------------------------------------------------------
    // Generic / Core
    // ---------------------------------------------------------------------
    UNKNOWN,
    INVALID_ARGUMENT,
    MESSAGE_ERROR,

    // ---------------------------------------------------------------------
    // Service / DI
    // ---------------------------------------------------------------------
    SERVICE_MISSING,
    SERVICE_NOT_FOUND,
    SERVICE_ALREADY_REGISTERED,
    SCHEDULER_MISSING,
    DOMAIN_EVENT_HANDLER_FAILED,

    // ---------------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------------
    CONFIG_PARSE_FAILED,
    CONFIG_RELOAD_FAILED,

    // ---------------------------------------------------------------------
    // Redis / Infrastructure
    // ---------------------------------------------------------------------
    REDIS_FAILURE,
    REDIS_NO_CONNECTION,
    REDIS_INIT_FAILED,

    // ---------------------------------------------------------------------
    // Task / Scheduler
    // ---------------------------------------------------------------------
    TASK_EXECUTION_FAILED,
    TASK_SCHEDULER_SHUTDOWN_INTERRUPTED,
    TASK_SCHEDULER_SHUTDOWN_TIMEOUT,

    // ---------------------------------------------------------------------
    // Networking / Messaging / Bridge
    // ---------------------------------------------------------------------
    NETWORK_PAYLOAD_DECODE_FAILED,
    NETWORK_PAYLOAD_ENCODE_FAILED,
    NETWORK_SEND_FAILED,
    NETWORK_HANDLER_FAILED,
    NETWORK_INVALID_PAYLOAD_DATA,
    NETWORK_REGISTRATION_FAILED,
    SERVER_SWITCHER_OPEN_FAILED,

    // ---------------------------------------------------------------------
    // Commands / Permissions
    // ---------------------------------------------------------------------
    COMMAND_DISCOVERY_FAILED,
    COMMAND_PERMISSION_MAPPING_WARNING,
    COMMAND_REGISTRATION_FAILED,
    COMMAND_SUGGESTIONS_FAILED,
    PERMISSION_CHECK_FAILED,
    PERMISSION_BACKEND_NOT_AVAILABLE,

    // ---------------------------------------------------------------------
    // Player Context / Lifecycle
    // ---------------------------------------------------------------------
    PLAYERCONTEXT_INIT_FAILED,
    PLAYERCONTEXT_QUIT_FAILED,

    // ---------------------------------------------------------------------
    // Player Profile
    // ---------------------------------------------------------------------
    PLAYERPROFILE_SERVICE_MISSING,
    PLAYERPROFILE_FIND_FAILED,
    PLAYERPROFILE_LOAD_FAILED,
    PLAYERPROFILE_UPDATE_ON_JOIN_FAILED,
    PLAYERPROFILE_UPDATE_ON_QUIT_FAILED,
    PLAYERPROFILE_REDIS_LOAD_FAILED,
    PLAYERPROFILE_REDIS_SAVE_FAILED,

    // ---------------------------------------------------------------------
    // Player Data / Inventory
    // ---------------------------------------------------------------------
    PLAYERDATA_NBT_DECODE_FAILED,
    PLAYERDATA_NBT_ENCODE_FAILED,
    PLAYERDATA_REDIS_LOAD_FAILED,
    PLAYERDATA_REDIS_SAVE_FAILED,
    PLAYERDATA_REDIS_DELETE_FAILED,

    // ---------------------------------------------------------------------
    // Player Settings
    // ---------------------------------------------------------------------
    PLAYERSETTINGS_PARSE_FAILED,
    PLAYERSETTINGS_REDIS_LOAD_FAILED,
    PLAYERSETTINGS_REDIS_SAVE_FAILED,
    PLAYERSETTINGS_REDIS_DELETE_FAILED,

    // ---------------------------------------------------------------------
    // Messaging / Chat
    // ---------------------------------------------------------------------
    PLAYER_MESSAGE_SEND_FAILED,
    CHATFILTER_REDIS_LOAD_FAILED,
    CHATFILTER_REDIS_SAVE_FAILED,
    PLAYER_NICK_REDIS_SAVE_FAILED,

    // ---------------------------------------------------------------------
    // Friends
    // ---------------------------------------------------------------------
    FRIENDS_REDIS_FAILURE,
    FRIENDS_MESSAGE_PARSE_FAILED,

    // ---------------------------------------------------------------------
    // Clans
    // ---------------------------------------------------------------------
    CLAN_REDIS_FAILURE,
    CLAN_INVITE_MISSING,
    CLAN_ALREADY_IN_CLAN,
    CLAN_TAG_ALREADY_USED,
    CLAN_MEMBER_PARSE_FAILED,

    // ---------------------------------------------------------------------
    // Economy
    // ---------------------------------------------------------------------
    ECONOMY_BALANCE_LOAD_FAILED,
    ECONOMY_BALANCE_PARSE_FAILED,
    ECONOMY_EVENT_PUBLISH_FAILED,
    ECONOMY_TRANSACTION_FAILED,
    ECONOMY_TRANSACTION_LOG_FAILED,
    ECONOMY_TRANSACTION_SCRIPT_FAILED,

    // ---------------------------------------------------------------------
    // Metrics / Features
    // ---------------------------------------------------------------------
    METRICS_PARSE_FAILED,
    METRICS_WRITE_FAILED,
    FEATURE_TOGGLE_READ_FAILED,

    // ---------------------------------------------------------------------
    // Islands – Node / Routing
    // ---------------------------------------------------------------------
    ISLAND_NODE_CONFIG_FLAG_INVALID,
    ISLAND_NODE_CONFIG_HOSTNAME_RESOLVE_FAILED,
    ISLAND_NODE_HEARTBEAT_ALREADY_REGISTERED,
    ISLAND_NODE_HEARTBEAT_INIT_FAILED,
    ISLAND_NODE_HEARTBEAT_SEND_FAILED,
    ISLAND_NODE_LOAD_FAILED,
    ISLAND_NODE_LOAD_ALL_FAILED,
    ISLAND_NODE_SAVE_FAILED,
    ISLAND_NODE_STATUS_PARSE_FAILED,

    // ---------------------------------------------------------------------
    // Islands – Owner / Assignment
    // ---------------------------------------------------------------------
    ISLAND_OWNER_ASSIGN_REDIS_FAILED,
    ISLAND_OWNER_LIST_REDIS_FAILED,
    ISLAND_OWNER_LOAD_REDIS_FAILED,

    // ---------------------------------------------------------------------
    // Islands – World Lifecycle
    // ---------------------------------------------------------------------
    ISLAND_CREATE_FAILED,
    ISLAND_CACHE_FAILED,
    ISLAND_COUNTER_FAILED,
    ISLAND_DELETE_FAILED,
    ISLAND_DELETE_ENQUEUE_FAILED,
    ISLAND_WORLD_DELETE_TICK_FAILED,
    PRIVATE_ISLAND_CALL_ON_NON_ISLAND_NODE,
    PRIVATE_ISLAND_MAX_WORLDS_REACHED,
    PRIVATE_ISLAND_OVERWORLD_MISSING,
    PRIVATE_ISLAND_BORDER_CONFIG_FAILED,
    PRIVATE_ISLAND_WORLD_CREATION_FAILED,
    RUNTIME_WORLD_CONSTRUCTION_FAILED,
    RUNTIME_WORLD_OPEN_FAILED,
    WORLD_DIRECTORY_DELETE_FAILED,

    // ---------------------------------------------------------------------
    // Islands – Prepare / Workers
    // ---------------------------------------------------------------------
    ISLAND_PREPARE_WORKER_INVALID_MESSAGE,
    ISLAND_PREPARE_WORKER_PUBLISH_FAILED,
    ISLAND_PREPARE_WORKER_WORLD_FAILED,

    // ---------------------------------------------------------------------
    // Regions
    // ---------------------------------------------------------------------
    REGION_WORLDSTATE_LOAD_FAILED,

    // ---------------------------------------------------------------------
    // Structures
    // ---------------------------------------------------------------------
    STRUCTURE_RELOAD_FAILED
}
