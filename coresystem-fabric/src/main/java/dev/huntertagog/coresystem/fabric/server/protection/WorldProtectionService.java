package dev.huntertagog.coresystem.fabric.server.protection;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

/**
 * Globale World-Schutz-Policy pro Minecraft-Server.
 * <p>
 * Aktuell:
 * - TNT-Explosionen blockieren
 * - Feuer-Tick (Spread) deaktivieren
 * <p>
 * Konfiguration per ENV:
 * PROTECT_TNT=true|false
 * PROTECT_FIRE=true|false
 * <p>
 * Optional: PROTECT_WORLD_FILTER = "all" (default) oder
 * CSV-Liste von World-Pfaden (z.B. "minecraft:overworld,coresystem:lobby")
 */
public final class WorldProtectionService implements Service {

    private static final Logger LOG = LoggerFactory.get("WorldProtection");

    private final MinecraftServer server;
    private final boolean protectTnt;
    private final boolean protectFire;
    private final ProtectionScope scope;

    public WorldProtectionService(MinecraftServer server) {
        this.server = server;

        this.protectTnt = Boolean.parseBoolean(
                System.getenv().getOrDefault("PROTECT_TNT", "false")
        );
        this.protectFire = Boolean.parseBoolean(
                System.getenv().getOrDefault("PROTECT_FIRE", "false")
        );

        String filterRaw = System.getenv().getOrDefault("PROTECT_WORLD_FILTER", "all")
                .trim()
                .toLowerCase(Locale.ROOT);

        if ("all".equals(filterRaw) || filterRaw.isBlank()) {
            this.scope = ProtectionScope.allWorlds();
        } else {
            this.scope = ProtectionScope.fromCsv(filterRaw);
        }

        LOG.info("WorldProtectionService initialized: TNT={}, FIRE={}, scope={}",
                protectTnt, protectFire, scope);
    }

    /**
     * Wird aufgerufen, wenn eine Welt geladen wurde – setzt z. B. GameRules.
     */
    public void applyOnWorldLoad(@NotNull ServerWorld world) {
        if (!scope.isProtected(world)) {
            return;
        }

        GameRules rules = world.getGameRules();

        if (protectFire) {
            rules.get(GameRules.DO_FIRE_TICK).set(false, server);
            LOG.info("Applied DO_FIRE_TICK=false for world {}", world.getRegistryKey().getValue());
        }

        // TNT gibt es in Java-Edition NICHT als eigenen Gamerule.
        // TNT-Explosionen werden weiter unten via Mixin/TNT-Hook geblockt.
    }

    /**
     * Wird im TNT-Mixin verwendet.
     */
    public boolean isTntBlocked(@NotNull World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false; // nur auf Server-Welten schützen
        }
        return protectTnt && scope.isProtected(serverWorld);
    }

    /**
     * Optional, falls du später noch andere Mechaniken an World-Schutz knüpfen willst.
     */
    public boolean isFireTickBlocked(@NotNull World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false; // nur auf Server-Welten schützen
        }
        return protectFire && scope.isProtected(serverWorld);
    }

    // ------------------------------------------------------------
    // Scope-Definition (welche Welten sind „geschützt“)
    // ------------------------------------------------------------

    private record ProtectionScope(boolean all, Set<String> protectedWorldIds) {

        static ProtectionScope allWorlds() {
            return new ProtectionScope(true, Set.of());
        }

        static ProtectionScope fromCsv(String csv) {
            String[] parts = csv.split(",");
            Set<String> ids = java.util.Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            return new ProtectionScope(false, ids);
        }

        boolean isProtected(ServerWorld world) {
            if (all) return true;
            String id = world.getRegistryKey().getValue().toString().toLowerCase(Locale.ROOT);
            return protectedWorldIds.contains(id);
        }

        @Override
        public @NotNull String toString() {
            if (all) return "all-worlds";
            return "worlds=" + protectedWorldIds;
        }
    }
}
