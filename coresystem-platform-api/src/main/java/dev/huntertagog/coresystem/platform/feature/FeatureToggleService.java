package dev.huntertagog.coresystem.platform.feature;

import dev.huntertagog.coresystem.platform.provider.Service;

public interface FeatureToggleService extends Service {

    /**
     * Prüft, ob ein Feature aktiv ist.
     */
    boolean isEnabled(FeatureToggleKey key);

    /**
     * Optional: lokale Override-API (z. B. Admin-Tools später).
     */
    void setOverride(FeatureToggleKey key, Boolean enabled);
}
