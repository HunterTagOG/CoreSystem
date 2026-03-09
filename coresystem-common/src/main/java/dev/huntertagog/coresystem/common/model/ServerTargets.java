package dev.huntertagog.coresystem.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.huntertagog.coresystem.common.model.ServerTarget.Category;

/**
 * Zentrale Definition aller Server-Ziele für das Teleport-GUI.
 * <p>
 * - Statische Basis-Ziele (Spawn, Build, Farm, Wild, Event, VIP-Wild)
 * - Meta-Targets für Admin-Funktionen (Wildness-Liste, Build-Liste)
 * - Collections für Default/VIP/Admin
 * - Platzhalterlisten für dynamische Wildness-Shards & private Build-Areas
 */
public final class ServerTargets {

    // --- Basis-Targets (einzelne Server / Proxy-Namen) ---

    public static final ServerTarget SPAWN =
            new ServerTarget("spawn", "Spawn", "spawn", Category.SPAWN);

    public static final ServerTarget BUILD =
            new ServerTarget("build", "Build", "build", Category.BUILD);

    public static final ServerTarget FARM =
            new ServerTarget("farm", "Farm", "farm", Category.FARM);

    public static final ServerTarget WILD =
            new ServerTarget("wild", "Wildness", "wild", Category.WILD);

    public static final ServerTarget VIP_WILD =
            new ServerTarget("vip_wild", "VIP Wildness", "vip_wild", Category.VIP_WILD);

    public static final ServerTarget EVENT =
            new ServerTarget("event", "Event", "event", Category.EVENT);


    // --- Meta-Targets (nur für Admin-GUI, kein direkter /server-Call) ---

    /**
     * Öffnet im Client die SubListScreen mit allen Wildness-Servern.
     */
    public static final ServerTarget WILD_LIST_META =
            new ServerTarget("wild_list", "Alle Wildness-Server", "",
                    Category.META);

    /**
     * Öffnet im Client den BuildSearchScreen mit allen privaten Build-Areas.
     */
    public static final ServerTarget BUILD_LIST_META =
            new ServerTarget("build_list", "Private Build Areas", "",
                    Category.META);


    // --- Collections für Rollen / Permission-Level ---

    /**
     * Default-User:
     * - Spawn
     * - Wildness
     * - Farm
     * - Build
     */
    public static final List<ServerTarget> DEFAULT =
            List.of(SPAWN, WILD, FARM, BUILD);

    /**
     * VIP-User:
     * - Spawn
     * - Wildness
     * - VIP Wildness
     * - Farm
     * - Build
     * - Event
     */
    public static final List<ServerTarget> VIP =
            List.of(SPAWN, WILD, VIP_WILD, FARM, BUILD, EVENT);

    /**
     * Admin-User:
     * - Spawn
     * - Wildness-Liste (Meta)
     * - VIP Wildness
     * - Farm
     * - Build-Liste (Meta)
     * - Event
     */
    public static final List<ServerTarget> ADMIN =
            List.of(SPAWN, WILD_LIST_META, VIP_WILD, FARM, BUILD_LIST_META, EVENT);


    // --- Dynamische Listen für Unter-Screens ---

    /**
     * Alle physischen Wildness-Server (Shards), z.B. wild-1, wild-2, wild-3...
     * <p>
     * Intern mutable, nach außen nur defensive Copies.
     */
    private static final List<ServerTarget> WILDNESS_ALL =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Alle privaten Build-Areas (pro Spieler/Welt), z.B. build_dennis, build_team1...
     * <p>
     * Intern mutable, nach außen nur defensive Copies.
     */
    private static final List<ServerTarget> PRIVATE_BUILDS =
            Collections.synchronizedList(new ArrayList<>());


    private ServerTargets() {
    }

    // ---------------------------------------------------------------------
    // Permission-Level API (für ServerKey / GUI)
    // ---------------------------------------------------------------------

    public enum Level {
        DEFAULT,
        VIP,
        ADMIN
    }

    /**
     * Liefert die passende Basiskonfiguration je Permission-Level.
     */
    public static List<ServerTarget> baseTargets(Level level) {
        return switch (level) {
            case DEFAULT -> DEFAULT;
            case VIP -> VIP;
            case ADMIN -> ADMIN;
        };
    }

    // ---------------------------------------------------------------------
    // Dynamische Listen – only controlled access
    // ---------------------------------------------------------------------

    public static void registerWildShard(String id, String displayName, String commandTarget) {
        WILDNESS_ALL.add(new ServerTarget(id, displayName, commandTarget, Category.WILD));
    }

    public static void registerPrivateBuild(String id, String displayName, String commandTarget) {
        PRIVATE_BUILDS.add(new ServerTarget(id, displayName, commandTarget, Category.BUILD));
    }

    public static List<ServerTarget> wildnessAll() {
        synchronized (WILDNESS_ALL) {
            return List.copyOf(WILDNESS_ALL);
        }
    }

    public static List<ServerTarget> privateBuilds() {
        synchronized (PRIVATE_BUILDS) {
            return List.copyOf(PRIVATE_BUILDS);
        }
    }

    public static void clearWildnessAll() {
        WILDNESS_ALL.clear();
    }

    public static void clearPrivateBuilds() {
        PRIVATE_BUILDS.clear();
    }
}
