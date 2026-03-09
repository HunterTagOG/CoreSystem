package dev.huntertagog.coresystem.client.screen;

import dev.huntertagog.coresystem.common.model.ServerTarget;
import net.minecraft.util.Identifier;

public final class ServerSwitcherTextures {

    private static Identifier id(String path) {
        return Identifier.of("coresystem", path);
    }

    // Frame & globale UI-Elemente
    public static final Identifier FRAME_OUTER = id("textures/gui/server_switch/frame_outer.png");
    public static final Identifier BUTTON_TP = id("textures/gui/server_switch/button_teleport.png");
    public static final Identifier BUTTON_ADMIN = id("textures/gui/server_switch/button_admin.png");
    public static final Identifier BUTTON_VIP = id("textures/gui/server_switch/button_vip.png");

    // Kacheln
    public static final Identifier TILE_SPAWN = id("textures/gui/server_switch/tile_spawn.png");
    public static final Identifier TILE_BUILD = id("textures/gui/server_switch/tile_build.png");
    public static final Identifier TILE_FARM = id("textures/gui/server_switch/tile_farm.png");
    public static final Identifier TILE_WILD = id("textures/gui/server_switch/tile_wild.png");
    public static final Identifier TILE_EVENT = id("textures/gui/server_switch/tile_event.png");
    public static final Identifier TILE_FALLBACK = id("textures/gui/server_switch/tile_spawn.png"); // Default

    private ServerSwitcherTextures() {
    }

    public static Identifier tileFor(ServerTarget target) {
        String id = target.id().toLowerCase();

        // IDs musst du ggf. an deine ServerTargets anpassen
        if (id.contains("spawn")) return TILE_SPAWN;
        if (id.contains("build")) return TILE_BUILD;
        if (id.contains("farm")) return TILE_FARM;
        if (id.contains("wild")) return TILE_WILD;
        if (id.contains("event")) return TILE_EVENT;

        return TILE_FALLBACK;
    }

    public static boolean isVip(ServerTarget target) {
        String id = target.id().toLowerCase();
        return id.contains("vip");
    }
}
