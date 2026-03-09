package dev.huntertagog.coresystem.common.clans.gui;

import java.util.UUID;

public final class ClanSettingsStore {

    private ClanSettingsStore() {
    }

    public record Settings(boolean openInvites, boolean friendlyFire, boolean clanChatDefault) {
    }

    // TODO: in Redis persistieren: cs:clan:<id>:settings
    public static Settings get(UUID clanId) {
        return new Settings(true, false, false);
    }

    public static void set(UUID clanId, boolean openInvites, boolean friendlyFire, boolean clanChatDefault) {
        // TODO persist
    }
}
