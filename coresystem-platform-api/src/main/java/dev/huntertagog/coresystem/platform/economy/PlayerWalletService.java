package dev.huntertagog.coresystem.platform.economy;

import dev.huntertagog.coresystem.platform.player.PlayerRef;
import dev.huntertagog.coresystem.platform.provider.Service;

public interface PlayerWalletService extends Service {

    CurrencyId DEFAULT_CURRENCY = CurrencyId.of("coins");

    Money getBalance(PlayerRef player);

    void add(PlayerRef player, long amount);

    EconomyTransactionResult withdraw(PlayerRef player, long amount, String reason);

    boolean hasAtLeast(PlayerRef player, long amount);

    Money deposit(PlayerRef player, long amount, String reason);
}
