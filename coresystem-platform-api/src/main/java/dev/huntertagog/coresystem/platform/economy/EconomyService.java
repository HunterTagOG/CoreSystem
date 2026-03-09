package dev.huntertagog.coresystem.platform.economy;

import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.UUID;

/**
 * Zentrale Schnittstelle für Account-basierte Währungen im Netzwerk.
 * Backend (Redis, SQL, Mongo, ...) ist austauschbar.
 */
public interface EconomyService extends Service {

    /**
     * Liefert den aktuellen Kontostand für Account + Currency.
     * Bei technischen Fehlern sollte ein Fallback (z. B. 0) zurückkommen und
     * der Fehler über CoreError telemetriert werden.
     */

    Money getBalance(UUID accountId, CurrencyId currencyId);

    /**
     * Schneller Check, ob Account >= amount hat.
     * Default-Impl. kann auf getBalance basieren.
     */
    default boolean hasBalance(UUID accountId,
                               Money amount) {
        Money current = getBalance(accountId, amount.currency());
        return current.gte(amount);
    }

    /**
     * Erhöht Guthaben um delta (delta > 0).
     * Gibt neuen Kontostand zurück.
     */

    Money deposit(UUID accountId, Money delta, String reason);

    /**
     * Verringert Guthaben um delta (delta > 0).
     * Gibt neuen Kontostand zurück.
     * Wirft oder signalisiert Fehler, wenn nicht genügend Guthaben vorhanden ist.
     */

    EconomyTransactionResult withdraw(UUID accountId, Money delta, String reason);

    /**
     * Transfer von einem Account auf einen anderen (atomar per Backend).
     */

    EconomyTransactionResult transfer(UUID sourceAccountId,
                                      UUID targetAccountId,
                                      Money delta,
                                      String reason);
}
