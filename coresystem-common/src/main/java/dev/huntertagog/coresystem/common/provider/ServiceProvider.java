package dev.huntertagog.coresystem.common.provider;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceProvider {

    private static final Logger LOG = LoggerFactory.get("ServiceProvider");
    private static final Map<Class<? extends Service>, Service> SERVICES = new ConcurrentHashMap<>();

    private ServiceProvider() {
    }

    // ----------------------------------------------------------------------
    // REGISTRIERUNG
    // ----------------------------------------------------------------------
    public static <T extends Service> void registerService(
            Class<T> type,
            T service
    ) {
        Service previous = SERVICES.putIfAbsent(type, service);

        if (previous != null) {
            // CoreError erzeugen
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_ALREADY_REGISTERED,
                            CoreErrorSeverity.ERROR,
                            "Service already registered for type"
                    )
                    .withContextEntry("serviceType", type.getName())
                    .withContextEntry("existingImpl", previous.getClass().getName())
                    .withContextEntry("newImpl", service.getClass().getName());

            LOG.error(error.toLogString());
            throw new IllegalStateException(error.technicalMessage());
        }
    }

    // ----------------------------------------------------------------------
    // OPTIONAL GETTER
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static <T extends Service> T getService(Class<T> type) {
        return (T) SERVICES.get(type);
    }

    // ----------------------------------------------------------------------
    // REQUIRED GETTER
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static <T extends Service> T getRequiredService(Class<T> type) {
        Service service = SERVICES.get(type);

        if (service == null) {
            CoreError error = CoreError.of(
                            CoreErrorCode.SERVICE_NOT_FOUND,
                            CoreErrorSeverity.CRITICAL,
                            "Required service not registered"
                    )
                    .withContextEntry("serviceType", type.getName());

            LOG.error(error.toLogString());
            throw new IllegalStateException(error.technicalMessage());
        }

        return (T) service;
    }

    // ----------------------------------------------------------------------
    // UTILITIES
    // ----------------------------------------------------------------------
    public static <T extends Service> boolean isRegistered(Class<T> type) {
        return SERVICES.containsKey(type);
    }

    public static void clear() {
        SERVICES.clear();
    }
}
