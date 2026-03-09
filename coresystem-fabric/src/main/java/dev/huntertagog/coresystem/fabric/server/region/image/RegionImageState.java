package dev.huntertagog.coresystem.fabric.server.region.image;

import dev.huntertagog.coresystem.common.region.RegionImageFacing;
import dev.huntertagog.coresystem.common.region.RegionImageMode;
import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;

import java.util.*;

public final class RegionImageState extends PersistentState {

    public static final String KEY = "coresystem_region_images";

    public static final Type<RegionImageState> TYPE = new Type<>(
            RegionImageState::new,
            RegionImageState::fromNbt,
            null
    );

    private final Map<String, RegionImageDef> byRegionId = new HashMap<>();

    public Optional<RegionImageDef> get(String regionId) {
        return Optional.ofNullable(byRegionId.get(regionId));
    }

    public Collection<RegionImageDef> all() {
        return List.copyOf(byRegionId.values());
    }

    public void put(RegionImageDef entry) {
        byRegionId.put(entry.regionId(), entry);
        markDirty();
    }

    public void remove(String regionId) {
        byRegionId.remove(regionId);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList entries = new NbtList();

        for (RegionImageDef e : byRegionId.values()) {
            NbtCompound c = new NbtCompound();
            c.putString("regionId", e.regionId());
            c.putString("worldId", e.worldId().toString());

            c.putString("mode", e.mode().name());

            c.putString("facing", e.facing() == null ? "" : e.facing().name());
            c.putInt("baseY", e.baseY());

            entries.add(c);
        }

        nbt.put("entries", entries);
        return nbt;
    }

    public static RegionImageState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        RegionImageState st = new RegionImageState();

        if (!nbt.contains("entries", NbtElement.LIST_TYPE)) return st;

        NbtList list = nbt.getList("entries", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);

            String regionId = c.getString("regionId");
            Identifier worldId = Identifier.tryParse(c.getString("worldId"));
            if (worldId == null || regionId == null || regionId.isBlank()) continue;

            String imageKey = c.getString("imageKey");

            RegionImageMode mode = c.contains("mode", NbtElement.STRING_TYPE)
                    ? safeMode(c.getString("mode"))
                    : RegionImageMode.WALL;

            RegionImageFacing facing = RegionImageFacing.valueOf(c.contains("facing", NbtElement.STRING_TYPE) ? c.getString("facing") : "");
            int baseY = c.contains("baseY", NbtElement.INT_TYPE) ? c.getInt("baseY") : 0;

            Box bounds = new Box(
                    0, 0, 0,
                    0, 0, 0
            );

            st.byRegionId.put(regionId, new RegionImageDef(
                    regionId, worldId,
                    bounds, mode,
                    facing, baseY,
                    imageKey
            ));
        }

        return st;
    }

    private static RegionImageMode safeMode(String raw) {
        try {
            return RegionImageMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return RegionImageMode.WALL;
        }
    }

    private static NbtList writeUuidList(List<UUID> ids) {
        NbtList l = new NbtList();
        if (ids == null || ids.isEmpty()) return l;
        for (UUID id : ids) {
            if (id != null) l.add(NbtString.of(id.toString()));
        }
        return l;
    }

    private static List<UUID> readUuidList(NbtList list) {
        if (list == null || list.isEmpty()) return List.of();
        List<UUID> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String s = list.getString(i);
            try {
                out.add(UUID.fromString(s));
            } catch (Exception ignored) {
            }
        }
        return List.copyOf(out);
    }

    // -------- BlockPos list (Light blocks) --------

    private static NbtList writeBlockPosList(List<BlockPos> positions) {
        NbtList l = new NbtList();
        if (positions == null || positions.isEmpty()) return l;

        for (BlockPos p : positions) {
            if (p == null) continue;
            NbtCompound c = new NbtCompound();
            c.putInt("x", p.getX());
            c.putInt("y", p.getY());
            c.putInt("z", p.getZ());
            l.add(c);
        }
        return l;
    }

    private static List<BlockPos> readBlockPosList(NbtList list) {
        if (list == null || list.isEmpty()) return List.of();
        List<BlockPos> out = new ArrayList<>(list.size());

        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            out.add(new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z")));
        }
        return List.copyOf(out);
    }

    // -------- int list helpers --------

    private static int[] toIntArray(List<Integer> list) {
        if (list == null || list.isEmpty()) return new int[0];
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Integer v = list.get(i);
            arr[i] = (v != null) ? v : 0;
        }
        return arr;
    }

    private static List<Integer> toIntList(int[] arr) {
        if (arr == null || arr.length == 0) return List.of();
        List<Integer> out = new ArrayList<>(arr.length);
        for (int v : arr) out.add(v);
        return List.copyOf(out);
    }
}
