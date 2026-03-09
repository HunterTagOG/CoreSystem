package dev.huntertagog.coresystem.platform.config;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Optional;

public interface ConfigService extends Service {

    <T> T get(ConfigKey<T> key);

    <T> T getOrNull(ConfigKey<T> key);

    <T> Optional<T> getOptional(ConfigKey<T> key);
}

