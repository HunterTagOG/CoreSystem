package dev.huntertagog.coresystem.common.config;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.config.ConfigSource;

import java.util.HashMap;
import java.util.Map;

public record EnvConfigSource(String prefix) implements ConfigSource {

    private static final Logger LOG = LoggerFactory.get("EnvConfigSource");

    @Override
    public Map<String, String> load() {
        Map<String, String> result = new HashMap<>();

        Map<String, String> env;
        try {
            env = System.getenv();
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.CONFIG_RELOAD_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Unable to read environment variables.",
                    e,
                    Map.of("prefix", prefix)
            );
            LOG.error(error.toLogString(), e);
            return result; // leer → Config bleibt stabil
        }

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            try {
                if (!key.startsWith(prefix)) continue;

                String stripped = key.substring(prefix.length());                // ISLANDS_MAX_ISLANDS_PER_NODE
                String normalized = stripped.toLowerCase().replace('_', '.');   // islands.max_islands_per_node

                result.put(normalized, entry.getValue());
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.CONFIG_PARSE_FAILED,
                        CoreErrorSeverity.WARN,
                        "Failed to normalize environment variable key.",
                        e,
                        Map.of(
                                "originalKey", key,
                                "prefix", prefix
                        )
                );
                LOG.warn(error.toLogString(), e);
            }
        }

        return result;
    }

    @Override
    public int priority() {
        return 200;
    }
}
