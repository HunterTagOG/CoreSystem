package dev.huntertagog.coresystem.fabric.common.sync;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Generischer Hook für zusätzliche Slots (Trinkets / Curios / Custom).
 * Erstmal minimal, bei Bedarf kannst du hier mod-spezifische Integrationen andocken.
 */
public final class ExtraSlotsSyncUtil {

    private ExtraSlotsSyncUtil() {
    }

    public static void encodeExtraSlots(ServerPlayerEntity player, NbtCompound target) {
        // Beispiel: Trinkets-API (optional, pseudo-code / soft-dependency)
        // if (FabricLoader.getInstance().isModLoaded("trinkets")) {
        //     NbtCompound trinketNbt = TrinketsApi.getTrinkets(player).writeNbt(new NbtCompound());
        //     target.put("trinkets", trinketNbt);
        // }

        // Hier könntest du später weitere Mods integrieren (Curios-Ports o. Ä.)
    }

    public static void applyExtraSlots(ServerPlayerEntity player, NbtCompound source) {
        // if (source.contains("trinkets") && FabricLoader.getInstance().isModLoaded("trinkets")) {
        //     NbtCompound trinketNbt = source.getCompound("trinkets");
        //     TrinketsApi.getTrinkets(player).readNbt(trinketNbt);
        // }
    }
}
