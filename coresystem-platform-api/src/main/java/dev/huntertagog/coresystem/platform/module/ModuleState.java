package dev.huntertagog.coresystem.platform.module;

/**
 * Zustand eines Moduls – ob es aktiv ist und warum (optional).
 */
public final class ModuleState {

    private final CoreModule module;
    private final boolean enabled;
    private final String reason;

    public ModuleState(CoreModule module, boolean enabled, String reason) {
        this.module = module;
        this.enabled = enabled;
        this.reason = reason;
    }

    public CoreModule module() {
        return module;
    }

    public boolean enabled() {
        return enabled;
    }

    public String reason() {
        return reason;
    }

    public ModuleState withEnabled(boolean enabled, String reason) {
        return new ModuleState(this.module, enabled, reason);
    }
}
