package dev.huntertagog.coresystem.common.dev;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.player.event.PlayerJoinedNetworkEvent;
import dev.huntertagog.coresystem.common.player.event.PlayerQuitNetworkEvent;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.dev.DevToolsService;
import dev.huntertagog.coresystem.platform.economy.CurrencyId;
import dev.huntertagog.coresystem.platform.economy.EconomyService;
import dev.huntertagog.coresystem.platform.economy.EconomyTransactionResult;
import dev.huntertagog.coresystem.platform.economy.Money;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import redis.clients.jedis.Jedis;

import java.util.UUID;

public final class DefaultDevToolsService implements DevToolsService {

    private static final Logger LOG = LoggerFactory.get("DevToolsService");

    private final DomainEventBus eventBus;
    private final String serverName;
    private final String nodeId;

    public DefaultDevToolsService() {
        this.eventBus = ServiceProvider.getService(DomainEventBus.class);
        this.serverName = System.getenv().getOrDefault("CORESYSTEM_SERVER_NAME", "dev-server");
        this.nodeId = System.getenv().getOrDefault("CORESYSTEM_NODE_ID", "dev-node");
    }

    // ---------------------------------------------------
    // Bereits bestehende Methoden
    // ---------------------------------------------------

    @Override
    public void simulatePlayerJoin(UUID uniqueId, String name) {
        if (eventBus == null) {
            LOG.warn("DomainEventBus not available - simulatePlayerJoin ignored");
            return;
        }

        long now = System.currentTimeMillis();
        PlayerProfile profile = new PlayerProfile(
                uniqueId,
                name,
                now,
                now,
                1,
                serverName,
                nodeId
        );

        LOG.info("[DEV] Simulating PlayerJoinedNetworkEvent for {} ({})", name, uniqueId);
        eventBus.publishAsync(new PlayerJoinedNetworkEvent(profile));
    }

    @Override
    public void simulatePlayerQuit(UUID uniqueId, String name) {
        if (eventBus == null) {
            LOG.warn("DomainEventBus not available - simulatePlayerQuit ignored");
            return;
        }

        long now = System.currentTimeMillis();
        PlayerProfile profile = new PlayerProfile(
                uniqueId,
                name,
                now - 30_000L,
                now,
                1,
                serverName,
                nodeId
        );

        LOG.info("[DEV] Simulating PlayerQuitNetworkEvent for {} ({})", name, uniqueId);
        eventBus.publishAsync(new PlayerQuitNetworkEvent(profile));
    }

    // ---------------------------------------------------
    // NEU: Economy-Smoke-Test
    // ---------------------------------------------------

    @Override
    public String runEconomySmokeTest(UUID accountId) {
        EconomyService economy = ServiceProvider.getService(EconomyService.class);
        if (economy == null) {
            LOG.warn("[DEV] EconomyService not available - economy smoke test skipped");
            return "EconomyService not available – smoke test skipped.";
        }

        // Default-Währung bestimmen (anpassbar)
        String defaultCurrencyId = System.getenv().getOrDefault("CORESYSTEM_DEFAULT_CURRENCY", "default");
        CurrencyId currency = CurrencyId.of(defaultCurrencyId);

        // kleines Testvolumen
        long testAmount = 10L;

        StringBuilder sb = new StringBuilder();
        sb.append("Economy smoke test for account=").append(accountId)
                .append(" currency=").append(defaultCurrencyId).append("\n");

        Money before = economy.getBalance(accountId, currency);
        sb.append("  before=").append(before.amount()).append("\n");

        Money afterDeposit = economy.deposit(accountId, Money.of(currency, testAmount), "[DEV] smoke deposit");
        sb.append("  after deposit(+").append(testAmount).append(")=").append(afterDeposit.amount()).append("\n");

        EconomyTransactionResult withdrawResult = economy.withdraw(
                accountId,
                Money.of(currency, testAmount),
                "[DEV] smoke withdraw"
        );
        Money afterWithdraw = withdrawResult.balance();
        sb.append("  after withdraw(-").append(testAmount).append(")=")
                .append(afterWithdraw.amount())
                .append(" status=").append(withdrawResult.status().name())
                .append("\n");

        LOG.info("[DEV] Economy smoke test finished for {}: before={} afterDeposit={} afterWithdraw={} status={}",
                accountId,
                before.amount(),
                afterDeposit.amount(),
                afterWithdraw.amount(),
                withdrawResult.status().name()
        );

        return sb.toString();
    }

    // ---------------------------------------------------
    // NEU: Redis-Latenz-Messung
    // ---------------------------------------------------

    @Override
    public long measureRedisLatencyMs(int samples) {
        if (samples <= 0) {
            samples = 1;
        }

        long totalMs = 0L;
        int ok = 0;

        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            try (Jedis jedis = RedisClient.get().getResource()) {
                String pong = jedis.ping();
                long durationMs = (System.nanoTime() - start) / 1_000_000L;
                if ("PONG".equalsIgnoreCase(pong)) {
                    totalMs += durationMs;
                    ok++;
                }
            } catch (Exception e) {
                LOG.warn("[DEV] Redis latency sample {} failed", i, e);
            }
        }

        if (ok == 0) {
            LOG.warn("[DEV] Redis latency measurement: no successful samples");
            return -1L;
        }

        long avg = totalMs / ok;
        LOG.info("[DEV] Redis latency avg={}ms over {} successful samples", avg, ok);
        return avg;
    }

    // ---------------------------------------------------
    // NEU: Join/Quit-Burst-Loadtest
    // ---------------------------------------------------

    @Override
    public void simulateJoinQuitBurst(int count) {
        if (eventBus == null) {
            LOG.warn("DomainEventBus not available - simulateJoinQuitBurst ignored");
            return;
        }

        if (count <= 0) {
            return;
        }

        LOG.info("[DEV] Simulating join+quit burst count={}", count);

        for (int i = 0; i < count; i++) {
            String name = "DevLoadTest_" + i;
            UUID uuid = UUID.nameUUIDFromBytes(("dev-load-" + i).getBytes());

            long now = System.currentTimeMillis();

            PlayerProfile joinProfile = new PlayerProfile(
                    uuid,
                    name,
                    now,
                    now,
                    1,
                    serverName,
                    nodeId
            );
            eventBus.publishAsync(new PlayerJoinedNetworkEvent(joinProfile));

            PlayerProfile quitProfile = new PlayerProfile(
                    uuid,
                    name,
                    now,
                    now + 5_000L,
                    1,
                    serverName,
                    nodeId
            );
            eventBus.publishAsync(new PlayerQuitNetworkEvent(quitProfile));
        }
    }
}
