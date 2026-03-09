package dev.huntertagog.coresystem.common.model;

public record ServerTarget(
        String id,             // interne ID, z.B. "spawn", "vip_wild", "event"
        String displayName,    // Label im GUI
        String commandTarget,  // Ziel für /server <name> o.ä.
        Category category      // fachliche Einordnung
) {

    public enum Category {
        SPAWN,
        WILD,
        VIP_WILD,
        FARM,
        BUILD,
        EVENT,
        META            // nur GUI-Funktion (z.B. "wild_list", "build_list")
    }
}
