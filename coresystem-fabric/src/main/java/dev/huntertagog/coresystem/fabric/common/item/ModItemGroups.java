package dev.huntertagog.coresystem.fabric.common.item;

import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModItemGroups {

    public static final RegistryKey<ItemGroup> CORE_TAB_KEY =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of("coresystem", "core_tab"));

    public static final ItemGroup CORE_TAB = Registry.register(
            Registries.ITEM_GROUP,
            CORE_TAB_KEY,
            FabricItemGroup.builder()
                    // nutzt nun den standardisierten Stack aus ModItems
                    .icon(ModItems::serverKeyStack)
                    .displayName(Messages.t(CoreMessage.ITEMGROUP_CORE_TAB))
                    .build()
    );

    public static void init() {
        // nur damit die Klasse geladen wird
    }
}
