package dev.huntertagog.coresystem.common.event;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.event.DomainEvent;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.event.DomainEventListener;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SimpleDomainEventBus implements DomainEventBus {

    private static final Logger LOG = LoggerFactory.get("DomainEventBus");

    private final Map<Class<?>, CopyOnWriteArrayList<DomainEventListener<?>>> listeners =
            new ConcurrentHashMap<>();

    private final TaskScheduler scheduler;

    public SimpleDomainEventBus(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public <E extends DomainEvent> void subscribe(Class<E> type, DomainEventListener<E> listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    @Override
    public <E extends DomainEvent> void unsubscribe(Class<E> type, DomainEventListener<E> listener) {
        List<DomainEventListener<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;

        List<DomainEventListener<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }

        for (DomainEventListener<?> rawListener : list) {
            invokeListener(rawListener, event, false);
        }
    }

    @Override
    public void publishAsync(DomainEvent event) {
        if (event == null) return;

        if (scheduler == null) {
            // Fallback: synchron
            publish(event);
            return;
        }

        scheduler.runAsync(() -> {
            List<DomainEventListener<?>> list = listeners.get(event.getClass());
            if (list == null || list.isEmpty()) {
                return;
            }

            for (DomainEventListener<?> rawListener : list) {
                invokeListener(rawListener, event, true);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void invokeListener(DomainEventListener<?> rawListener,
                                DomainEvent event,
                                boolean async) {
        try {
            DomainEventListener<DomainEvent> listener =
                    (DomainEventListener<DomainEvent>) rawListener;
            listener.onEvent(event);
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.DOMAIN_EVENT_HANDLER_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Error while handling domain event.",
                    e,
                    Map.of(
                            "eventType", event.eventType(),
                            "eventClass", event.getClass().getName(),
                            "listenerClass", rawListener.getClass().getName(),
                            "async", async
                    )
            );
            LOG.error(error.toLogString(), e);
        }
    }
}
