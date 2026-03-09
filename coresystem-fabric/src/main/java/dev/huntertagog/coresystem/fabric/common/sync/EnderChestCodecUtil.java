package dev.huntertagog.coresystem.fabric.common.sync;

import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;

public final class EnderChestCodecUtil {

    private EnderChestCodecUtil() {
    }

    public static NbtList serializeEnderChest(EnderChestInventory inv,
                                              RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            // encode(...) returns NbtElement in your mappings → cast to NbtCompound
            NbtCompound tag = (NbtCompound) stack.encode(registries);
            tag.putByte("Slot", (byte) i);

            list.add(tag);
        }

        return list;
    }

    public static void deserializeEnderChest(EnderChestInventory inv,
                                             NbtList list,
                                             RegistryWrapper.WrapperLookup registries) {
        inv.clear();

        for (int i = 0; i < list.size(); i++) {
            // In deiner Mapping-Version: NbtList#get(int) -> NbtElement
            var element = list.get(i);
            if (!(element instanceof NbtCompound tag)) {
                continue; // defensive, falls jemand Mist in die NBT schreibt
            }

            int slot = tag.getByte("Slot") & 0xFF;
            if (slot >= inv.size()) {
                continue;
            }

            // fromNbt(...) -> Optional<ItemStack> bei dir
            ItemStack stack = ItemStack.fromNbt(registries, tag).orElse(ItemStack.EMPTY);
            inv.setStack(slot, stack);
        }
    }
}
