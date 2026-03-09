package dev.huntertagog.coresystem.common.economy;

import com.google.gson.Gson;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.audit.AuditContext;
import dev.huntertagog.coresystem.platform.audit.AuditLogService;
import dev.huntertagog.coresystem.platform.economy.*;
import dev.huntertagog.coresystem.platform.event.DomainEventBus;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleKey;
import dev.huntertagog.coresystem.platform.feature.FeatureToggleService;
import dev.huntertagog.coresystem.platform.metrics.MetricsService;
import dev.huntertagog.coresystem.platform.module.CoreModule;
import dev.huntertagog.coresystem.platform.module.ModuleRegistryService;
import redis.clients.jedis.Jedis;

import java.util.*;

public final class RedisEconomyService implements EconomyService {

    private static final Logger LOG = LoggerFactory.get("RedisEconomyService");
    private static final String BALANCE_KEY_PREFIX = "cs:economy:balance:";
    private static final String TX_LOG_KEY_PREFIX = "cs:economy:txlog:";
    private static final int MAX_LOG_ENTRIES_PER_CURRENCY = 10_000;

    private final Gson gson = new Gson();
    private final DomainEventBus eventBus;

    // Lua-Script: atomarer Withdraw (kein Negative-Balance)
    private static final String LUA_WITHDRAW = """
            local key = KEYS[1]
            local field = ARGV[1]
            local amount = tonumber(ARGV[2])
            local current = tonumber(redis.call('HGET', key, field) or '0')
            if current < amount then
              return {0, current}
            end
            local newBalance = redis.call('HINCRBY', key, field, -amount)
            return {1, newBalance}
            """;

    // Lua-Script: atomarer Transfer (Withdraw + Deposit in einem Schritt)
    private static final String LUA_TRANSFER = """
            local key = KEYS[1]
            local sourceField = ARGV[1]
            local targetField = ARGV[2]
            local amount = tonumber(ARGV[3])
            local currentSource = tonumber(redis.call('HGET', key, sourceField) or '0')
            if currentSource < amount then
              return {0, currentSource, 0}
            end
            local newSource = redis.call('HINCRBY', key, sourceField, -amount)
            local newTarget = redis.call('HINCRBY', key, targetField, amount)
            return {1, newSource, newTarget}
            """;

    public RedisEconomyService() {
        this.eventBus = ServiceProvider.getService(DomainEventBus.class);
    }

    private MetricsService metrics() {
        return ServiceProvider.getService(MetricsService.class);
    }

    private AuditLogService audit() {
        return ServiceProvider.getService(AuditLogService.class);
    }

    // -------------------------------------------------
    // Public API (EconomyService)
    // -------------------------------------------------

