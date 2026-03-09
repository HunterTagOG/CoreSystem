package dev.huntertagog.coresystem.fabric.server.islands;

import lombok.Getter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

public final class PrivateIslandInitState extends PersistentState {
    public static final String KEY = "coresystem_island_init";

    private static final int CURRENT_SCHEMA = 1;

    // --------- getters/setters (minimal) ----------
    @Getter
    private boolean initialized = false;
    private int schema = CURRENT_SCHEMA;

    private long initEpochMs = 0L;

    // optional, aber sehr hilfreich fürs Troubleshooting
    private long seed = 0L;
    private String preset = "";   // z.B. REALISTIC_SINGLE
    private String style = "";    // z.B. MIXED
    private String configHash = ""; // z.B. sha1/xxhash64 von RuntimeWorldConfig/Env

    public PrivateIslandInitState() {
    }

    public static final PersistentState.Type<PrivateIslandInitState> TYPE =
            new PersistentState.Type<>(
                    PrivateIslandInitState::new,
                    PrivateIslandInitState::fromNbt,
                    null // passt für custom saved data
            );

    public void markInitialized() {
        this.initialized = true;
        this.initEpochMs = System.currentTimeMillis();
        this.schema = CURRENT_SCHEMA;
        this.markDirty(); // wichtig: damit MC es speichert
    }

    public void setDebugMeta(long seed, String preset, String style, String configHash) {
        this.seed = seed;
        this.preset = preset != null ? preset : "";
        this.style = style != null ? style : "";
        this.configHash = configHash != null ? configHash : "";
        this.markDirty();
    }

    public void setUninitialized() {
        this.initialized = false;
        this.markDirty();
    }

    // --------- PersistentState serialization ----------
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("schema", schema);
        nbt.putBoolean("initialized", initialized);
        nbt.putLong("initEpochMs", initEpochMs);

        // optional meta
        nbt.putLong("seed", seed);
        if (!preset.isEmpty()) nbt.putString("preset", preset);
        if (!style.isEmpty()) nbt.putString("style", style);
        if (!configHash.isEmpty()) nbt.putString("configHash", configHash);

        return nbt;
    }

    public static PrivateIslandInitState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PrivateIslandInitState s = new PrivateIslandInitState();

        // Schema/Versioning (Migration-ready)
        int schema = nbt.contains("schema", NbtCompound.INT_TYPE) ? nbt.getInt("schema") : 0;
        s.schema = schema;

        // Backward-compatible reads
        s.initialized = nbt.getBoolean("initialized");
        s.initEpochMs = nbt.contains("initEpochMs", NbtCompound.LONG_TYPE) ? nbt.getLong("initEpochMs") : 0L;

        // optional meta
        s.seed = nbt.contains("seed", NbtCompound.LONG_TYPE) ? nbt.getLong("seed") : 0L;
        s.preset = nbt.contains("preset", NbtCompound.STRING_TYPE) ? nbt.getString("preset") : "";
        s.style = nbt.contains("style", NbtCompound.STRING_TYPE) ? nbt.getString("style") : "";
        s.configHash = nbt.contains("configHash", NbtCompound.STRING_TYPE) ? nbt.getString("configHash") : "";

        // Mini-Migration Beispiel: schema==0 -> schema=1 (falls du früher ohne schema gespeichert hast)
        if (s.schema < CURRENT_SCHEMA) {
            s.schema = CURRENT_SCHEMA;
            // keine markDirty() hier zwingend nötig, aber kann sinnvoll sein:
            // s.markDirty();
        }

        return s;
    }
}
