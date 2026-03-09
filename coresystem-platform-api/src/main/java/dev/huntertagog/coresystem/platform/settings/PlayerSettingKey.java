package dev.huntertagog.coresystem.platform.settings;

import java.util.Objects;
import java.util.function.Function;

public final class PlayerSettingKey<T> {

    private final String key;
    private final Class<T> type;
    private final T defaultValue;
    private final Function<String, T> parser;
    private final Function<T, String> serializer;

    private PlayerSettingKey(String key,
                             Class<T> type,
                             T defaultValue,
                             Function<String, T> parser,
                             Function<T, String> serializer) {
        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = defaultValue;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
    }

    public String key() {
        return key;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public T parse(String raw) {
        return parser.apply(raw);
    }

    public String serialize(T value) {
        if (value == null) {
            return "";
        }
        return serializer.apply(value);
    }

    // ------------ Factory-Methoden ------------

    public static PlayerSettingKey<Boolean> bool(String key, boolean defaultValue) {
        return new PlayerSettingKey<>(
                key,
                Boolean.class,
                defaultValue,
                raw -> raw == null || raw.isEmpty() ? defaultValue : Boolean.parseBoolean(raw),
                value -> Boolean.toString(Boolean.TRUE.equals(value))
        );
    }

    public static PlayerSettingKey<Integer> integer(String key, int defaultValue) {
        return new PlayerSettingKey<>(
                key,
                Integer.class,
                defaultValue,
                raw -> {
                    if (raw == null || raw.isEmpty()) return defaultValue;
                    try {
                        return Integer.parseInt(raw);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                },
                value -> Integer.toString(value != null ? value : defaultValue)
        );
    }

    public static PlayerSettingKey<String> string(String key, String defaultValue) {
        return new PlayerSettingKey<>(
                key,
                String.class,
                defaultValue,
                raw -> raw == null ? defaultValue : raw,
                value -> value != null ? value : ""
        );
    }

    // Falls du später JSON möchtest, kannst du hier mit Gson/Jackson arbeiten
    public static PlayerSettingKey<String> json(String key, String defaultJson) {
        return string(key, defaultJson);
    }
}
