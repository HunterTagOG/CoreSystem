package dev.huntertagog.coresystem.fabric.common.text;

import dev.huntertagog.coresystem.common.text.CoreMessage;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class MessageItems {

    private MessageItems() {
    }

    /**
     * Baut einen ItemStack mit lokalisiertem Namen + Lore.
     */
    public static ItemStack stack(Item item,
                                  CoreMessage displayNameKey,
                                  CoreMessage... loreKeys) {
        ItemStack stack = new ItemStack(item);
        applyDisplayName(stack, displayNameKey);
        applyLore(stack, loreKeys);
        return stack;
    }

    /**
     * Setzt den angepassten Display-Namen via DataComponent CUSTOM_NAME.
     */
    public static void applyDisplayName(ItemStack stack, CoreMessage displayNameKey) {
        if (displayNameKey == null) {
            return;
        }
        Text name = Messages.t(displayNameKey);
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
    }

    /**
     * Setzt Lore-Zeilen via DataComponent LORE.
     */
    public static void applyLore(ItemStack stack, CoreMessage... loreKeys) {
        if (loreKeys == null || loreKeys.length == 0) {
            return;
        }

        List<Text> lines = Arrays.stream(loreKeys)
                .map(Messages::t)
                .collect(Collectors.toList());

        if (lines.isEmpty()) {
            return;
        }

        LoreComponent lore = new LoreComponent(lines);
        stack.set(DataComponentTypes.LORE, lore);
    }
}
