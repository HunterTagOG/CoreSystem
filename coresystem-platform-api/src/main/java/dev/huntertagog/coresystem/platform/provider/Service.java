package dev.huntertagog.coresystem.platform.provider;

/**
 * Marker-Interface für zentral verwaltete Dienste im Coresystem.
 * <p>
 * Alle globalen Services (z.B. StructureSpawnService, UserCacheService, etc.)
 * implementieren dieses Interface und werden über den ServiceProvider registriert.
 */
public interface Service {
    // absichtlich leer – reine Typmarkierung
}

