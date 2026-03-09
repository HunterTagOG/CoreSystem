package dev.huntertagog.coresystem.fabric.common.gui;

import net.minecraft.item.ItemStack;

public final class GuiItem {

    private final ItemStack itemStack;

    public GuiItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public static GuiItem of(ItemStack stack) {
        return new GuiItem(stack);
    }
}
