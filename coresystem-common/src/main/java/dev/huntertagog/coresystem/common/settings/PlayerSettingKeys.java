package dev.huntertagog.coresystem.common.settings;

import dev.huntertagog.coresystem.platform.settings.PlayerSettingKey;

public final class PlayerSettingKeys {

    private PlayerSettingKeys() {
    }

    // Chat / Notifications
    public static final PlayerSettingKey<Boolean> CHAT_MENTIONS_SOUND =
            PlayerSettingKey.bool("chat.mentions.sound", true);

    public static final PlayerSettingKey<Boolean> CHAT_SYSTEM_MESSAGES =
            PlayerSettingKey.bool("chat.system.enabled", true);

    // Teleports / GUIs
    public static final PlayerSettingKey<Boolean> TELEPORT_CONFIRM_ISLAND =
            PlayerSettingKey.bool("teleport.island.confirm", false);

    public static final PlayerSettingKey<Boolean> TELEPORT_ANIMATION_ENABLED =
            PlayerSettingKey.bool("teleport.animation.enabled", true);

    // Economy
    public static final PlayerSettingKey<Boolean> ECONOMY_BALANCE_AUTOMSG =
            PlayerSettingKey.bool("economy.balance.autoshow", true);

    // Sprache / Locale
    public static final PlayerSettingKey<String> UI_LOCALE =
            PlayerSettingKey.string("ui.locale", "auto");

    // … hier kannst du nach und nach weitere Settings ergänzen
}
