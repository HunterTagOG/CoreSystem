package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.region.RegionDefinitionDto;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class RegionPersistentState extends PersistentState {

    public static final String KEY = "coresystem_regions_v1";

    /**
     * WICHTIG: neuer Type für getOrCreate
     */
    public static final Type<RegionPersistentState> TYPE =
            new Type<>(
                    RegionPersistentState::new,
                    RegionPersistentState::fromNbt,
                    null
            );

    private final Map<String, RegionDefinitionDto> regionsById = new HashMap<>();

    public Collection<RegionDefinitionDto> all() {
        return regionsById.values();
    }

    public RegionDefinitionDto get(String id) {
        return regionsById.get(id);
    }

    public void put(RegionDefinitionDto dto) {
        if (dto == null || dto.id() == null || dto.id().isBlank()) return;
        regionsById.put(dto.id(), dto);
        markDirty();
    }

    public void remove(String id) {
        if (id == null || id.isBlank()) return;
        if (regionsById.remove(id) != null) {
            markDirty();
        }
    }

    public static RegionPersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    // ------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------

    private static RegionPersistentState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        RegionPersistentState state = new RegionPersistentState();

        if (nbt.contains("regions", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("regions", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound r = list.getCompound(i);
                RegionDefinitionDto dto = RegionNbtCodec.readDto(r);
                if (dto != null && dto.id() != null && !dto.id().isBlank()) {
                    state.regionsById.put(dto.id(), dto);
                }
            }
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        NbtList list = new NbtList();
        for (RegionDefinitionDto dto : regionsById.values()) {
            NbtCompound r = RegionNbtCodec.writeDto(dto);
            if (r != null) list.add(r);
        }
        nbt.put("regions", list);
        return nbt;
    }
}
