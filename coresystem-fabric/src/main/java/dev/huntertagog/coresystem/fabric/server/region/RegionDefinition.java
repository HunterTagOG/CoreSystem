package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.region.RegionFlag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class RegionDefinition {

    private final String id;
    private final String name;
    private final UUID ownerId;
    private final Identifier worldId;

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final Set<RegionFlag> flags;
    private final List<String> onEnterCommands;
    private final List<String> onLeaveCommands;

    public RegionDefinition(
            String id,
            String name,
            UUID ownerId,
            Identifier worldId,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            Set<RegionFlag> flags,
            List<String> onEnterCommands,
            List<String> onLeaveCommands
    ) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.worldId = worldId;

        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);

        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);

        this.flags = (flags == null || flags.isEmpty())
                ? EnumSet.noneOf(RegionFlag.class)
                : EnumSet.copyOf(flags);

        this.onEnterCommands = onEnterCommands != null ? List.copyOf(onEnterCommands) : List.of();
        this.onLeaveCommands = onLeaveCommands != null ? List.copyOf(onLeaveCommands) : List.of();
    }

    public String id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public Identifier worldId() {
        return worldId;
    }

    public Set<RegionFlag> flags() {
        return Collections.unmodifiableSet(flags);
    }

    public List<String> onEnterCommands() {
        return onEnterCommands;
    }

    public List<String> onLeaveCommands() {
        return onLeaveCommands;
    }

    public boolean hasFlag(RegionFlag flag) {
        return flags.contains(flag);
    }

    public boolean contains(ServerWorld world, BlockPos pos) {
        if (!world.getRegistryKey().getValue().equals(this.worldId)) {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    @Override
    public String toString() {
        return "Region[" + id + "@" + worldId + " (" +
                minX + "," + minY + "," + minZ + " -> " +
                maxX + "," + maxY + "," + maxZ + ")]";
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }
}
