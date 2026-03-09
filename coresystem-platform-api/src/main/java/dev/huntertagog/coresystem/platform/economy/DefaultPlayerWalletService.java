package dev.huntertagog.coresystem.platform.economy;

import dev.huntertagog.coresystem.platform.player.PlayerRef;

public record DefaultPlayerWalletService(EconomyService delegate) implements PlayerWalletService {

    @Override
    public Money getBalance(PlayerRef player) {
        return delegate.getBalance(player.playerId(), PlayerWalletService.DEFAULT_CURRENCY);
    }

    @Override
    public void add(PlayerRef player, long amount) {
        delegate.deposit(player.playerId(), Money.of(PlayerWalletService.DEFAULT_CURRENCY, amount), "Add funds");
    }

    @Override
    public boolean hasAtLeast(PlayerRef player, long amount) {
        return delegate.hasBalance(player.playerId(), Money.of(PlayerWalletService.DEFAULT_CURRENCY, amount));
    }

    @Override
    public Money deposit(PlayerRef player, long amount, String reason) {
        return delegate.deposit(player.playerId(), Money.of(PlayerWalletService.DEFAULT_CURRENCY, amount), reason);
    }

    @Override
    public EconomyTransactionResult withdraw(PlayerRef player, long amount, String reason) {
        return delegate.withdraw(player.playerId(), Money.of(PlayerWalletService.DEFAULT_CURRENCY, amount), reason);
    }
}
