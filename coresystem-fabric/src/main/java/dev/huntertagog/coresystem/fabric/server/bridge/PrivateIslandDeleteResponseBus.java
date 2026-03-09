package dev.huntertagog.coresystem.fabric.server.bridge;

import dev.huntertagog.coresystem.platform.bridge.codec.BridgeCodec;
import dev.huntertagog.coresystem.platform.provider.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public final class PrivateIslandDeleteResponseBus implements Service {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final Map<UUID, CompletableFuture<BridgeCodec.IslandDeleteResponse>> PENDING =
            new ConcurrentHashMap<>();

    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "IslandDeleteResponseBus-Timeout");
                t.setDaemon(true);
                return t;
            });

    public PrivateIslandDeleteResponseBus() {
    }

    // ------------------------------------------------------------
    // API
    // ------------------------------------------------------------

    public static CompletableFuture<BridgeCodec.IslandDeleteResponse> register(UUID requestId) {
        return register(requestId, DEFAULT_TIMEOUT);
    }

    public static CompletableFuture<BridgeCodec.IslandDeleteResponse> register(
            UUID requestId,
            Duration timeout
    ) {
        CompletableFuture<BridgeCodec.IslandDeleteResponse> future =
                new CompletableFuture<>();

        PENDING.put(requestId, future);

        TIMEOUT_SCHEDULER.schedule(() -> {
            CompletableFuture<BridgeCodec.IslandDeleteResponse> f =
                    PENDING.remove(requestId);

            if (f != null && !f.isDone()) {
                f.completeExceptionally(
                        new TimeoutException(
                                "Island delete response timed out after " + timeout
                        )
                );
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        return future;
    }

    public static void complete(BridgeCodec.IslandDeleteResponse response) {
        if (response == null) return;

        CompletableFuture<BridgeCodec.IslandDeleteResponse> future =
                PENDING.remove(response.requestId());

        if (future != null && !future.isDone()) {
            future.complete(response);
        }
    }

    public static void fail(UUID requestId, Throwable error) {
        CompletableFuture<BridgeCodec.IslandDeleteResponse> future =
                PENDING.remove(requestId);

        if (future != null && !future.isDone()) {
            future.completeExceptionally(error);
        }
    }

    /**
     * Optional, aber empfohlen bei Server-Shutdown
     */
    public void shutdown() {
        TIMEOUT_SCHEDULER.shutdownNow();
        PENDING.clear();
    }
}
