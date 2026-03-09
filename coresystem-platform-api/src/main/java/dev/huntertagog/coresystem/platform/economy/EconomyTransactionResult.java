package dev.huntertagog.coresystem.platform.economy;


public record EconomyTransactionResult(Status status, Money newSourceBalance, Money newTargetBalance,
                                       String errorMessage) {

    public Money balance() {
        return newSourceBalance;
    }

    public enum Status {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        TECHNICAL_FAILURE
    }

    public static EconomyTransactionResult success(Money newSourceBalance, Money newTargetBalance) {
        return new EconomyTransactionResult(Status.SUCCESS, newSourceBalance, newTargetBalance, null);
    }

    public static EconomyTransactionResult insufficientFunds(Money currentBalance) {
        return new EconomyTransactionResult(Status.INSUFFICIENT_FUNDS, currentBalance, null, null);
    }

    public static EconomyTransactionResult technicalFailure(String message, Money lastKnownBalance) {
        return new EconomyTransactionResult(Status.TECHNICAL_FAILURE, lastKnownBalance, null, message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
