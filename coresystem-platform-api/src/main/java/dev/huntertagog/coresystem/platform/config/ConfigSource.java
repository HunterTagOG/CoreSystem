package dev.huntertagog.coresystem.platform.config;

import java.util.Map;

public interface ConfigSource {

    /**
     * Liefert flache Key-Value-Paare, z.B.
     * "islands.max_islands_per_node" -> "64"
     */
    Map<String, String> load();

    /**
     * Priorität für Merge:
     * höhere Zahl = überschreibt niedrige.
     * z.B. Env=200, File=100, Defaults=0
     */
    int priority();
}

