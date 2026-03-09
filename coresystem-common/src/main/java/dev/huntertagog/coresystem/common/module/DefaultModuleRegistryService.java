package dev.huntertagog.coresystem.common.module;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.module.CoreModule;
import dev.huntertagog.coresystem.platform.module.ModuleRegistryService;
import dev.huntertagog.coresystem.platform.module.ModuleState;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class DefaultModuleRegistryService implements ModuleRegistryService {

    private static final Logger LOG = LoggerFactory.get("ModuleRegistry");

    private final Map<CoreModule, ModuleState> states = new EnumMap<>(CoreModule.class);

    @Override
    public synchronized void registerModule(CoreModule module,
                                            boolean enabledByDefault,
                                            String reasonIfDisabled) {

        // Environment Override:
        // CORESYSTEM_MODULE_<ID>=true/false
        String envKey = "CORESYSTEM_MODULE_" + module.id().toUpperCase().replace('-', '_');
        String envVal = System.getenv(envKey);

        boolean enabled = enabledByDefault;
        String reason = reasonIfDisabled;

        if (envVal != null) {
            enabled = Boolean.parseBoolean(envVal);
            reason = "overridden via ENV " + envKey + "=" + envVal;
        }

        ModuleState state = new ModuleState(module, enabled, reason);
        states.put(module, state);

        LOG.info("Module '{}' registered: enabled={} reason={}", module.id(), enabled, reason);
    }

    @Override
    public synchronized void setModuleEnabled(CoreModule module, boolean enabled, String reason) {
        ModuleState current = states.getOrDefault(module, new ModuleState(module, enabled, null));
        ModuleState updated = current.withEnabled(enabled, reason);
        states.put(module, updated);

        LOG.info("Module '{}' set to enabled={} reason={}", module.id(), enabled, reason);
    }

    @Override
    public synchronized ModuleState getState(CoreModule module) {
        return states.getOrDefault(module, new ModuleState(module, false, "not-registered"));
    }

    @Override
    public synchronized Collection<ModuleState> getAllStates() {
        return Collections.unmodifiableCollection(states.values());
    }
}
