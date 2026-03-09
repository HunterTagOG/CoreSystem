package dev.huntertagog.coresystem.fabric.common.rct.rewards;

import dev.huntertagog.coresystem.common.rct.rewards.dto.RctRewardItemDto;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class RctRewardItemCodec {

    private RctRewardItemCodec() {
    }

    public static RctRewardItemDto toDto(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        if (stack == null || stack.isEmpty()) {
            return new RctRewardItemDto("minecraft:air", 0, "");
        }

        Identifier id = Registries.ITEM.getId(stack.getItem());
        int count = stack.getCount();

        NbtCompound nbt = new NbtCompound();
        // "encode" kann NbtElement sein → robust handeln
        NbtElement encoded = stack.encode(registries);
        if (encoded instanceof NbtCompound c) {
            nbt = c;
        } else {
            // Fallback: in Compound legen
            nbt.put("stack", encoded);
        }

        String nbtB64 = NbtBase64Codec.toBase64(nbt);
        return new RctRewardItemDto(id.toString(), count, nbtB64);
    }

    public static ItemStack fromDto(RctRewardItemDto dto, RegistryWrapper.WrapperLookup registries) {
        if (dto == null || dto.itemId() == null || dto.itemId().isBlank() || dto.count() <= 0) {
            return ItemStack.EMPTY;
        }

        // Wenn NBT vorhanden ist: bevorzugt daraus den kompletten Stack rekonstruieren
        if (dto.nbtBase64() != null && !dto.nbtBase64().isBlank()) {
            NbtCompound nbt = NbtBase64Codec.fromBase64(dto.nbtBase64());
            if (nbt != null) {
                // falls du oben "stack" als wrapper genutzt hast
                if (nbt.contains("stack", NbtElement.COMPOUND_TYPE)) {
                    nbt = nbt.getCompound("stack");
                }

                ItemStack rebuilt = ItemStackCompat.fromNbtCompat(registries, nbt);
                if (!rebuilt.isEmpty()) {
                    // Count aus DTO ist “source of truth” (optional)
                    rebuilt.setCount(dto.count());
                    return rebuilt;
                }
            }
        }

        // Fallback: Item + Count (ohne Custom-NBT)
        Item item = Registries.ITEM.get(Identifier.of(dto.itemId()));
        return new ItemStack(item, dto.count());
    }

    // ------------------------------------------------------------
    // NBT<->Base64 helper (gzip) mit NbtSizeTracker/NbtAccounter Fallback
    // ------------------------------------------------------------
    private static final class NbtBase64Codec {

        static String toBase64(NbtCompound nbt) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NbtIo.writeCompressed(nbt, baos);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                return "";
            }
        }

        static NbtCompound fromBase64(String base64) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

                // MC-Versionen: mal NbtSizeTracker, mal NbtAccounter, mal ohne Parameter.
                // Wir lösen das robust per Reflection.
                return readCompressedCompat(bais);

            } catch (Exception e) {
                return null;
            }
        }

        private static NbtCompound readCompressedCompat(ByteArrayInputStream in) throws Exception {
            // 1) NbtIo.readCompressed(InputStream) – falls vorhanden
            try {
                var m = NbtIo.class.getMethod("readCompressed", java.io.InputStream.class);
                Object res = m.invoke(null, in);
                return (NbtCompound) res;
            } catch (NoSuchMethodException ignored) {
            }

            // 2) NbtIo.readCompressed(InputStream, NbtSizeTracker)
            try {
                Class<?> tracker = Class.forName("net.minecraft.nbt.NbtSizeTracker");
                var ofUnlimited = tracker.getMethod("ofUnlimitedBytes");
                Object trackerInstance = ofUnlimited.invoke(null);

                var m = NbtIo.class.getMethod("readCompressed", java.io.InputStream.class, tracker);
                Object res = m.invoke(null, in, trackerInstance);
                return (NbtCompound) res;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }

            // 3) NbtIo.readCompressed(InputStream, NbtAccounter)
            Class<?> accounter = Class.forName("net.minecraft.nbt.NbtAccounter");
            Object accounterInstance;
            try {
                accounterInstance = accounter.getMethod("unlimitedHeap").invoke(null);
            } catch (NoSuchMethodException e) {
                // older: constructor(long)
                accounterInstance = accounter.getConstructor(long.class).newInstance(Long.MAX_VALUE);
            }

            var m = NbtIo.class.getMethod("readCompressed", java.io.InputStream.class, accounter);
            Object res = m.invoke(null, in, accounterInstance);
            return (NbtCompound) res;
        }
    }

    private static final class ItemStackCompat {

        private ItemStackCompat() {
        }

        static ItemStack fromNbtCompat(RegistryWrapper.WrapperLookup registries, NbtCompound nbt) {
            try {
                // 1) ItemStack.fromNbt(WrapperLookup, NbtCompound)
                try {
                    var m = ItemStack.class.getMethod("fromNbt", RegistryWrapper.WrapperLookup.class, NbtCompound.class);
                    Object res = m.invoke(null, registries, nbt);
                    return (ItemStack) res;
                } catch (NoSuchMethodException ignored) {
                }

                // 2) ItemStack.fromNbtOrEmpty(WrapperLookup, NbtCompound)
                try {
                    var m = ItemStack.class.getMethod("fromNbtOrEmpty", RegistryWrapper.WrapperLookup.class, NbtCompound.class);
                    Object res = m.invoke(null, registries, nbt);
                    return (ItemStack) res;
                } catch (NoSuchMethodException ignored) {
                }

                // 3) older: ItemStack.of(NbtCompound)
                try {
                    var m = ItemStack.class.getMethod("of", NbtCompound.class);
                    Object res = m.invoke(null, nbt);
                    return (ItemStack) res;
                } catch (NoSuchMethodException ignored) {
                }

            } catch (Exception ignored) {
            }

            return ItemStack.EMPTY;
        }
    }

}
