package dev.huntertagog.coresystem.platform.module;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Collection;

public interface ModuleRegistryService extends Service {

    /**
     * Registriert ein Modul mit einem Default-Enabled-Status.
     * Environment Overrides werden direkt berücksichtigt.
     */
    void registerModule(CoreModule module, boolean enabledByDefault, String reasonIfDisabled);

    /**
     * Setzt Status explizit (Server-Local).
     */
    void setModuleEnabled(CoreModule module, boolean enabled, String reason);

    /**
     * Liest aktuellen Status.
     */
    ModuleState getState(CoreModule module);

    /**
     * Shortcut.
     */
    default boolean isEnabled(CoreModule module) {
        return getState(module).enabled();
    }

    /**
     * Alle registrierten Module.
     */
    Collection<ModuleState> getAllStates();
}
