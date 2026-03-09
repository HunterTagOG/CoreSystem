package dev.huntertagog.coresystem.common.log;

import java.util.concurrent.ConcurrentHashMap;

public final class LoggerFactory {

    private static final ConcurrentHashMap<String, Logger> LOGGERS =
            new ConcurrentHashMap<>();

    public static Logger get(String name) {
        return LOGGERS.computeIfAbsent(name, Logger::new);
    }
}