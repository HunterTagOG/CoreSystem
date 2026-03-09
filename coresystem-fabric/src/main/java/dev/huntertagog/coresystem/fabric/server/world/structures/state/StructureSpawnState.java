package dev.huntertagog.coresystem.fabric.server.world.structures.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.*;

/**
 * Persistenter Zustand:
 * - je Struktur-ID eine Liste von Ziel-Positionen
 * - BitSet zum Markieren, welche Ziele bereits platziert wurden
 * <p>
 * Liegt pro Welt unter: world/data/coresystem_structure_spawns.dat
 */
public class StructureSpawnState extends PersistentState {

    // 1.21.x: Type wird direkt über den Konstruktor gebaut, kein create(...)
    public static final Type<StructureSpawnState> TYPE = new PersistentState.Type<>(
            StructureSpawnState::new,
            StructureSpawnState::fromNbt,
            null
    );

    public static class StructureData {
        public final List<BlockPos> targets = new ArrayList<>();
        public final BitSet placed = new BitSet();

        public boolean isFullyPlaced() {
            return placed.cardinality() >= targets.size();
        }
    }

    private final Map<Identifier, StructureData> structures = new HashMap<>();

    public StructureSpawnState() {
    }

    public static StructureSpawnState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, "coresystem_structure_spawns");
    }

    // ---------- Serialisierung ----------

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();

        for (Map.Entry<Identifier, StructureData> entry : structures.entrySet()) {
            Identifier id = entry.getKey();
            StructureData data = entry.getValue();

            NbtCompound structNbt = new NbtCompound();
            structNbt.putString("Id", id.toString());

            NbtList targetsList = new NbtList();
            for (BlockPos pos : data.targets) {
                NbtCompound p = new NbtCompound();
                p.putInt("x", pos.getX());
                p.putInt("y", pos.getY());
                p.putInt("z", pos.getZ());
                targetsList.add(p);
            }
            structNbt.put("Targets", targetsList);

            long[] bits = data.placed.toLongArray();
            structNbt.putLongArray("PlacedBits", bits);

            list.add(structNbt);
        }

        nbt.put("Structures", list);
        return nbt;
    }

    // Loader, von TYPE benutzt
    public static StructureSpawnState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        StructureSpawnState state = new StructureSpawnState();
        NbtList list = nbt.getList("Structures", NbtElement.COMPOUND_TYPE);

        for (NbtElement element : list) {
            NbtCompound structNbt = (NbtCompound) element;
            Identifier id = Identifier.of(structNbt.getString("Id"));

            StructureData data = new StructureData();

            NbtList targetsList = structNbt.getList("Targets", NbtElement.COMPOUND_TYPE);
            for (NbtElement tElem : targetsList) {
                NbtCompound p = (NbtCompound) tElem;
                BlockPos pos = new BlockPos(p.getInt("x"), p.getInt("y"), p.getInt("z"));
                data.targets.add(pos);
            }

            long[] bits = structNbt.getLongArray("PlacedBits");
            if (bits.length > 0) {
                data.placed.clear();
                data.placed.or(BitSet.valueOf(bits));
            }

            state.structures.put(id, data);
        }

        return state;
    }

    // ---------- API ----------

    public Map<Identifier, StructureData> getStructures() {
        return structures;
    }

    public StructureData getOrCreate(Identifier id) {
        return structures.computeIfAbsent(id, k -> new StructureData());
    }
}
