package dev.huntertagog.coresystem.platform.metrics;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Map;

public interface MetricsService extends Service {

    /**
     * Erhöht einen Counter atomar um delta (default 1).
     */
    default void incrementCounter(String name) {
        incrementCounter(name, 1L, null);
    }

    void incrementCounter(String name, long delta, Map<String, String> tags);

    /**
     * Setzt einen Gauge (z. B. aktuelle Player-Anzahl, TPS-Snapshot).
     */
    void setGauge(String name, long value, Map<String, String> tags);

    /**
     * Timer-Metrik – Laufzeit in Millisekunden.
     * Kann optional auf Durchschnitt / P50 / P95 etc. aggregiert werden (später).
     */
    void recordTimer(String name, long durationMillis, Map<String, String> tags);
}
