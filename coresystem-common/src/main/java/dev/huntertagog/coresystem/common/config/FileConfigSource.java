package dev.huntertagog.coresystem.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.config.ConfigSource;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public record FileConfigSource(File file) implements ConfigSource {

    private static final Logger LOG = LoggerFactory.get("FileConfigSource");

    @Override
    public Map<String, String> load() {
        Map<String, String> result = new HashMap<>();

        if (!file.exists()) {
            // Kein Fehlerfall – Datei ist optional, wir loggen nur informativ.
            LOG.info("Config file {} does not exist; skipping.", file.getAbsolutePath());
            return result;
        }

        try {
            String content = Files.readString(file.toPath());
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            flatten("", root, result);
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.CONFIG_RELOAD_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to load JSON config file.",
                    e,
                    Map.of(
                            "file", file.getAbsolutePath()
                    )
            );
            LOG.error(error.toLogString(), e);
        }

        return result;
    }

    private void flatten(String prefix, JsonObject obj, Map<String, String> out) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement val = e.getValue();

            try {
                if (val.isJsonObject()) {
                    flatten(key, val.getAsJsonObject(), out);
                } else if (!val.isJsonNull()) {
                    out.put(key, val.getAsString());
                }
            } catch (Exception ex) {
                CoreError error = new CoreError(
                        CoreErrorCode.CONFIG_PARSE_FAILED,
                        CoreErrorSeverity.WARN,
                        "Failed to flatten JSON config entry.",
                        ex,
                        Map.of(
                                "file", file.getAbsolutePath(),
                                "key", key,
                                "value", val.toString()
                        )
                );
                LOG.warn(error.toLogString(), ex);
            }
        }
    }

    @Override
    public int priority() {
        return 100;
    }
}
