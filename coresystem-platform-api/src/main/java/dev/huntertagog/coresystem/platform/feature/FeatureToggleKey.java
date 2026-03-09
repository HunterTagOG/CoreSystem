package dev.huntertagog.coresystem.platform.feature;

import dev.huntertagog.coresystem.platform.module.CoreModule;

public enum FeatureToggleKey {

    // Economy
    ECONOMY_ENABLED("economy.enabled", CoreModule.ECONOMY, true, "Global toggle for economy system"),
    ECONOMY_TRANSFERS_ENABLED("economy.transfers.enabled", CoreModule.ECONOMY, true, "Enables /pay / transfers"),

    // Health
    HEALTH_COMMAND_ENABLED("health.command.enabled", CoreModule.HEALTH, true, "Enables /cs health command"),

    // Audit
    AUDIT_COMMANDS_ENABLED("audit.commands.enabled", CoreModule.AUDIT, true, "Logs staff/admin commands to audit log"),
    AUDIT_ECONOMY_ENABLED("audit.economy.enabled", CoreModule.AUDIT, true, "Logs economy transactions to audit log"),

    // DevTools
    DEVTOOLS_ENABLED("devtools.enabled", CoreModule.DEVTOOLS, false, "Enables /cs dev tools");

    private final String key;
    private final CoreModule module;
    private final boolean defaultEnabled;
    private final String description;

    FeatureToggleKey(String key, CoreModule module, boolean defaultEnabled, String description) {
        this.key = key;
        this.module = module;
        this.defaultEnabled = defaultEnabled;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public CoreModule module() {
        return module;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public String description() {
        return description;
    }
}
