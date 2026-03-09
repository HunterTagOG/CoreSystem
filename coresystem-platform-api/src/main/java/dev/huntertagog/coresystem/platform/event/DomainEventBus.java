package dev.huntertagog.coresystem.platform.event;

import dev.huntertagog.coresystem.platform.provider.Service;

/**
 * Zentrales Event-Bus-Interface für Domain-Events.
 */
public interface DomainEventBus extends Service {

    /**
     * Listener für einen konkreten Event-Typ registrieren.
     */
    <E extends DomainEvent> void subscribe(Class<E> type, DomainEventListener<E> listener);

    /**
     * Listener deregistrieren.
     */
    <E extends DomainEvent> void unsubscribe(Class<E> type, DomainEventListener<E> listener);

    /**
     * Events synchron im aktuellen Thread verarbeiten.
     */
    void publish(DomainEvent event);

    /**
     * Events asynchron über den TaskScheduler verarbeiten.
     */
    void publishAsync(DomainEvent event);
}
