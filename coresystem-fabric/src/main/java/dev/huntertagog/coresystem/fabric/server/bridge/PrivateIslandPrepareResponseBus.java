package dev.huntertagog.coresystem.fabric.server.bridge;

import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.platform.provider.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public final class PrivateIslandPrepareResponseBus implements Service {

    private final Map<UUID, CompletableFuture<BridgeCodec.PrepareResponse>> pending =
            new ConcurrentHashMap<>();

    // EIN Scheduler für alle Requests
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cs-island-prepare-timeout");
                t.setDaemon(true);
                return t;
            });

    public CompletableFuture<BridgeCodec.PrepareResponse> register(
            UUID requestId,
            Duration timeout
    ) {
        CompletableFuture<BridgeCodec.PrepareResponse> future = new CompletableFuture<>();

        // atomar registrieren
        pending.put(requestId, future);

        scheduler.schedule(() -> {
            CompletableFuture<BridgeCodec.PrepareResponse> f = pending.remove(requestId);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(
                        new TimeoutException("Prepare timeout: " + requestId)
                );
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        return future;
    }

    public void complete(BridgeCodec.PrepareResponse resp) {
        CompletableFuture<BridgeCodec.PrepareResponse> future =
                pending.remove(resp.requestId());

        if (future != null && !future.isDone()) {
            future.complete(resp);
        }
    }

    /**
     * Optional, aber empfohlen bei Server-Shutdown
     */
    public void shutdown() {
        scheduler.shutdownNow();
        pending.clear();
    }
}
