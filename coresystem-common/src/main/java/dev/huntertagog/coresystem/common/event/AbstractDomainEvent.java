package dev.huntertagog.coresystem.common.event;

import dev.huntertagog.coresystem.platform.event.DomainEvent;

import java.util.UUID;

public abstract class AbstractDomainEvent implements DomainEvent {

    private final long occurredAt;
    private final UUID correlationId;

    protected AbstractDomainEvent() {
        this(System.currentTimeMillis(), UUID.randomUUID());
    }

    protected AbstractDomainEvent(UUID correlationId) {
        this(System.currentTimeMillis(), correlationId);
    }

    protected AbstractDomainEvent(long occurredAt, UUID correlationId) {
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
    }

    @Override
    public long occurredAt() {
        return occurredAt;
    }

    @Override
    public UUID correlationId() {
        return correlationId;
    }
}
