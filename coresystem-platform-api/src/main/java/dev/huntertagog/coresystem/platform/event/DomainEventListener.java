package dev.huntertagog.coresystem.platform.event;

@FunctionalInterface
public interface DomainEventListener<E extends DomainEvent> {

    void onEvent(E event);
}
