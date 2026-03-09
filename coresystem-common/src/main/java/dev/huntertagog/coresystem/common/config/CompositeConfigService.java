package dev.huntertagog.coresystem.common.config;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.config.ConfigKey;
import dev.huntertagog.coresystem.platform.config.ConfigService;
import dev.huntertagog.coresystem.platform.config.ConfigSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CompositeConfigService implements ConfigService {

    private static final Logger LOG = LoggerFactory.get("ConfigService");

    private final List<ConfigSource> sources;
    private final Map<String, String> merged = new ConcurrentHashMap<>();

    public CompositeConfigService(List<ConfigSource> sources) {
        this.sources = new ArrayList<>(sources);
        this.sources.sort(Comparator.comparingInt(ConfigSource::priority));
        reload();
    }

    public synchronized void reload() {
        merged.clear();

        for (ConfigSource source : sources) {
            try {
                Map<String, String> data = source.load();
                if (data == null || data.isEmpty()) {
                    continue;
                }

                for (Map.Entry<String, String> e : data.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    merged.put(key, value);
                }
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.CONFIG_RELOAD_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to load config source.",
                        e,
                        Map.of(
                                "sourceClass", source.getClass().getName(),
                                "priority", source.priority()
                        )
                );
                LOG.error(error.toLogString(), e);
            }
        }

        LOG.info("ConfigService reloaded; {} keys active.", merged.size());
    }

    @Override
    public <T> T get(ConfigKey<T> key) {
        return getOptional(key).orElse(key.defaultValue());
    }

    @Override
    public <T> T getOrNull(ConfigKey<T> key) {
        return getOptional(key).orElse(null);
    }

    @Override
    public <T> Optional<T> getOptional(ConfigKey<T> key) {
        String raw = merged.get(key.path());
        if (raw == null) {
            return Optional.empty();
        }

        try {
            T t = getT(key, raw);
            return Optional.of(t);
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.CONFIG_PARSE_FAILED,
                    CoreErrorSeverity.WARN,
                    "Failed to parse config value; falling back to default.",
                    e,
                    Map.of(
                            "path", key.path(),
                            "raw", raw,
                            "type", key.type().name()
                    )
            );
            LOG.warn(error.toLogString(), e);
            return Optional.empty();
        }
    }

    private <T> T getT(ConfigKey<T> key, String raw) {
        Object converted = switch (key.type()) {
            case STRING -> raw;
            case INT -> Integer.parseInt(raw);
            case LONG -> Long.parseLong(raw);
            case BOOLEAN -> {
                if ("1".equals(raw)) yield true;
                if ("0".equals(raw)) yield false;
                yield Boolean.parseBoolean(raw);
            }
            case DOUBLE -> Double.parseDouble(raw);
        };
        @SuppressWarnings("unchecked")
        T t = (T) converted;
        return t;
    }
}
