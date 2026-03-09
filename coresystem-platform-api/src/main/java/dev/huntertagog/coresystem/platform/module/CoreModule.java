package dev.huntertagog.coresystem.platform.module;

/**
 * Bekannte Kernmodule des CoreSystems.
 * Erweiterbar, aber gute Basis für Registry & Health.
 */
public enum CoreModule {

    PLAYER_PROFILE("player-profile", "Player profiles & PlayerContext"),
    ECONOMY("economy", "Currency & Wallet system"),
    ISLANDS("islands", "Private islands & nodes"),
    MESSAGING("messaging", "Player messaging / notifications"),
    METRICS("metrics", "Telemetry & metrics"),
    AUDIT("audit", "Audit logging for commands & economy"),
    HEALTH("health", "Health checks / self-diagnose"),
    DEVTOOLS("devtools", "Developer & testing tools (simulations, debug commands)");

    private final String id;
    private final String description;

    CoreModule(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public static CoreModule byId(String id) {
        for (CoreModule m : values()) {
            if (m.id.equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }
}
