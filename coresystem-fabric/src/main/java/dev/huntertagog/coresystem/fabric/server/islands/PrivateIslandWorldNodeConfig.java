package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.net.InetAddress;
import java.nio.file.Path;

public final class PrivateIslandWorldNodeConfig {

    private static final Logger LOG = LoggerFactory.get("IslandNodeConfig");

    public static String ISLANDS_PATH;
    public static boolean IS_ISLAND_SERVER;
    public static String SERVER_ID;
    public static String SERVER_NAME;

    static {
        // --- Pfad ---
        String pathEnv = System.getenv("ISLANDS_PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            ISLANDS_PATH = pathEnv;
        } else {
            ISLANDS_PATH = "/islands";
        }

        // --- Flag ---
        String flagEnv = System.getenv("ISLAND_SERVER");
        String prop = System.getProperty("coresystem.isIslandServer");

        boolean flag = false;
        if (prop != null && !prop.isBlank()) {
            flag = parseFlag(prop, "systemProperty");
        } else if (flagEnv != null && !flagEnv.isBlank()) {
            flag = parseFlag(flagEnv, "env");
        }

        IS_ISLAND_SERVER = flag;

        // --- NODE-ID ---
        String nodeEnv = System.getenv("SERVER_ID");
        if (nodeEnv != null && !nodeEnv.isBlank()) {
            SERVER_ID = nodeEnv;
        } else {
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                CoreError error = CoreError.of(
                                CoreErrorCode.ISLAND_NODE_CONFIG_HOSTNAME_RESOLVE_FAILED,
                                CoreErrorSeverity.WARN,
                                "Failed to resolve local hostname for island node id."
                        )
                        .withCause(e);
                LOG.warn(error.toLogString(), e);

                hostname = "unknown-node";
            }
            SERVER_ID = hostname;
        }

        // --- ISLAND SERVER NAME ---
        // Der Servername, über den Velocity / FabricProxyLite angesprochen wird
        String nameEnv = System.getenv("SERVER_NAME");
        if (nameEnv != null && !nameEnv.isBlank()) {
            SERVER_NAME = nameEnv;
        } else {
            SERVER_NAME = SERVER_ID; // fallback
        }

        LOG.info("islandServerFlag = {}", IS_ISLAND_SERVER);
        LOG.info("nodeId = {}", SERVER_ID);
        LOG.info("serverName = {}", SERVER_NAME);
        LOG.info("islandsPath = {}", ISLANDS_PATH);
    }

    private PrivateIslandWorldNodeConfig() {
    }

    private static boolean parseFlag(String raw, String source) {
        if (raw == null) return false;

        String v = raw.trim().toLowerCase();

        return switch (v) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;

            default -> {
                CoreError error = CoreError.of(
                                CoreErrorCode.ISLAND_NODE_CONFIG_FLAG_INVALID,
                                CoreErrorSeverity.WARN,
                                "Invalid boolean value for island server flag."
                        )
                        .withContextEntry("raw", raw)
                        .withContextEntry("source", source);
                LOG.warn(error.toLogString());
                yield false;
            }
        };
    }

    public static Path getIslandsBasePath() {
        return Path.of(ISLANDS_PATH);
    }

    /**
     * Erkennung: Ist das eine Private-Island-Welt von Coresystem?
     * Annahme: Namespace = dein Mod-ID, Pfad beginnt mit "island/"
     * → coresystem:island/<uuid>
     */
    public static boolean isIslandWorldKey(RegistryKey<World> key) {
        Identifier id = key.getValue();
        return id.getNamespace().equals(CoresystemCommon.MOD_ID)
                && id.getPath().startsWith("island/");
    }

    /**
     * Pfadauflösung für Island-Welten:
     * Beispiel:
     * key = coresystem:island/1234-uuid
     * base = /islands
     * => /islands/1234-uuid
     */
    public static Path resolveIslandPath(Path base, RegistryKey<World> key) {
        Identifier id = key.getValue();
        String path = id.getPath(); // z.B. "island/1234-uuid"

        String sub;
        String prefix = "island/";
        if (path.startsWith(prefix)) {
            sub = path.substring(prefix.length()); // nur <uuid>
        } else {
            sub = path; // Fallback
        }

        return base.resolve(sub);
    }
}
