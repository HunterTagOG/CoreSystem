package dev.huntertagog.coresystem.fabric.common.economy;

import dev.huntertagog.coresystem.platform.economy.CurrencyId;
import dev.huntertagog.coresystem.platform.economy.EconomyTransactionResult;
import dev.huntertagog.coresystem.platform.economy.Money;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Spezialisierte API für Player-Wallet-Operations.
 * Kapselt EconomyService + Player-spezifische Komfortfunktionen.
 */
public interface PlayerWalletService extends Service {

    CurrencyId DEFAULT_CURRENCY = CurrencyId.COINS;

    @NotNull
    Money getBalance(@NotNull UUID playerId);

    default @NotNull Money getBalance(@NotNull ServerPlayerEntity player) {
        return getBalance(player.getUuid());
    }

    boolean hasAtLeast(@NotNull UUID playerId, long amount);

    default boolean hasAtLeast(@NotNull ServerPlayerEntity player, long amount) {
        return hasAtLeast(player.getUuid(), amount);
    }

    @NotNull
    Money deposit(@NotNull UUID playerId, long amount, String reason);

    default @NotNull Money deposit(@NotNull ServerPlayerEntity player, long amount, String reason) {
        return deposit(player.getUuid(), amount, reason);
    }

    @NotNull
    EconomyTransactionResult withdraw(@NotNull UUID playerId, long amount, String reason);

    default @NotNull EconomyTransactionResult withdraw(@NotNull ServerPlayerEntity player, long amount, String reason) {
        return withdraw(player.getUuid(), amount, reason);
    }
}
