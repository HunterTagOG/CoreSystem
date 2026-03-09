package dev.huntertagog.coresystem.platform.event;

import java.util.UUID;

/**
 * Basis-Domain-Event für alle Business-Events im CoreSystem.
 * Immutable, rein DTO.
 */
public interface DomainEvent {

    /**
     * Technischer Event-Typ.
     * Default: vollqualifizierter Klassenname.
     */
    default String eventType() {
        return getClass().getName();
    }

    /**
     * Zeitpunkt des Events (epoch millis).
     */
    long occurredAt();

    /**
     * Korrelation / Trace-ID für verknüpfte Events.
     */
    UUID correlationId();
}
