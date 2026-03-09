package dev.huntertagog.coresystem.platform.economy;

import java.util.Objects;

/**
 * Technische Währungs-ID (z. B. "coins", "gems", "tickets").
 * Dient als Schlüssel in Backend/Configs, nicht als angezeigter Name.
 */
public record CurrencyId(String value) {

    public CurrencyId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static CurrencyId of(String value) {
        return new CurrencyId(value.toLowerCase());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CurrencyId other)) return false;
        return this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return "CurrencyId[" + value + "]";
    }

    // Optional: vordefinierte Kernwährungen
    public static final CurrencyId COINS = CurrencyId.of("coins");
    public static final CurrencyId GEMS = CurrencyId.of("gems");
}
