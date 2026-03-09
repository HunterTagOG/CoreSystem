package dev.huntertagog.coresystem.common.config;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.config.ConfigSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class SystemPropertyConfigSource implements ConfigSource {

    private static final Logger LOG = LoggerFactory.get("SystemPropertyConfigSource");

    @Override
    public Map<String, String> load() {
        Map<String, String> result = new HashMap<>();

        try {
            Properties props = System.getProperties();

            for (String name : props.stringPropertyNames()) {
                try {
                    // Wir gehen weiterhin davon aus, dass relevante Keys bereits korrekt formatiert sind.
                    result.put(name, props.getProperty(name));
                } catch (Exception e) {
                    CoreError error = new CoreError(
                            CoreErrorCode.CONFIG_PARSE_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to read system property.",
                            e,
                            Map.of("property", name)
                    );
                    LOG.warn(error.toLogString(), e);
                }
            }

        } catch (Exception e) {
            // Gesamtausfall von System.getProperties() – extrem selten, aber sauber geloggt
            CoreError error = new CoreError(
                    CoreErrorCode.CONFIG_RELOAD_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to load system properties.",
                    e,
                    Map.of()
            );
            LOG.error(error.toLogString(), e);
        }

        return result;
    }

    @Override
    public int priority() {
        return 150;
    }
}