    @Override
    public Money getBalance(UUID accountId,
                            CurrencyId currencyId) {
        String key = balanceKey(currencyId);
        String field = accountId.toString();

        if (!isEconomyEnabled()) {
            // Economy abgeschaltet → immer 0, ohne Fehler
            return Money.of(currencyId, 0L);
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            String raw = jedis.hget(key, field);
            if (raw == null) {
                return Money.of(currencyId, 0L);
            }
            long amount = Long.parseLong(raw);
            return Money.of(currencyId, amount);
        } catch (NumberFormatException e) {
            CoreError.of(CoreErrorCode.ECONOMY_BALANCE_PARSE_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to parse balance value from Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("field", field)
                    .log();
            return Money.of(currencyId, 0L);
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.ECONOMY_BALANCE_LOAD_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to load balance from Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("field", field)
                    .log();
            return Money.of(currencyId, 0L);
        }
    }

    @Override
    public Money deposit(UUID accountId,
                         Money delta,
                         String reason) {

        if (delta.amount() <= 0) {
            return getBalance(accountId, delta.currency());
        }

        String key = balanceKey(delta.currency());
        String field = accountId.toString();

        long newBalance = 0L;
        try (Jedis jedis = RedisClient.get().getResource()) {
            newBalance = jedis.hincrBy(key, field, delta.amount());
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.ECONOMY_TRANSACTION_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Failed to deposit funds in Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("field", field)
                    .withContextEntry("amount", String.valueOf(delta.amount()))
                    .log();
            // wir können hier nur „Last known“ simulieren
            Money last = getBalance(accountId, delta.currency());
            logTransaction(TransactionLogEntry.depositSuccess(accountId, delta.currency(), delta.amount(), reason),
                    false);
            return last;
        }

        Money balance = Money.of(delta.currency(), newBalance);

        // Transaction log – best effort
        logTransaction(TransactionLogEntry.depositSuccess(accountId, delta.currency(), delta.amount(), reason), true);

        // --- Metrics (Best Effort) ---
        MetricsService m = metrics();
        if (m != null) {
            Map<String, String> tags = Map.of(
                    "currency", delta.currency().value(),
                    "type", "deposit"
            );
            m.incrementCounter("economy.transactions.total", 1L, tags);
            m.incrementCounter("economy.volume.delta", delta.amount(), tags);
        }

        AuditLogService audit = audit();
        if (audit != null) {
            auditEconomy(
                    "deposit",
                    accountId,
                    delta.currency(),
                    delta.amount(),
                    "SUCCESS",
                    reason,
                    null
            );
        }

        // Optional: Domain-Event (z. B. CurrencyTransactionEvent)
        publishBalanceChanged(accountId, delta.currency(), delta.amount(), balance, "deposit", reason);

        return balance;
    }

    @Override
    public EconomyTransactionResult withdraw(UUID accountId,
                                             Money delta,
                                             String reason) {

        if (delta.amount() <= 0) {
            Money current = getBalance(accountId, delta.currency());
            return EconomyTransactionResult.success(current, null);
        }

        String key = balanceKey(delta.currency());
        String field = accountId.toString();

        try (Jedis jedis = RedisClient.get().getResource()) {
            @SuppressWarnings("rawtypes")
            List result = (List) jedis.eval(LUA_WITHDRAW, List.of(key), List.of(field, String.valueOf(delta.amount())));

            long statusCode = ((Number) result.get(0)).longValue();
            long resultingBalance = ((Number) result.get(1)).longValue();
            Money newBalance = Money.of(delta.currency(), resultingBalance);

            EconomyTransactionResult txResult;
            if (statusCode == 0L) {
                txResult = EconomyTransactionResult.insufficientFunds(newBalance);
            } else {
                txResult = EconomyTransactionResult.success(newBalance, null);
            }

            // Transaction log – best effort
            logTransaction(TransactionLogEntry.withdrawResult(accountId, delta.currency(), delta.amount(), txResult, reason),
                    true);

            // --- Metrics ---
            MetricsService m = metrics();
            if (m != null) {
                Map<String, String> tags = new HashMap<>();
                tags.put("currency", delta.currency().value());
                tags.put("type", "withdraw");
                tags.put("status", txResult.status().name());

                m.incrementCounter("economy.transactions.total", 1L, tags);
                if (txResult.isSuccess()) {
                    m.incrementCounter("economy.volume.delta", delta.amount(), tags);
                }
            }

            AuditLogService audit = audit();
            if (audit != null) {
                String status = switch (txResult.status()) {
                    case SUCCESS -> "SUCCESS";
                    case INSUFFICIENT_FUNDS -> "INSUFFICIENT_FUNDS";
                    case TECHNICAL_FAILURE -> "TECHNICAL_FAILURE";
                };

                auditEconomy(
                        "withdraw",
                        accountId,
                        delta.currency(),
                        delta.amount(),
                        status,
                        reason,
                        null
                );
            }

            if (txResult.isSuccess()) {
                publishBalanceChanged(accountId, delta.currency(), -delta.amount(), newBalance, "withdraw", reason);
            }

            return txResult;

        } catch (Exception e) {
            Money current = getBalance(accountId, delta.currency());

            CoreError.of(CoreErrorCode.ECONOMY_TRANSACTION_SCRIPT_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Withdraw Lua script failed in Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("field", field)
                    .withContextEntry("amount", String.valueOf(delta.amount()))
                    .log();

            EconomyTransactionResult txResult = EconomyTransactionResult.technicalFailure("backend-error", current);
            logTransaction(TransactionLogEntry.withdrawResult(accountId, delta.currency(), delta.amount(), txResult, reason),
                    false);
            return txResult;
        }
    }

    @Override
    public EconomyTransactionResult transfer(UUID sourceAccountId,
                                             UUID targetAccountId,
                                             Money delta,
                                             String reason) {

        if (!isEconomyEnabled() || !isTransfersEnabled()) {
            Money sourceBalance = getBalance(sourceAccountId, delta.currency());
            // Operation geblockt, aber Server-Flow nicht crashen
            return EconomyTransactionResult.technicalFailure("transfers-disabled", sourceBalance);
        }

        if (delta.amount() <= 0 || sourceAccountId.equals(targetAccountId)) {
            Money sourceBalance = getBalance(sourceAccountId, delta.currency());
            Money targetBalance = getBalance(targetAccountId, delta.currency());
            return EconomyTransactionResult.success(sourceBalance, targetBalance);
        }

        String key = balanceKey(delta.currency());
        String sourceField = sourceAccountId.toString();
        String targetField = targetAccountId.toString();

        try (Jedis jedis = RedisClient.get().getResource()) {
            @SuppressWarnings("rawtypes")
            List result = (List) jedis.eval(
                    LUA_TRANSFER,
                    List.of(key),
                    List.of(sourceField, targetField, String.valueOf(delta.amount()))
            );

            long statusCode = ((Number) result.get(0)).longValue();
            long sourceBalanceLong = ((Number) result.get(1)).longValue();
            long targetBalanceLong = ((Number) result.get(2)).longValue();

            Money newSourceBalance = Money.of(delta.currency(), sourceBalanceLong);
            Money newTargetBalance = Money.of(delta.currency(), targetBalanceLong);

            EconomyTransactionResult txResult;
            if (statusCode == 0L) {
                txResult = EconomyTransactionResult.insufficientFunds(newSourceBalance);
            } else {
                txResult = EconomyTransactionResult.success(newSourceBalance, newTargetBalance);
            }

            AuditLogService audit = audit();
            if (audit != null) {
                String status = switch (txResult.status()) {
                    case SUCCESS -> "SUCCESS";
                    case INSUFFICIENT_FUNDS -> "INSUFFICIENT_FUNDS";
                    case TECHNICAL_FAILURE -> "TECHNICAL_FAILURE";
                };

                Map<String, String> extra = Map.of(
                        "targetAccountId", targetAccountId.toString()
                );

                auditEconomy(
                        "transfer",
                        sourceAccountId,
                        delta.currency(),
                        delta.amount(),
                        status,
                        reason,
                        extra
                );
            }

            // Transaction-Log – best effort
            logTransaction(TransactionLogEntry.transferResult(
                    sourceAccountId, targetAccountId, delta.currency(), delta.amount(), txResult, reason
            ), true);

            if (txResult.isSuccess()) {
                publishBalanceChanged(sourceAccountId, delta.currency(), -delta.amount(), newSourceBalance, "transfer-out", reason);
                publishBalanceChanged(targetAccountId, delta.currency(), delta.amount(), newTargetBalance, "transfer-in", reason);
            }

            return txResult;

        } catch (Exception e) {
            Money lastSource = getBalance(sourceAccountId, delta.currency());

            CoreError.of(CoreErrorCode.ECONOMY_TRANSACTION_SCRIPT_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Transfer Lua script failed in Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("sourceField", sourceField)
                    .withContextEntry("targetField", targetField)
                    .withContextEntry("amount", String.valueOf(delta.amount()))
                    .log();

            EconomyTransactionResult txResult =
                    EconomyTransactionResult.technicalFailure("backend-error", lastSource);
            logTransaction(TransactionLogEntry.transferResult(
                            sourceAccountId, targetAccountId, delta.currency(), delta.amount(), txResult, reason),
                    false);

            return txResult;
        }
    }

    // -------------------------------------------------
    // Internal Helpers
    // -------------------------------------------------

    private String balanceKey(CurrencyId currencyId) {
        return BALANCE_KEY_PREFIX + currencyId.value();
    }

    private String txLogKey(CurrencyId currencyId) {
        return TX_LOG_KEY_PREFIX + currencyId.value();
    }

    private void logTransaction(TransactionLogEntry entry, boolean important) {
        CurrencyId currencyId = CurrencyId.of(entry.currency());
        String key = txLogKey(currencyId);

        try (Jedis jedis = RedisClient.get().getResource()) {
            String json = gson.toJson(entry);
            jedis.rpush(key, json);
            jedis.ltrim(key, -MAX_LOG_ENTRIES_PER_CURRENCY, -1); // keep last N
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.ECONOMY_TRANSACTION_LOG_FAILED,
                            important ? CoreErrorSeverity.ERROR : CoreErrorSeverity.WARN,
                            "Failed to append transaction log entry to Redis")
                    .withCause(e)
                    .withContextEntry("redisKey", key)
                    .withContextEntry("txType", entry.type().name())
                    .withContextEntry("txStatus", entry.status().name())
                    .withContextEntry("currency", entry.currency())
                    .log();

            LOG.warn("Transaction log write failed for tx {} ({}) currency={} amount={}",
                    entry.txId(), entry.type(), entry.currency(), entry.amount());
        }
    }

    private void publishBalanceChanged(UUID accountId,
                                       CurrencyId currency,
                                       long delta,
                                       Money newBalance,
                                       String operation,
                                       String reason) {
        if (eventBus == null) {
            return;
        }

        try {
            // Hier könntest du ein dediziertes Domain-Event bauen, z. B.:
            // eventBus.publishAsync(new CurrencyBalanceChangedEvent(...));
        } catch (Exception e) {
            CoreError.of(CoreErrorCode.ECONOMY_EVENT_PUBLISH_FAILED,
                            CoreErrorSeverity.WARN,
                            "Failed to publish economy domain event")
                    .withCause(e)
                    .withContextEntry("uuid", accountId.toString())
                    .withContextEntry("currency", currency.value())
                    .withContextEntry("delta", String.valueOf(delta))
                    .withContextEntry("balance", String.valueOf(newBalance.amount()))
                    .withContextEntry("operation", operation)
                    .withContextEntry("reason", String.valueOf(reason))
                    .log();
        }
    }

    private boolean isEconomyEnabled() {
        ModuleRegistryService modules = ServiceProvider.getService(ModuleRegistryService.class);
        if (modules != null && !modules.isEnabled(CoreModule.ECONOMY)) {
            return false;
        }

        FeatureToggleService features = ServiceProvider.getService(FeatureToggleService.class);
        if (features != null && !features.isEnabled(FeatureToggleKey.ECONOMY_ENABLED)) {
            return false;
        }

        return true;
    }

    private boolean isTransfersEnabled() {
        FeatureToggleService features = ServiceProvider.getService(FeatureToggleService.class);
        if (features == null) return true;
        return features.isEnabled(FeatureToggleKey.ECONOMY_TRANSFERS_ENABLED);
    }

    private static AuditContext systemAuditContext() {
        return new AuditContext() {
            @Override
            public Optional<String> actorId() {
                return Optional.empty();
            }

            @Override
            public String actorName() {
                return "SYSTEM";
            }

            @Override
            public String serverName() {
                return "";
            }

            @Override
            public String nodeId() {
                return "";
            }

            @Override
            public AuditActorType actorType() {
                return AuditActorType.SYSTEM;
            }
        };
    }

    private void auditEconomy(
            String action,
            UUID accountId,
            CurrencyId currencyId,
            long amount,
            String status,
            String reason,
            Map<String, String> extra
    ) {
        AuditLogService a = audit();
        if (a == null) return;

        Map<String, String> details = new HashMap<>();
        details.put("domain", "economy");
        details.put("action", action);
        details.put("accountId", accountId.toString());
        details.put("currency", currencyId.value());
        details.put("amount", String.valueOf(amount));
        details.put("status", status);
        if (reason != null) details.put("reason", reason);

        if (extra != null && !extra.isEmpty()) {
            details.putAll(extra);
        }

        // Action ist „stabiler Key“ für spätere Auswertung
        a.log(systemAuditContext(), "economy." + action, details.toString());
    }
}
