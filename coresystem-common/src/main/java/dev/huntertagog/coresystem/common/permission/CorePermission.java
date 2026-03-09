package dev.huntertagog.coresystem.common.permission;

public enum CorePermission {

    ADMIN_SRUCTURES(PermissionKeys.ADMIN_SRUCTURES, "Strukturen verwalten", "admin"),

    HOME_USE(PermissionKeys.HOME_USE, "Nutzung des /home-Kommandos", "default"),
    HOME_SET(PermissionKeys.HOME_SET, "Setzen des /sethome", "trusted"),

    ISLAND_CREATE(PermissionKeys.ISLAND_CREATE, "Neue Island-Welt anlegen", "default"),
    ISLAND_INVITE(PermissionKeys.ISLAND_INVITE, "Spieler auf die eigene Island einladen", "member"),
    ISLAND_ADMIN(PermissionKeys.ISLAND_ADMIN, "Admin-Operations auf allen Islands", "admin"),

    STAFF_RELOAD(PermissionKeys.STAFF_RELOAD, "CoreSystem reloaden", "admin"),
    STAFF_DEBUG(PermissionKeys.STAFF_DEBUG, "Debug-Kommandos nutzen", "admin"),

    SERVERSWITCH_DEFAULT(PermissionKeys.SERVERSWITCH_DEFAULT, "Zugriff auf den Standard-Serverswitch", "default"),
    SERVERSWITCH_VIP(PermissionKeys.SERVERSWITCH_VIP, "Zugriff auf den VIP-Serverswitch", "vip"),
    SERVERSWITCH_ADMIN(PermissionKeys.SERVERSWITCH_ADMIN, "Zugriff auf den Admin-Serverswitch", "admin"),
    STAFF_CHAT_FILTER_BYPASS(PermissionKeys.STAFF_CHAT_FILTER_BYPASS, "Bypass chat filter (see unfiltered messages)", "admin"),
    REGION_ADMIN(PermissionKeys.REGION_ADMIN, "Regionen verwalten", "admin"),
    PLAYER_CLAN_INFO(PermissionKeys.PLAYER_CLAN_INFO, "Clan-Info von anderen Spielern ansehen", "default"),
    PLAYER_CLAN_LEAVE(PermissionKeys.PLAYER_CLAN_INFO, "Clan verlassen", "member"),
    PLAYER_CLAN_JOIN(PermissionKeys.PLAYER_CLAN_INFO, "Clan beitreten", "default"),
    PLAYER_CLAN_INVITE(PermissionKeys.PLAYER_CLAN_INFO, "Clan-Mitglieder einladen", "member"),
    PLAYER_CLAN_CREATE(PermissionKeys.PLAYER_CLAN_INFO, "Einen neuen Clan erstellen", "default"),
    PLAYER_CLAN_CHAT(PermissionKeys.PLAYER_CLAN_CHAT, "Clan Chat togglen", "default"),
    STAFF_CHAT_SPY(PermissionKeys.STAFF_CHAT_ADMIN, "Admin Clan Chat Spy", "admin"),
    STAFF_ADMIN(PermissionKeys.STAFF_ADMIN, "Admin Prefix", "admin"),
    STAFF_MOD(PermissionKeys.STAFF_MOD, "Mod Prefix", "mod"),
    VIP(PermissionKeys.VIP, "Vip Prefix", "vip");

    private final String key;
    private final String description;
    private final String defaultGroup;

    CorePermission(String key, String description, String defaultGroup) {
        this.key = key;
        this.description = description;
        this.defaultGroup = defaultGroup;
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    public String defaultGroup() {
        return defaultGroup;
    }
}
