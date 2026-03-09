package dev.huntertagog.coresystem.platform.config;

public final class ConfigKey<T> {

    private final String path;          // z.B. "islands.max_islands_per_node"
    private final ConfigValueType type; // STRING, INT, BOOLEAN, ...
    private final T defaultValue;

    private ConfigKey(String path, ConfigValueType type, T defaultValue) {
        this.path = path;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public static ConfigKey<String> string(String path, String defaultValue) {
        return new ConfigKey<>(path, ConfigValueType.STRING, defaultValue);
    }

    public static ConfigKey<Integer> integer(String path, int defaultValue) {
        return new ConfigKey<>(path, ConfigValueType.INT, defaultValue);
    }

    public static ConfigKey<Boolean> bool(String path, boolean defaultValue) {
        return new ConfigKey<>(path, ConfigValueType.BOOLEAN, defaultValue);
    }

    public static ConfigKey<Long> longKey(String path, long defaultValue) {
        return new ConfigKey<>(path, ConfigValueType.LONG, defaultValue);
    }

    public String path() {
        return path;
    }

    public ConfigValueType type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }
}

