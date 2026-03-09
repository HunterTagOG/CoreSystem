package dev.huntertagog.coresystem.platform.health;

import dev.huntertagog.coresystem.platform.provider.Service;

public interface HealthCheck extends Service {

    /**
     * Eindeutige Kennung, z. B. "redis", "economy", "playerProfile".
     */
    String name();

    /**
     * Führt die Prüfung synchron durch.
     * Exceptions werden NICHT nach außen propagiert, sondern im Ergebnis abgebildet.
     */
    HealthCheckResult check();
}
