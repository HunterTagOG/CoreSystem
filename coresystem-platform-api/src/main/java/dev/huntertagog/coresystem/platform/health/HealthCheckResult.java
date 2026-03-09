package dev.huntertagog.coresystem.platform.health;

import java.util.Collections;
import java.util.Map;

public final class HealthCheckResult {

    private final String name;
    private final HealthStatus status;
    private final String message;
    private final Map<String, String> details;

    private HealthCheckResult(String name,
                              HealthStatus status,
                              String message,
                              Map<String, String> details) {
        this.name = name;
        this.status = status;
        this.message = message;
        this.details = Collections.unmodifiableMap(details);
    }

    public static HealthCheckResult of(String name,
                                       HealthStatus status,
                                       String message,
                                       Map<String, String> details) {
        return new HealthCheckResult(
                name,
                status,
                message,
                details != null ? details : Collections.emptyMap()
        );
    }

    public String name() {
        return name;
    }

    public HealthStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public Map<String, String> details() {
        return details;
    }
}
