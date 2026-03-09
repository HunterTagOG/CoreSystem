package dev.huntertagog.coresystem.fabric.server.region.image;

import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageRemoveS2CPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSetS2CPayload;
import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;

import java.util.Objects;

public final class RegionImageServerService {

    private RegionImageServerService() {
    }

    /**
     * Persistiert den Def in der Welt-PSM und broadcastet an alle Spieler dieser Welt.
     */
    public static void upsertAndBroadcast(ServerWorld world, RegionImageDef def) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(def, "def");

        // Safety: nur in der passenden Welt persistieren/broadcasten
        if (!world.getRegistryKey().getValue().equals(def.worldId())) {
            // optional: throw, oder einfach ignorieren
            return;
        }

        // persist
        RegionImageState st = state(world);
        st.put(toStateEntry(def));
        st.markDirty();

        // broadcast
        var payload = RegionImageSetS2CPayload.fromDef(def);
        for (ServerPlayerEntity p : world.getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /**
     * Entfernt Region-Image in dieser Welt und broadcastet Remove.
     */
    public static void removeAndBroadcast(ServerWorld world, String regionId) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(regionId, "regionId");

        RegionImageState st = state(world);
        st.remove(regionId);
        st.markDirty();

        var payload = new RegionImageRemoveS2CPayload(regionId, world.getRegistryKey().getValue());
        for (ServerPlayerEntity p : world.getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /**
     * Helper: Common-Def -> PersistentState Entry
     * (Damit bleibt State unabhängig vom Netzwerk-Payload.)
     */
    private static RegionImageDef toStateEntry(RegionImageDef def) {
        return new RegionImageDef(
                def.regionId(),
                def.worldId(),
                def.bounds(),
                def.mode(),
                def.facing(),
                def.baseY(),      // oder imageKey() je nach Naming in deinem Def
                def.textureId()
        );
    }

    public static RegionImageState state(ServerWorld world) {
        PersistentStateManager psm = world.getPersistentStateManager();
        return psm.getOrCreate(RegionImageState.TYPE, RegionImageState.KEY);
    }


    public static void syncToPlayer(ServerWorld world, ServerPlayerEntity player) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(player, "player");

        RegionImageState st = state(world);

        for (RegionImageDef e : st.all()) {
            // Safety: nur gleiche Dimension
            if (!world.getRegistryKey().getValue().equals(e.worldId())) continue;

            RegionImageDef def = toStateEntry(e); // -> Helper unten
            ServerPlayNetworking.send(player, RegionImageSetS2CPayload.fromDef(def));
        }
    }
}
