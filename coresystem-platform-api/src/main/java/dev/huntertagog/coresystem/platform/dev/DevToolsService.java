package dev.huntertagog.coresystem.platform.dev;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.UUID;

/**
 * Zentrale Dev-/Test-Werkzeuge.
 * Alles hier sollte durch FeatureToggle + ModuleRegistry gated sein.
 */
public interface DevToolsService extends Service {

    /**
     * Simuliert einen Netzwerk-Join:
     * - Dummy PlayerProfile
     * - PlayerJoinedNetworkEvent auf dem DomainEventBus
     */
    void simulatePlayerJoin(UUID uniqueId, String name);

    /**
     * Simuliert einen Netzwerk-Quit:
     * - Dummy PlayerProfile
     * - PlayerQuitNetworkEvent auf dem DomainEventBus
     */
    void simulatePlayerQuit(UUID uniqueId, String name);

    // ---------------------------------------------------
    // NEU: Economy-Smoke-Test (gegen EconomyService)
    // ---------------------------------------------------

    /**
     * Führt einen einfachen Smoke-Test gegen das Economy-System aus:
     * - Balance lesen
     * - kleinen Deposit
     * - kleinen Withdraw
     * - Ergebnis als kurze Textzusammenfassung zurückgeben
     * <p>
     * accountId: i. d. R. Player-UUID
     */
    String runEconomySmokeTest(UUID accountId);

    // ---------------------------------------------------
    // NEU: Redis-Latenz-Messung
    // ---------------------------------------------------

    /**
     * Misst die durchschnittliche Redis-Latenz (PING) über n Samples.
     * Rückgabewert in Millisekunden.
     */
    long measureRedisLatencyMs(int samples);

    // ---------------------------------------------------
    // NEU: Join/Quit-Burst (Load-Test auf Event-/Metrics-/Audit-Pipeline)
    // ---------------------------------------------------

    /**
     * Simuliert eine Serie von Join+Quit-Events für synthetische UUIDs.
     */
    void simulateJoinQuitBurst(int count);
}
