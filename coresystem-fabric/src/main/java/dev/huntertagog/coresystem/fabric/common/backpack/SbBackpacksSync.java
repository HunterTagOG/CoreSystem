package dev.huntertagog.coresystem.fabric.common.backpack;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;

import java.util.List;
import java.util.UUID;

public final class SbBackpacksSync {

    private static final Logger LOG = LoggerFactory.get("SbBackpacksSync");
    private static final String NBT_KEY = "sbBackpacks";

    private SbBackpacksSync() {
    }

    /**
     * Ergänzt den Spieler-Snapshot um alle Sophisticated-Backpacks,
     * die der Spieler aktuell trägt (Main/Armor/Offhand).
     */
    public static void enrichSnapshot(ServerPlayerEntity player, NbtCompound root) {
        List<UUID> ids = SbBackpackUtil.getCarriedBackpackUUIDs(player);
        if (ids.isEmpty()) {
            return;
        }

        NbtList list = new NbtList();
        for (UUID id : ids) {
            try {
                // holt die Inhalte dieses Backpacks
                NbtCompound contents = BackpackStorage.get().getOrCreateBackpackContents(id);

                NbtCompound entry = new NbtCompound();
                entry.putUuid("id", id);
                // Kopie, damit wir keine Shared-Referenz im Snapshot halten
                entry.put("data", contents.copy());

                list.add(entry);
            } catch (Exception e) {
                LOG.warn("Failed to snapshot SophisticatedBackpack contents for {}: {}", id, e.toString());
            }
        }

        if (!list.isEmpty()) {
            root.put(NBT_KEY, list);
            LOG.debug("Snapshot for {}: stored {} Sophisticated Backpack(s)", player.getGameProfile().getName(), list.size());
        }
    }

    /**
     * Stellt die Inhalte der Sophisticated-Backpacks aus dem Snapshot wieder her.
     * Wird auf dem Zielserver nach dem normalen Inventory/Enderchest-Restore aufgerufen.
     */
    public static void restoreFromSnapshot(ServerPlayerEntity player, NbtCompound root) {
        if (!root.contains(NBT_KEY, NbtElement.LIST_TYPE)) {
            return;
        }

        NbtList list = root.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
        if (list.isEmpty()) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            try {
                UUID id = entry.getUuid("id");
                NbtCompound data = entry.getCompound("data");

                NbtCompound target = BackpackStorage.get().getOrCreateBackpackContents(id);
                // überschreiben mit dem Snapshot
                target.copyFrom(data);
            } catch (Exception e) {
                LOG.warn("Failed to restore SophisticatedBackpack entry {} for {}: {}",
                        i, player.getGameProfile().getName(), e.toString());
            }
        }

        LOG.debug("Restored {} Sophisticated Backpack(s) for {}",
                list.size(), player.getGameProfile().getName());
    }
}
