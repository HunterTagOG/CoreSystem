package dev.huntertagog.coresystem.fabric.common.player.playerdata;

import dev.huntertagog.coresystem.fabric.common.backpack.SbBackpacksSync;
import dev.huntertagog.coresystem.fabric.common.sync.EnderChestCodecUtil;
import dev.huntertagog.coresystem.fabric.common.sync.ExtraSlotsSyncUtil;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public final class PlayerInventoryCodec {

    private PlayerInventoryCodec() {
    }

    /**
     * Snapshot der Spieler-Inventar-Daten (ohne Position, ohne Health usw.).
     * - Main Inventory
     * - Armor
     * - Offhand
     * - Enderchest
     * - Extra-Slots (Travellers Backpack in Trinkets / Curios etc.)
     * - Sophisticated Backpacks
     */
    public static String encodeInventoryToBase64(MinecraftServer server, ServerPlayerEntity player) throws IOException {
        NbtCompound root = new NbtCompound();

        // Registry-Lookup für Enderchest / ItemStacks
        RegistryWrapper.WrapperLookup registries = server.getRegistryManager();

        // 1) Vanilla Inventory
        NbtList invList = new NbtList();
        player.getInventory().writeNbt(invList);
        root.put("inventory", invList);

        // 2) Enderchest
        root.put(
                "enderChest",
                EnderChestCodecUtil.serializeEnderChest(player.getEnderChestInventory(), registries)
        );

        // 3) Extra-Slots (Travellers Backpack in Trinkets / Curios etc.)
        ExtraSlotsSyncUtil.encodeExtraSlots(player, root);

        // 4) Sophisticated Backpacks
        SbBackpacksSync.enrichSnapshot(player, root);

        // 5) NBT -> gzip + Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static void applyInventoryFromBase64(MinecraftServer server,
                                                ServerPlayerEntity player,
                                                String base64Payload) throws IOException {
        if (base64Payload == null || base64Payload.isEmpty()) {
            return;
        }

        byte[] bytes = Base64.getDecoder().decode(base64Payload);

        // Registry-Lookup für Enderchest / ItemStacks
        RegistryWrapper.WrapperLookup registries = server.getRegistryManager();

        NbtCompound root;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             GZIPInputStream gis = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gis)) {

            // In deinen Mappings: NbtIo.read(...) -> NbtElement
            NbtElement element = NbtIo.read(dis, NbtSizeTracker.ofUnlimitedBytes());
            if (!(element instanceof NbtCompound compound)) {
                // sehr defensive Guard: falls jemand kein Compound gespeichert hat
                return;
            }
            root = compound;
        }

        // -------------------------------
        // Ab hier: dein bestehender Code
        // -------------------------------

        // 1) Vanilla Inventory
        if (root.contains("inventory", NbtElement.LIST_TYPE)) {
            NbtList invList = root.getList("inventory", NbtElement.COMPOUND_TYPE);
            player.getInventory().clear();
            player.getInventory().readNbt(invList);
            player.playerScreenHandler.sendContentUpdates();
        }

        // 2) Enderchest
        if (root.contains("enderChest", NbtElement.LIST_TYPE)) {
            NbtList ecList = root.getList("enderChest", NbtElement.COMPOUND_TYPE);
            EnderChestCodecUtil.deserializeEnderChest(
                    player.getEnderChestInventory(),
                    ecList,
                    registries
            );
        }

        // 3) Extra-Slots (Travellers Backpack, Trinkets etc.)
        ExtraSlotsSyncUtil.applyExtraSlots(player, root);

        // 4) Sophisticated Backpacks wiederherstellen
        SbBackpacksSync.restoreFromSnapshot(player, root);
    }
}
