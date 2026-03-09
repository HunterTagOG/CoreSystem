package dev.huntertagog.coresystem.fabric.server.world.island;

import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public final class IslandStyleSelector {

    private IslandStyleSelector() {
    }

    public static IslandStyle selectFor(ServerWorld world, UUID ownerId, long worldSeed) {
        long h = worldSeed
                ^ ownerId.getMostSignificantBits()
                ^ ownerId.getLeastSignificantBits()
                ^ 0xCAFEBABECAFEL;

        IslandStyle[] values = IslandStyle.values();
        int idx = (int) (Math.abs(h) % values.length);
        return values[idx];
    }
}
