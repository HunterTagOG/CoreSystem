package dev.huntertagog.coresystem.fabric.common.item;

import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import dev.huntertagog.coresystem.fabric.common.item.impl.ServerKeyItem;
import dev.huntertagog.coresystem.fabric.common.text.MessageItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {

    // ------------------------------------------------------------------------
    // Item-Registrierung
    // ------------------------------------------------------------------------

    /**
     * Animierter, interaktiver Key mit GeckoLib + ServerSwitcher-Logik
     */
    public static final Item SERVER_KEY = register(
            "server_key",
            new ServerKeyItem(new Item.Settings().maxCount(1))
    );

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(CoresystemCommon.MOD_ID, name),
                item
        );
    }

    public static void init() {
        // damit die Klasse geladen wird
    }

    // ------------------------------------------------------------------------
    // Message-basierte Standard-Stacks
    // ------------------------------------------------------------------------

    /**
     * Standard-Stack des Server Keys mit lokalisiertem Namen & Lore.
     */
    public static ItemStack serverKeyStack() {
        return MessageItems.stack(
                SERVER_KEY,
                CoreMessage.ITEM_SERVER_KEY_NAME,
                CoreMessage.ITEM_SERVER_KEY_LORE_1,
                CoreMessage.ITEM_SERVER_KEY_LORE_2
        );
    }

    /**
     * Generic helper
     */
    public static ItemStack stackWithMessages(Item item,
                                              CoreMessage displayName,
                                              CoreMessage... lore) {
        return MessageItems.stack(item, displayName, lore);
    }
}
