package dev.huntertagog.coresystem.platform.settings;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSettingsService extends Service {

    <T> Optional<T> get(UUID playerId, PlayerSettingKey<T> key);

    <T> T getOrDefault(UUID playerId, PlayerSettingKey<T> key);

    <T> void set(UUID playerId, PlayerSettingKey<T> key, T value);

    void clear(UUID playerId, PlayerSettingKey<?> key);

    /**
     * Rohansicht aller Settings (key -> rawValue), vor allem für Debug/Export.
     */
    Map<String, String> getAllRaw(UUID playerId);
}
