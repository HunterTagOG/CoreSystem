package dev.huntertagog.coresystem.platform.economy;

import java.util.UUID;

/**
 * Repräsentiert eine einzelne Währungstransaktion im System.
 * Wird als JSON in Redis geloggt (z. B. für Audits/Analytics).
 *
 * @param currency        CurrencyId.value()
 * @param sourceAccountId UUID als String
 * @param targetAccountId bei Deposit/Withdraw optional
 */
public record TransactionLogEntry(String txId, long timestamp, Type type, Status status, String currency, long amount,
                                  String sourceAccountId, String targetAccountId, String reason) {

    public enum Type {
        DEPOSIT,
        WITHDRAW,
        TRANSFER
    }

    public enum Status {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        TECHNICAL_FAILURE
    }

    public static TransactionLogEntry depositSuccess(UUID accountId,
                                                     CurrencyId currency,
                                                     long amount,
                                                     String reason) {
        long now = System.currentTimeMillis();
        return new TransactionLogEntry(
                UUID.randomUUID().toString(),
                now,
                Type.DEPOSIT,
                Status.SUCCESS,
                currency.value(),
                amount,
                accountId.toString(),
                null,
                reason
        );
    }

    public static TransactionLogEntry withdrawResult(UUID accountId,
                                                     CurrencyId currency,
                                                     long amount,
                                                     EconomyTransactionResult result,
                                                     String reason) {
        long now = System.currentTimeMillis();
        Status status = switch (result.status()) {
            case SUCCESS -> Status.SUCCESS;
            case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS;
            case TECHNICAL_FAILURE -> Status.TECHNICAL_FAILURE;
        };
        return new TransactionLogEntry(
                UUID.randomUUID().toString(),
                now,
                Type.WITHDRAW,
                status,
                currency.value(),
                amount,
                accountId.toString(),
                null,
                reason
        );
    }

    public static TransactionLogEntry transferResult(UUID sourceAccountId,
                                                     UUID targetAccountId,
                                                     CurrencyId currency,
                                                     long amount,
                                                     EconomyTransactionResult result,
                                                     String reason) {
        long now = System.currentTimeMillis();
        Status status = switch (result.status()) {
            case SUCCESS -> Status.SUCCESS;
            case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS;
            case TECHNICAL_FAILURE -> Status.TECHNICAL_FAILURE;
        };
        return new TransactionLogEntry(
                UUID.randomUUID().toString(),
                now,
                Type.TRANSFER,
                status,
                currency.value(),
                amount,
                sourceAccountId.toString(),
                targetAccountId.toString(),
                reason
        );
    }
}
