package dev.huntertagog.coresystem.common.metrics;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.player.event.PlayerJoinedNetworkEvent;
import dev.huntertagog.coresystem.common.player.event.PlayerQuitNetworkEvent;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.event.DomainEventListener;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class MetricsEventListener {

    private static final Logger LOG = LoggerFactory.get("MetricsEventListener");

    private final MetricsService metrics;
    private final String serverName;
    private final String nodeId;

    // In-Memory Gauge für Online-Player pro Node (wird zusätzlich in Redis gespiegelt)
    private final AtomicInteger onlinePlayers = new AtomicInteger(0);

    // Referenzen auf die registrierten Listener, um ggf. auch wieder unsubscriben zu können
    private DomainEventListener<PlayerJoinedNetworkEvent> joinListener;
    private DomainEventListener<PlayerQuitNetworkEvent> quitListener;

    public MetricsEventListener(MetricsService metrics) {
        this.metrics = metrics;
        this.serverName = System.getenv().getOrDefault("CORESYSTEM_SERVER_NAME", "unknown");
        this.nodeId = System.getenv().getOrDefault("CORESYSTEM_NODE_ID", "unknown-node");
    }

    public void registerOnBus() {
        DomainEventBus bus = ServiceProvider.getService(DomainEventBus.class);
        if (bus == null) {
            LOG.warn("DomainEventBus not available - MetricsEventListener not wired");
            return;
        }

        // Typed Listener erzeugen und registrieren
        this.joinListener = this::handlePlayerJoined;
        this.quitListener = this::handlePlayerQuit;

        bus.subscribe(PlayerJoinedNetworkEvent.class, joinListener);
        bus.subscribe(PlayerQuitNetworkEvent.class, quitListener);

        LOG.info("MetricsEventListener registered on DomainEventBus (server={}, node={})",
                serverName, nodeId);
    }

    public void unregisterFromBus() {
        DomainEventBus bus = ServiceProvider.getService(DomainEventBus.class);
        if (bus == null) {
            return;
        }
        if (joinListener != null) {
            bus.unsubscribe(PlayerJoinedNetworkEvent.class, joinListener);
        }
        if (quitListener != null) {
            bus.unsubscribe(PlayerQuitNetworkEvent.class, quitListener);
        }
        LOG.info("MetricsEventListener unregistered from DomainEventBus");
    }

    // ------------------------------------------------------------------------
    // Event-Handler (typed)
    // ------------------------------------------------------------------------

    private void handlePlayerJoined(PlayerJoinedNetworkEvent event) {
        PlayerProfile profile = event.profile();

        Map<String, String> commonTags = Map.of(
                "server", serverName,
                "node", nodeId
        );

        // Total Joins
        metrics.incrementCounter("players.joins.total", 1L, commonTags);

        // First Join: anhand Profil-Heuristik
        boolean isFirstJoin = profile.getTotalJoins() <= 1;
        if (isFirstJoin) {
            metrics.incrementCounter("players.joins.first.total", 1L, commonTags);

            // optional: pro Tag aggregieren
            String day = java.time.LocalDate.now().toString(); // YYYY-MM-DD
            Map<String, String> dayTags = new HashMap<>(commonTags);
            dayTags.put("day", day);
            metrics.incrementCounter("players.joins.first.by_day", 1L, dayTags);
        }

        // Online-Gauge hochzählen
        int currentOnline = onlinePlayers.incrementAndGet();
        metrics.setGauge("players.online", currentOnline, commonTags);
    }

    private void handlePlayerQuit(PlayerQuitNetworkEvent event) {
        Map<String, String> commonTags = Map.of(
                "server", serverName,
                "node", nodeId
        );

        int currentOnline = onlinePlayers.decrementAndGet();
        if (currentOnline < 0) {
            // defensive reset
            onlinePlayers.set(0);
            currentOnline = 0;
        }
        metrics.setGauge("players.online", currentOnline, commonTags);
    }
}
